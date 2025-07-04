package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.*
import au.org.ala.ws.tokens.TokenService
import com.nimbusds.oauth2.sdk.token.AccessToken
import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonSlurper
import org.codehaus.jackson.map.ObjectMapper
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller

import static grails.async.Promises.waitAll
/**
 * Tests for the ParatooService.
 * The tests are incomplete as some of the behaviour needs to be specified.
 */
class ParatooServiceSpec extends MongoSpec implements ServiceUnitTest<ParatooService>, DataTest {

    String userId = 'u1'
    SiteService siteService = Mock(SiteService)
    ProjectService projectService = Mock(ProjectService)
    WebService webService = Mock(WebService)
    TokenService tokenService = Mock(TokenService)
    SettingService settingService = Mock(SettingService)
    MetadataService metadataService = Mock(MetadataService)
    SpeciesReMatchService speciesReMatchService = Mock(SpeciesReMatchService)
    ActivityService activityService = Mock(ActivityService)
    RecordService recordService = Mock(RecordService)
    UserService userService = Mock(UserService)

    static Map DUMMY_POLYGON = [type: 'Polygon', coordinates: [[[1, 2], [2, 2], [2, 1], [1, 1], [1, 2]]]]
    static Map DUMMY_PLOT = ['type':'Point', coordinates: [1,2]]
    static Map DUMMY_MULTI_POLYGON = [type: 'MultiPolygon', coordinates: [[[[1, 2], [2, 2], [2, 1], [1, 1], [1, 2]]]]]

    // The am/pm in the formatted time is local dependent and this appears to be easiest way to determine the value.
    String am = DateUtil.formatAsDisplayDateTime("2024-05-14T00:00:00Z")[-2..-1]
    String pm = am == "AM" ? "PM" : "pm"

    def setup() {

        deleteAll()
        setupData()

        grailsApplication.config.paratoo.location.excluded = ['location.vegetation-association-nvis']
        grailsApplication.config.paratoo.species.specialCase = ["Other"]
        service.grailsApplication = grailsApplication
        service.webService = webService
        service.siteService = siteService
        service.projectService = projectService
        service.permissionService = new PermissionService() // Using the real permission service for this test
        service.tokenService = tokenService
        service.settingService = settingService
        service.metadataService = metadataService
        service.activityService = activityService
        service.recordService = recordService
        service.cacheService = new CacheService()
        service.userService = userService
        service.speciesReMatchService = speciesReMatchService

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    private Map readSurveyData(String name) {
        URL url = getClass().getResource("/paratoo/${name}.json")
        new JsonSlurper().parse(url)
    }

    private ParatooCollectionId buildCollectionId(String name = "mintCollectionIdPayload", String protocolId = 'guid-2') {
        Map collectionIdJson = readSurveyData(name)
        ParatooCollectionId collectionId = ParatooCollectionId.fromMap(collectionIdJson)
        collectionId.survey_metadata.survey_details.protocol_id = protocolId
        collectionId
    }

    private void deleteAll() {
        Hub.findAll().each { it.delete() }
        Project.findAll().each { it.delete() }
        ActivityForm.findAll().each { it.delete() }
        Service.findAll().each { it.delete() }
        UserPermission.findAll().each { it.delete() }
        Program.findAll().each { it.delete() }
        Site.findAll().each { it.delete() }
        Activity.findAll().each { it.delete() }
    }

    def cleanup() {
        deleteAll()
    }

    void "The service can map user projects into a format useful for the paratoo API"() {

        when:
        List<ParatooProject> projects = service.userProjects(userId)

        then:
        projects.size() == 1
        projects[0].id == "p1"
        projects[0].name == "Project 1"
        projects[0].accessLevel == AccessLevel.admin
        projects[0].projectArea == DUMMY_POLYGON
        projects[0].plots.size() == 1
        projects[0].plots[0].siteId == 's2'
        projects[0].protocols*.name == ["Plot Selection", "aParatooForm 1", "aParatooForm 2", "aParatooForm 3"]

        and:
        1 * siteService.geometryAsGeoJson({ it.siteId == 's1' }) >> DUMMY_POLYGON

    }

    void "Starred projects won't be included unless the user has a hub permission"() {

        setup:
        UserPermission userPermission = UserPermission.findByUserId(userId)
        userPermission.accessLevel = AccessLevel.starred
        userPermission.save(flush: true, failOnError: true)

        when:
        List<ParatooProject> projects = service.userProjects(userId)

        then:
        projects.size() == 0

        when: "The user has the MERIT grant manager role"
        UserPermission meritGrantManager = new UserPermission(userId: userId, entityId: 'merit', entityType: 'au.org.ala.ecodata.Hub', accessLevel: AccessLevel.caseManager)
        meritGrantManager.save(flush: true, failOnError: true)
        projects = service.userProjects(userId)

        then:
        projects.size() == 1
    }

    void "If the user has starred a project and also has a role, the role will be used"() {

        setup:
        UserPermission userPermission = new UserPermission(userId: userId, entityId: 'p1', entityType: 'au.org.ala.ecodata.Project', accessLevel: AccessLevel.starred)
        userPermission.save(flush: true, failOnError: true)

        when:
        List<ParatooProject> projects = service.userProjects(userId)

        then:
        projects.size() == 1
        projects[0].accessLevel == AccessLevel.admin

    }

    void "userProjects can convert a Feature or MultiPolygon typed project extent to a Polygon to support the use of known shape selection in MERIT (e.g. a NRM region)"() {

        when:
        List<ParatooProject> projects = service.userProjects(userId)

        then:
        1 * siteService.geometryAsGeoJson(_) >> DUMMY_MULTI_POLYGON

        and:
        projects.size() == 1
        projects[0].id == "p1"
        projects[0].name == "Project 1"
        projects[0].accessLevel == AccessLevel.admin
        projects[0].projectArea == DUMMY_POLYGON
        projects[0].plots.size() == 1
        projects[0].plots[0].siteId == 's2'
        projects[0].protocols*.name == ["Plot Selection", "aParatooForm 1", "aParatooForm 2", "aParatooForm 3"]

    }


    void "The service should create a data set in the planned state when the mintCollectionId method is called"() {
        setup:
        ParatooCollectionId collectionId = buildCollectionId()
        String projectId = 'p1'

        when:
        Map result = service.mintCollectionId('u1', collectionId)

        then:
        1 * projectService.updateDataSet(projectId, _) >> { pId, dataSet ->
            pId == projectId
            assert dataSet.surveyId != null
            assert dataSet.surveyId.eventTime != null
            assert dataSet.surveyId.userId == 'org1'
            assert dataSet.surveyId.survey_metadata.orgMintedUUID == dataSet.dataSetId
            assert dataSet.protocol == collectionId.protocolId
            assert dataSet.grantId == "g1"
            assert dataSet.progress == 'planned'
            assert dataSet.name == "aParatooForm 1 - ${DateUtil.formatAsDisplayDateTime(collectionId.eventTime)}"

            [status: 'ok']
        }

        and:
        result.status == 'ok'
        result.orgMintedIdentifier != null
    }

    void "The service can create a data set from a submitted collection"() {
        setup:
        String projectId = 'p1'
        String orgMintedId = 'd1'
        ParatooProtocolId protocol = new ParatooProtocolId(id: 1, version: 1)
        ParatooCollection collection = new ParatooCollection(
                orgMintedUUID:orgMintedId,
                coreProvenance: [
                        "system_core": "<system-core>",
                        "version_core": "<core-version>"
                ]
        )
        ParatooCollectionId paratooCollectionId = buildCollectionId()
        Map dataSet = [dataSetId:'d1',  grantId:'g1', surveyId:paratooCollectionId.toMap(), activityId: "123"]
        dataSet.surveyId.survey_metadata.orgMintedUUID = orgMintedId
        Map expectedDataSetSync = dataSet
        Map expectedDataSetAsync = dataSet + [progress: Activity.STARTED, startDate: "2023-09-01T00:00:00Z", endDate: "2023-09-01T00:00:00Z", areSpeciesRecorded: false, activityId: '123', siteId: "s1", format: "Database Table", sizeUnknown: true, name: "aParatooForm 1 - 2023-09-01 10:00 ${am}"]
        ParatooProject project = new ParatooProject(id: projectId, project: new Project(projectId: projectId, custom: [dataSets: [dataSet]]))
        Map site

        when:
        Map result = service.submitCollection(collection, project)
        waitAll(result.promise)

        then:
        1 * webService.doPost(*_) >> [resp: [collections: ["coarse-woody-debris-survey": [uuid: "1", createdAt: "2023-09-01T00:00:00.123Z", start_date_time: "2023-09-01T00:00:00.123Z", end_date_time: "2023-09-01T00:00:00.123Z"], "coarse-woody-debris-survey-observation": [[point: [lat: 1, lng: 2, name: [data: [attributes: [symbol: "ab"]]]]]]]]]
        1 * tokenService.getAuthToken(true) >> Mock(AccessToken)
        1 * projectService.updateDataSet(projectId, expectedDataSetAsync) >> [status: 'ok']
        1 * projectService.updateDataSet(projectId, expectedDataSetSync) >> [status: 'ok']
        1 * activityService.create({
            it.startDate == "2023-09-01T00:00:00Z" && it.endDate == "2023-09-01T00:00:00Z" &&
            it.plannedStartDate == "2023-09-01T00:00:00Z" && it.plannedEndDate == "2023-09-01T00:00:00Z" &&
            it.externalIds[0].externalId == "d1" && it.externalIds[0].idType == ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID
        }) >> [activityId: '123']
        1 * siteService.create(_) >> { site = it[0]; [siteId: 's1'] }
        1 * activityService.delete("123", true) >> [status: 'ok']
        1 * recordService.getAllByActivity('123') >> []
        1 * settingService.getSetting('paratoo.surveyData.mapping') >> {
            (["guid-2": [
                    "name"          : "coarse woody debris",
                    "usesPlotLayout": false,
                    "tags"          : ["survey"],
                    "apiEndpoint"   : "coarse-woody-debris-surveys",
                    "geometryType"  : "Point",
                    "overrides"     : [
                            "dataModel": null,
                            "viewModel": null
                    ]
            ]] as JSON).toString()
        }
        1 * userService.getCurrentUserDetails() >> [userId: userId]
        1 * userService.setCurrentUser(userId)

        and:
        result.updateResult == [status: 'ok']
        site.externalIds == [new ExternalId(externalId: "d1", idType: ExternalId.IdType.MONITOR_PLOT_GUID)]
    }

    void "The service can create a site from a submitted plot-selection"() {
        setup:
        Map data = [
                "plot_label"          : "CTMAUA2222",
                "recommended_location": ["lat": -35.2592424, "lng": 149.0651439],
                "uuid"                : "lmpisy5p9g896lad4ut",
                "comments"             : "Test"]

        Map expected = ['name': 'CTMAUA2222', 'description': 'CTMAUA2222', publicationStatus: 'published', 'externalIds': [new ExternalId(externalId: 'lmpisy5p9g896lad4ut', idType: ExternalId.IdType.MONITOR_PLOT_SELECTION_GUID)], 'notes': 'Test', 'extent': ['geometry': ['type': 'Point', 'coordinates': [149.0651439, -35.2592424], 'decimalLatitude': -35.2592424, 'decimalLongitude': 149.0651439], 'source': 'point'], 'projects': [], 'type': 'surveyArea']

        String userId = 'u1'

        when:
        service.addOrUpdatePlotSelections(userId, new ParatooPlotSelectionData(data))

        then:
        1 * siteService.create(expected)
    }

    void "The service can link a site to a project"() {
        setup:
        String projectId = 'p1'
        ParatooProject project = new ParatooProject(id: projectId, project: new Project(projectId: projectId))
        Map data = [plot_selections: ['s2']]

        when:
        service.updateProjectSites(project, data, [project])
        Site s2 = Site.findBySiteId('s2')

        then:
        s2.projects.indexOf(projectId) >= 0
    }

    void "The service can create a project area"() {
        setup:
        String projectId = 'p1'
        ParatooProject project = new ParatooProject(id: projectId, project: new Project(projectId: projectId))
        Map data = [project_area_type: 'polygon', project_area_coordinates: [
                [
                        "lat": -34.96643621094802,
                        "lng": 138.6845397949219
                ],
                [
                        "lat": -35.003565839769166,
                        "lng": 138.66394042968753
                ],
                [
                        "lat": -34.955744257334246,
                        "lng": 138.59973907470706
                ]
        ]]
        Map expectedSite = [name  : "Monitor Project Extent", type: Site.TYPE_PROJECT_AREA, projects: [projectId],
                            extent: [source: "drawn", geometry: [type: 'Polygon', coordinates: [[[138.6845397949219, -34.96643621094802], [138.66394042968753, -35.003565839769166], [138.59973907470706, -34.955744257334246], [138.6845397949219, -34.96643621094802]]]]]]

        when:
        service.updateProjectSites(project, data, [project])

        then:
        1 * siteService.create(expectedSite)
    }

    void "The service can create a site from a submitted collection"() {
        setup:
        String projectId = 'p1'
        String orgMintedId = 'd1'
        ParatooProtocolId protocol = new ParatooProtocolId(id: "1", version: 1)
        ParatooCollection collection = new ParatooCollection(
                orgMintedUUID:orgMintedId,
                coreProvenance: [
                        "system_core": "<system-core>",
                        "version_core": "<core-version>"
                ]
        )
        ParatooCollectionId paratooCollectionId = buildCollectionId("mintCollectionIdBasalAreaPayload","guid-3")
        Map dataSet =  [dataSetId:'d1', grantId:'g1', surveyId:paratooCollectionId.toMap()]
        ParatooProject project = new ParatooProject(id: projectId, project: new Project(projectId: projectId, custom: [dataSets: [dataSet]]))
        Map surveyData = readSurveyData('basalAreaDbhReverseLookup')
        Map site

        when:
        Map result = service.submitCollection(collection, project)
        waitAll(result.promise)
        println ("finished waiting")

        then:
        1 * webService.doPost(*_) >> [resp: surveyData]
        1 * tokenService.getAuthToken(true) >> Mock(AccessToken)
        2 * projectService.updateDataSet(projectId, _) >> [status: 'ok']
        1 * siteService.create(_) >> { site = it[0]; [siteId: 's1'] }
        1 * activityService.create({
            it.startDate == "2023-09-22T00:59:47Z" && it.endDate == "2023-09-23T00:59:47Z" &&
                    it.plannedStartDate == "2023-09-22T00:59:47Z" && it.plannedEndDate == "2023-09-23T00:59:47Z" &&
                    it.externalIds[0].externalId == "d1" && it.externalIds[0].idType == ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID
        }) >> [activityId: '123']
        1 * recordService.getAllByActivity('123') >> []
        1 * settingService.getSetting('paratoo.surveyData.mapping') >> {
            (["guid-3": [
                    "name"          : "Basal Area - DBH",
                    "usesPlotLayout": true,
                    "tags"          : ["survey"],
                    "apiEndpoint"   : "basal-area-dbh-measure-surveys",
                    "overrides"     : [
                            "dataModel": null,
                            "viewModel": null
                    ]
            ]] as JSON).toString()
        }
        1 * userService.getCurrentUserDetails() >> [userId: userId]
        1 * userService.setCurrentUser(userId)

        and:
        site.name == "SATFLB0001 - Control (Core + Fauna)"
        site.description == "SATFLB0001 - Control (Core + Fauna)"
        site.notes == "Core monitoring plot some comment"
        site.type == "compound"
        site.publicationStatus == "published"
        site.externalIds[0].externalId == "2"
        site.externalIds[0].idType == ExternalId.IdType.MONITOR_PLOT_GUID

        result.updateResult == [status: 'ok']

    }

    void "The service will create use existing site" () {
        setup:
        String projectId = 'p1'
        String orgMintedId = 'd1'
        Date afterSubmissionDate = DateUtil.parseWithMilliseconds("2023-09-15T06:00:11.996Z")
        Date beforeSubmissionDate = DateUtil.parseWithMilliseconds("2023-09-14T06:00:11.996Z")
        new Site(
                name: "SATFLB0001 - Control (Core)",
                siteId: "s0",
                extent: [geometry: DUMMY_POLYGON],
                description: "SATFLB0001 - Control (Core)",
                notes: "Core monitoring plot some comment",
                type: "compound",
                externalIds: [new ExternalId(externalId: "2", idType: ExternalId.IdType.MONITOR_PLOT_GUID)],
                dateCreated: afterSubmissionDate,
                lastUpdated: afterSubmissionDate
        ).save(flush: true)
        ParatooProtocolId protocol = new ParatooProtocolId(id: "1", version: 1)
        ParatooCollection collection = new ParatooCollection(
                orgMintedUUID:orgMintedId,
                coreProvenance: [
                        "system_core": "<system-core>",
                        "version_core": "<core-version>"
                ]
        )
        ParatooCollectionId paratooCollectionId = buildCollectionId("mintCollectionIdBasalAreaPayload","guid-3")
        Map dataSet =  [dataSetId:'d1', grantId:'g1', surveyId:paratooCollectionId.toMap()]
        ParatooProject project = new ParatooProject(id: projectId, project: new Project(projectId: projectId, custom: [dataSets: [dataSet]]))
        Map surveyData = readSurveyData('basalAreaDbhReverseLookup')

        when:
        Map result = service.submitCollection(collection, project)
        waitAll(result.promise)
        println ("finished waiting")

        then:
        1 * webService.doPost(*_) >> [resp: surveyData]
        1 * tokenService.getAuthToken(true) >> Mock(AccessToken)
        2 * projectService.updateDataSet(projectId, _) >> [status: 'ok']
        0 * siteService.create(_)
        1 * activityService.create({
            it.startDate == "2023-09-22T00:59:47Z" && it.endDate == "2023-09-23T00:59:47Z" &&
                    it.plannedStartDate == "2023-09-22T00:59:47Z" && it.plannedEndDate == "2023-09-23T00:59:47Z" &&
                    it.externalIds[0].externalId == "d1" && it.externalIds[0].idType == ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID
        }) >> [activityId: '123']
        1 * recordService.getAllByActivity('123') >> []
        1 * settingService.getSetting('paratoo.surveyData.mapping') >> {
            (["guid-3": [
                    "name"          : "Basal Area - DBH",
                    "usesPlotLayout": true,
                    "tags"          : ["survey"],
                    "apiEndpoint"   : "basal-area-dbh-measure-surveys",
                    "overrides"     : [
                            "dataModel": null,
                            "viewModel": null
                    ]
            ]] as JSON).toString()
        }
        1 * userService.getCurrentUserDetails() >> [userId: userId]
        1 * userService.setCurrentUser(userId)
    }

    void "The service will create a new site if plot layout has been updated" () {
        setup:
        String projectId = 'p1'
        String orgMintedId = 'd1'
        Date afterSubmissionDate = DateUtil.parseWithMilliseconds("2023-09-15T06:00:11.996Z")
        Date beforeSubmissionDate = DateUtil.parseWithMilliseconds("2023-09-14T06:00:11.996Z")
        Site.withSession { session ->
            new Site(
                    name: "SATFLB0001 - Control (Core)",
                    siteId: "s0",
                    extent: [geometry: DUMMY_POLYGON],
                    description: "SATFLB0001 - Control (Core)",
                    notes: "Core monitoring plot some comment",
                    type: "compound",
                    externalIds: [new ExternalId(externalId: "2", idType: ExternalId.IdType.MONITOR_PLOT_GUID)],
                    dateCreated: afterSubmissionDate,
                    lastUpdated: afterSubmissionDate
            ).save(flush: true)
            session.flush()
        }
        ParatooProtocolId protocol = new ParatooProtocolId(id: "1", version: 1)
        ParatooCollection collection = new ParatooCollection(
                orgMintedUUID:orgMintedId,
                coreProvenance: [
                        "system_core": "<system-core>",
                        "version_core": "<core-version>"
                ]
        )
        ParatooCollectionId paratooCollectionId = buildCollectionId("mintCollectionIdBasalAreaPayload","guid-3")
        Map dataSet =  [dataSetId:'d1', grantId:'g1', surveyId:paratooCollectionId.toMap()]
        ParatooProject project = new ParatooProject(id: projectId, project: new Project(projectId: projectId, custom: [dataSets: [dataSet]]))
        Map surveyData = readSurveyData('basalAreaDbhReverseLookup')
        String date = DateUtil.format(new Date())
        date = date.replace("Z", ".999Z")
        surveyData.collections."basal-area-dbh-measure-survey"."plot_visit"."plot_layout"."updatedAt" = date
        Map site

        when:
        def result = service.submitCollection(collection, project)
        waitAll(result.promise)

        then:
        1 * webService.doPost(*_) >> [resp: surveyData]
        1 * tokenService.getAuthToken(true) >> Mock(AccessToken)
        2 * projectService.updateDataSet(projectId, _) >> [status: 'ok']
        1 * siteService.create(_) >> { site = it[0]; [siteId: 's1'] }
        1 * activityService.create({
            it.startDate == "2023-09-22T00:59:47Z" && it.endDate == "2023-09-23T00:59:47Z" &&
                    it.plannedStartDate == "2023-09-22T00:59:47Z" && it.plannedEndDate == "2023-09-23T00:59:47Z" &&
                    it.externalIds[0].externalId == "d1" && it.externalIds[0].idType == ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID
        }) >> [activityId: '123']
        1 * recordService.getAllByActivity('123') >> []
        1 * settingService.getSetting('paratoo.surveyData.mapping') >> {
            (["guid-3": [
                    "name"          : "Basal Area - DBH",
                    "usesPlotLayout": true,
                    "tags"          : ["survey"],
                    "apiEndpoint"   : "basal-area-dbh-measure-surveys",
                    "overrides"     : [
                            "dataModel": null,
                            "viewModel": null
                    ]
            ]] as JSON).toString()
        }
        1 * userService.getCurrentUserDetails() >> [userId: userId]
        1 * userService.setCurrentUser(userId)
        result.updateResult == [status: 'ok']
    }

    void "isUpdatedPlotLayout should check plot layout has been updated after site has been updated" () {
        given:
        def date1 = DateUtil.parseWithMilliseconds("2023-09-22T00:59:47.111Z")
        def date2 = DateUtil.parseWithMilliseconds("2023-09-23T00:59:47.111Z")
        def date3 = DateUtil.parseWithMilliseconds("2023-09-24T00:59:47.111Z")

        when:
        def result = service.isUpdatedPlotLayout(date1, date2)

        then:
        result

        when:
        result = service.isUpdatedPlotLayout(date3, date2)

        then:
        !result
    }

    private Map getProject(){
        [
                projectId:"p1",
                name:"Project 1",
                grantId:"g1",
                programId:"prog1",
                hubId:"merit",
                organisationId: "org1",
                custom:[details:[
                        serviceIds:[1],
                        baseline:[rows:[[protocols:['protocol category 1']]]],
                        monitoring:[rows:[[protocols:['protocol category 2', 'protocol category 3']]]]
                ], dataSets: [[
                                      dataSetId:'c1'
                              ]]]
        ]
    }

    private void setupData() {
        Hub hub = new Hub(hubId: "merit", urlPath: "merit")
        hub.save(failOnError: true, flush: true)
        Project project = new Project(getProject())
        project.save(failOnError: true, flush: true)
        UserPermission userPermission = new UserPermission(accessLevel: AccessLevel.admin, userId: userId, entityId: 'p1', entityType: Project.name)
        userPermission.save(failOnError: true, flush: true)

        Site projectArea = new Site(siteId: 's1', name: 'Site 1', type: Site.TYPE_PROJECT_AREA, extent: [geometry: DUMMY_POLYGON])
        projectArea.save(failOnError: true, flush: true)
        Site plot = new Site(siteId: 's2', name: "Site 2", type: Site.TYPE_SURVEY_AREA, extent: [geometry: DUMMY_PLOT], projects: ['p1'], externalIds:[new ExternalId(externalId: "2", idType: ExternalId.IdType.MONITOR_PLOT_SELECTION_GUID)])
        plot.save(failOnError: true, flush: true)
        siteService.sitesForProjectWithTypes('p1', [Site.TYPE_PROJECT_AREA, Site.TYPE_SURVEY_AREA]) >> [projectArea, plot]

        Program program = new Program(programId: "prog1", name: "A program", config: [(ParatooProject.PROGRAM_CONFIG_PARATOO_ITEM): true])
        program.save(failOnError: true, flush: true)

        Service service = new Service(name: "S1", serviceId: '1', legacyId: 1, outputs: [new ServiceForm(externalId: "guid-2", formName: "aParatooForm", sectionName: null)])
        service.save(failOnError: true, flush: true)

        ActivityForm plotSelection = new ActivityForm(name:"Plot Selection", type:"EMSA", category:"Plot Selection and Layout", external:true)
        plotSelection.externalIds = [new ExternalId(externalId: "plot-selection-guid", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        plotSelection.save(failOnError:true, flush: true)

        ActivityForm activityForm = new ActivityForm(name: "aParatooForm 1", type: 'EMSA', category: 'protocol category 1', external: true,
                sections: [
                        new FormSection(name: "section 1", type: "section", template: [
                                dataModel    : [
                                        [
                                                dataType: "list",
                                                name    : "coarse-woody-debris-survey",
                                                columns : [
                                                        [
                                                                dataType: "list",
                                                                name    : "coarse-woody-debris-survey-observation",
                                                                columns: [
                                                                        [
                                                                                dataType: "feature",
                                                                                name: "point"
                                                                        ]
                                                                ]
                                                        ]

                                                ]
                                        ]
                                ],
                                viewModel    : [],
                                relationships: [
                                        ecodata  : ["coarse-woody-debris-survey":["coarse-woody-debris-survey-observation": [:]]],
                                        apiOutput: [:]
                                ]
                        ]
                        )
                ]
        )
        activityForm.externalIds = [new ExternalId(externalId: "guid-2", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError: true, flush: true)

        activityForm = new ActivityForm(name: "aParatooForm 2 ", type: 'EMSA', category: 'protocol category 2', external: true,
                sections: [
                        new FormSection(name: "section 1", type: "section", template: [
                                dataModel    : [
                                        [
                                                dataType: "list",
                                                name    : "basal-area-dbh-measure-survey",
                                                columns : [
                                                        [
                                                                dataType: "list",
                                                                name    : "basal-area-dbh-measure-observation",
                                                                columns : [
                                                                        [
                                                                                dataType: "feature",
                                                                                name    : "location"
                                                                        ]
                                                                ]
                                                        ]
                                                ]
                                        ]
                                ],
                                viewModel    : [],
                                relationships: [
                                        ecodata  : ["basal-area-dbh-measure-survey":["basal-area-dbh-measure-observation": [:]]],
                                        apiOutput: [:]
                                ]
                        ]
                        )
                ])
        activityForm.externalIds = [new ExternalId(externalId: "guid-3", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError: true, flush: true)

        activityForm = new ActivityForm(name: "aParatooForm 3", type: 'EMSA', category: 'protocol category 3', external: true)
        activityForm.externalIds = [new ExternalId(externalId: "guid-4", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError: true, flush: true)

        activityForm = new ActivityForm(name: "aParatooForm 4", type: 'EMSA', category: 'protocol category 4', external: true,
                sections: [
                        new FormSection(name: "section 1", type: "section", template: [
                                dataModel    : [
                                        [
                                                dataType: "list",
                                                name    : "bird-survey",
                                                columns : [
                                                        [
                                                                dataType: "integer",
                                                                name    : "bird-observation"
                                                        ]
                                                ]
                                        ]
                                ],
                                viewModel    : [],
                                relationships: [
                                        ecodata  : ["bird-survey": ["bird-observation": [:]]],
                                        apiOutput: ["bird-survey.bird-observation": ["bird-observation": [:]]]
                                ]
                        ]
                        )
                ]
        )
        activityForm.externalIds = [new ExternalId(externalId: "guid-4", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError: true, flush: true)

        def activity = new Activity(
                activityId: UUID.randomUUID().toString(),
                type             : activityForm.name,
                formVersion      : activityForm.formVersion,
                description      : "Activity submitted by monitor",
                projectId        : "p1",
                publicationStatus: PublicationStatus.PUBLISHED,
                siteId           : "s2",
                startDate        : new Date(),
                endDate          : new Date(),
                plannedStartDate : new Date(),
                plannedEndDate   : new Date(),
                progress         : Activity.FINISHED,
                userId           : 'user1'
        )
        activity.externalIds = [new ExternalId(idType: ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID, externalId: 'abc')]
        activity.save(failOnError: true, flush: true)

        activity = new Activity (
                activityId: UUID.randomUUID().toString(),
                type             : activityForm.name,
                formVersion      : activityForm.formVersion,
                description      : "Activity submitted by monitor",
                projectId        : "p2",
                publicationStatus: PublicationStatus.PUBLISHED,
                siteId           : "s1",
                startDate        : new Date(),
                endDate          : new Date(),
                plannedStartDate : new Date(),
                plannedEndDate   : new Date(),
                progress         : Activity.FINISHED,
                userId           : 'user1'
        )
        activity.externalIds = [new ExternalId(idType: ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID, externalId: 'def')]
        activity.save(failOnError: true, flush: true)
    }

    void "capitalizeModelName should convert hyphenated name to capitalised name"() {
        when:
        String result = service.capitalizeModelName("paratoo-protocol")

        then:
        result == "ParatooProtocol"

        when:
        result = service.capitalizeModelName("paratoo-protocol-id")

        then:
        result == "ParatooProtocolId"

        when:
        result = service.capitalizeModelName(null)

        then:
        result == null

        when:
        result = service.capitalizeModelName("")

        then:
        result == ""
    }

    void "buildRelationshipTree should build relationship tree correctly"() {
        given:
        def properties = [
                "bird-observation" : [
                        "type"       : "integer",
                        "x-model-ref": "bird-survey"
                ],
                "bird-survey"      : [
                        "type"       : "integer",
                        "x-model-ref": "plot-visit"
                ],
                "fauna-survey"     : [
                        "type"       : "integer",
                        "x-model-ref": "plot-visit"
                ],
                "fauna-observation": [
                        "type"       : "integer",
                        "x-model-ref": "fauna-survey"
                ],
                "plot-visit"       : [
                        "type"       : "integer",
                        "x-model-ref": "plot-layout"
                ],
                "plot-layout"      : [
                        "type"       : "integer",
                        "x-model-ref": "plot-selection"
                ],
                "plot-selection"   : [
                        "type"      : "object",
                        "properties": [
                                "column": [
                                        "type": "string"
                                ]
                        ]
                ]
        ]


        when:
        def relationships = service.buildParentChildRelationship(properties)

        then:
        relationships.size() == 2
        relationships["bird-survey"] != null
        relationships["bird-survey"].size() == 1
        relationships["bird-survey"].contains("bird-observation")
        relationships["fauna-survey"].size() == 1
        relationships["fauna-survey"].contains("fauna-observation")
    }

    void "transformSpeciesName should convert paratoo species name to object correctly"() {
        when:
        Map result = service.transformSpeciesName("Acacia glauca [Species] (scientific: Acacia glauca Willd.)")
        String outputSpeciesId = result.remove("outputSpeciesId")
        then:
        outputSpeciesId != null
        result == [name: "Acacia glauca Willd. (Acacia glauca)", scientificName: "Acacia glauca Willd.", guid: "A_GUID", commonName: "Acacia glauca", taxonRank: "Species"]
        1 * speciesReMatchService.searchByName(_) >> null
        1 * speciesReMatchService.searchByName(_, false, true) >> null

        when: // no scientific name
        result = service.transformSpeciesName("Frogs [Class] (scientific: )")
        outputSpeciesId = result.remove("outputSpeciesId")

        then:
        outputSpeciesId != null
        result == [name: "Frogs", scientificName: "", guid: "A_GUID", commonName: "Frogs", taxonRank: "Class"]
        1 * speciesReMatchService.searchByName(_) >> null
        1 * speciesReMatchService.searchByName(_, false, true) >> null

        when: // user inputs scientific name to field
        result = service.transformSpeciesName("Centipeda cunninghamii")
        outputSpeciesId = result.remove("outputSpeciesId")

        then:
        outputSpeciesId != null
        result == [name: "Centipeda cunninghamii (Common Sneezeweed)", scientificName: "Centipeda cunninghamii", guid: "https://id.biodiversity.org.au/node/apni/2916674", commonName: "Common Sneezeweed", taxonRank: "species"]
        1 * speciesReMatchService.searchByName(_) >> [
                scientificName: "Centipeda cunninghamii",
                commonName: "Common Sneezeweed",
                guid: "https://id.biodiversity.org.au/node/apni/2916674",
                taxonRank: "species"
        ]
        0 * speciesReMatchService.searchByName(_, false, true)

        when: // user inputs scientific name to field and ALA cannot match the name
        result = service.transformSpeciesName("Centipeda cunninghamii")
        outputSpeciesId = result.remove("outputSpeciesId")

        then:
        outputSpeciesId != null
        result == [name: "Centipeda cunninghamii", scientificName: "Centipeda cunninghamii", commonName: null, taxonRank: null, guid: "A_GUID"]
        1 * speciesReMatchService.searchByName(_) >> null
        1 * speciesReMatchService.searchByName(_, false, true) >> null

        when: // Do not create record when value equals special cases. Therefore, removes guid.
        result = service.transformSpeciesName("Other")
        outputSpeciesId = result.remove("outputSpeciesId")

        then:
        outputSpeciesId != null
        result == [name: "Other", scientificName: "Other", commonName: null, taxonRank: null]
        1 * speciesReMatchService.searchByName(_) >> null
        1 * speciesReMatchService.searchByName(_, false, true) >> null

    }

    void "buildTreeFromParentChildRelationships should build tree correctly"() {
        given:
        def relationships = [
                "plot-layout"   : ["plot-visit"],
                "bird-survey"   : ["bird-observation"],
                "plot-visit"    : ["bird-survey", "fauna-survey"],
                "plot-selection": ["plot-layout"],
                "fauna-survey"  : ["fauna-observation"]
        ]

        when:
        def tree = service.buildTreeFrom2DRelationship(relationships)

        then:
        tree.size() == 1
        tree["bird-survey"] == null
        tree["plot-selection"].size() != 0
        tree["plot-selection"]["plot-layout"]["plot-visit"]["bird-survey"]["bird-observation"] != null
    }

    void "findPathFromRelationship should find path to model from relationship"() {
        setup:
        Map relationship = ["aerial-observation": [survey: ["aerial-survey": [setup_ID: ["aerial-setup-desktop": [survey: [:]]]]]], "aerial-survey": [survey: [:]], "aerial-setup": [survey: ["aerial-survey": [setup_ID: ["aerial-setup-desktop": [survey: [:]]]]]]]

        when:
        List paths = service.findPathFromRelationship("aerial-setup-desktop", relationship)

        then:
        paths.size() == 2
        paths[0] == "aerial-observation.survey.aerial-survey.setup_ID"
        paths[1] == "aerial-setup.survey.aerial-survey.setup_ID"
    }

    void "removeProperty should remove property from nested object"() {
        setup: "nested map"
        def object = [a: 1, b: [d: [e: [f: ["toRemove"]]]], c: 3]

        when:
        service.removeProperty(object, "b.d.e.f")

        then:
        object.size() == 3
        object.b.d.e.f == null

        when: "nested list"
        object = [a: 1, b: [[d: [[e: [[f: ["toRemove"]]]]]]], c: 3]
        service.removeProperty(object, "b.d.e.f")

        then:
        object.size() == 3
        object.b[0].d[0].e[0].f == null

    }

    void "rearrangePropertiesAccordingToModelRelationship should rearrange properties according to model relationship"() {
        setup:
        def relationship = '{\n' +
                '          "ecodata": {\n' +
                '            "vegetation-mapping-survey": {\n' +
                '              "vegetation-mapping-observation": {}\n' +
                '            }\n' +
                '          },\n' +
                '          "apiOutput": {\n' +
                '            "vegetation-mapping-observation.properties.vegetation_mapping_survey": {\n' +
                '                "vegetation-mapping-survey": {}\n' +
                '            }\n' +
                '          }\n' +
                '        }'
        ObjectMapper mapper = new ObjectMapper()
        relationship = mapper.readValue(relationship, Map.class)

        def output = """
{
  "vegetation-mapping-observation": {
    "type": "object",
    "properties": {
      "vegetation_mapping_survey": {
        "type": "object",
        "properties": {
          "test": {
            "type": "string"
          }
        }
      },
      "observation": {
        "type": "object",
        "properties": {
          "survey": {
            "type": "object",
            "properties": {}
          }
        }
      }
    }
  }
}
"""

        output = mapper.readValue(output, Map.class)

        when:
        output = service.rearrangePropertiesAccordingToModelRelationship(output, relationship.apiOutput, relationship.ecodata)

        then:
        output.size() == 2
        output["properties"]["vegetation-mapping-survey"]["properties"].size() == 2
        output["properties"]["vegetation-mapping-survey"]["properties"]["vegetation-mapping-observation"]["properties"].size() == 1
        output["properties"]["vegetation-mapping-survey"]["properties"]["test"] == ["type": "string"]
        output["properties"]["vegetation-mapping-survey"]["properties"]["vegetation-mapping-observation"]["properties"]["observation"]["properties"]["survey"]["properties"].size() == 0
    }

    void "cleanSwaggerDefinition should standardize the swagger definition"() {
        setup:
        def definition = [
                "type"                : "object",
                "required"            : ["data"],
                "properties"          : [
                        "data": [
                                "required"  : ["transect_number", "date_time"],
                                "type"      : "object",
                                "properties": [
                                        "plot"     : [
                                                "type"       : "integer",
                                                "x-model-ref": "setup"
                                        ],
                                        "number"   : ["type": "string"],
                                        "photo"    : [
                                                "type"               : "integer",
                                                "x-paratoo-file-type": ["images"],
                                                "x-model-ref"        : "file"
                                        ],
                                        "quad"     : [
                                                "type" : "array",
                                                "items": [
                                                        "type"       : "integer",
                                                        "x-model-ref": "quadrant"
                                                ],
                                        ],
                                        "date_time": ["type": "string", "format": "date-time"]
                                ]
                        ]
                ],
                "additionalProperties": false
        ]

        when:
        def result = service.cleanSwaggerDefinition(definition)

        then:
        result.size() == 3

        result.properties.size() == 5
        result.properties.plot.size() == 2
        result.properties.number.type == "string"
        result.properties.quad.properties.type == "integer"
        result.required.size() == 2
        result.required[0] == "transect_number"
    }

    void "buildChildParentRelationship should build child parent relationship from simplified data model"() {
        setup:
        def output = ' {\n' +
                '  "type" : "object",\n' +
                '  "properties" : {\n' +
                '    "plot" : {\n' +
                '      "type" : "integer",\n' +
                '      "x-model-ref" : "setup"\n' +
                '    },\n' +
                '    "number" : {\n' +
                '      "type" : "string"\n' +
                '    },\n' +
                '    "photo" : {\n' +
                '      "type" : "integer",\n' +
                '      "x-paratoo-file-type" : [ "images" ],\n' +
                '      "x-model-ref" : "file"\n' +
                '    },\n' +
                '    "quad" : {\n' +
                '      "type" : "array",\n' +
                '      "properties" : {\n' +
                '        "type" : "integer",\n' +
                '        "x-model-ref" : "quadrant"\n' +
                '      }\n' +
                '    },\n' +
                '    "date_time" : {\n' +
                '      "type" : "string",\n' +
                '      "format" : "date-time"\n' +
                '    }\n' +
                '  },\n' +
                '  "required" : [ "transect_number", "date_time" ]\n' +
                '}'
        ObjectMapper mapper = new ObjectMapper()
        def input = mapper.readValue(output, Map.class)

        when:
        def result = service.buildChildParentRelationship(input)

        then:
        result.size() == 2
        result["properties.plot"] == ["setup"]
        result["properties.quad"] == ["quadrant"]
    }

    void "buildParentChildRelationship should build parent child relationship from simplified data model"() {
        setup:
        def output = ' {\n' +
                '  "type" : "object",\n' +
                '  "properties" : {\n' +
                '    "plot" : {\n' +
                '      "type" : "integer",\n' +
                '      "x-model-ref" : "setup"\n' +
                '    },\n' +
                '    "number" : {\n' +
                '      "type" : "string"\n' +
                '    },\n' +
                '    "photo" : {\n' +
                '      "type" : "integer",\n' +
                '      "x-paratoo-file-type" : [ "images" ],\n' +
                '      "x-model-ref" : "file"\n' +
                '    },\n' +
                '    "quad" : {\n' +
                '      "type" : "array",\n' +
                '      "properties" : {\n' +
                '        "type" : "integer",\n' +
                '        "x-model-ref" : "quadrant"\n' +
                '      }\n' +
                '    },\n' +
                '    "date_time" : {\n' +
                '      "type" : "string",\n' +
                '      "format" : "date-time"\n' +
                '    }\n' +
                '  },\n' +
                '  "required" : [ "transect_number", "date_time" ]\n' +
                '}'
        ObjectMapper mapper = new ObjectMapper()
        def input = mapper.readValue(output, Map.class)

        when:
        def result = service.buildParentChildRelationship(input)

        then:
        result.size() == 2
        result["setup"] == ["plot"]
        result["quadrant"] == ["quad"]
    }

    void "rearrangeSurveyData should reorder api output according to provided relationship"() {
        setup:
        def mapper = new ObjectMapper()
        def output = '{"a": {}, "f": 4, "b": {"c": 1, "d": {}}, "e": {"g": 3}}'
        output = mapper.readValue(output, Map.class)
        def relationship = '{"d": {"b": {"e": {"a": {} } }}, "c": {} }'
        relationship = mapper.readValue(relationship, Map.class)
        def apiOutputRelationship = '{ "e": {"e": {} }, "a": "a", "b.c": {"c": {}}, "b.d": {"d": {}} }'
        apiOutputRelationship = mapper.readValue(apiOutputRelationship, Map.class)
        when:
        def result = service.rearrangeSurveyData(output, output, relationship, apiOutputRelationship)
        then:
        result.size() == 3
        result == ["f": 4, "d": ["b": ["e": ["g": 3, "a": [:]]]], "c": 1]
    }

    void "resolveModelReferences should swap model with definitions"() {
        setup:
        def dataModel = [
                "type"      : "object",
                "properties": [
                        "plot"     : [
                                "type"       : "integer",
                                "x-model-ref": "setup"
                        ],
                        "number"   : ["type": "string"],
                        "photo"    : [
                                "type"               : "integer",
                                "x-paratoo-file-type": ["images"],
                                "x-model-ref"        : "file"
                        ],
                        "quad"     : [
                                "type"      : "array",
                                "properties": [
                                        "type"       : "integer",
                                        "x-model-ref": "quadrant"
                                ],
                        ],
                        "date_time": ["type": "string", "format": "date-time"]
                ]
        ]

        def components = [
                "SetupRequest"   : [
                        "type"      : "object",
                        "properties": [
                                "transect_number": ["type": "string"],
                                "date_time"      : ["type": "string", "format": "date-time"]
                        ]
                ],
                "QuadrantRequest": [
                        "type"      : "object",
                        "properties": [
                                "transect": ["type": "string"],
                                "date"    : ["type": "string", "format": "date"]
                        ]
                ]
        ]

        when:
        def result = service.resolveModelReferences(dataModel, components)

        then:
        result.size() == 2
        result.properties.plot.properties.transect_number.type == "string"
        result.properties.plot.properties.date_time.format == "date-time"
        result.properties.quad.properties.properties.date.format == "date"
        result.properties.quad.properties.properties.transect.type == "string"
        result.properties.photo.type == "integer"
        result.properties.photo["x-model-ref"] == "file"
    }

    void "isPlotLayoutNeededByProtocol checks if plot is required by protocol"() {
        given:
        def protocol1 = [attributes: [workflow: [[modelName: 'plot-layout'], [modelName: 'other-model']]]]
        def expected1 = true

        when:
        def result = service.isPlotLayoutNeededByProtocol(protocol1)

        then:
        result == expected1

        when:
        def protocol2 = [attributes: [workflow: [[modelName: 'other-model'], [modelName: 'another-model']]]]
        def expected2 = false
        result = service.isPlotLayoutNeededByProtocol(protocol2)

        then:
        result == expected2
    }

    void "findProtocolEndpointDefinition should find the protocol endpoint definition"() {
        given:
        def protocol = [attributes: [endpointPrefix: "opportunes", workflow: [[modelName: 'plot-layout'], [modelName: 'other-model']]]]
        def documentation = [
                paths: [
                        "opportunes/bulk": [
                                post: [
                                        requestBody:
                                                [
                                                        content: [
                                                                "application/json": [
                                                                        schema: [
                                                                                "properties": [
                                                                                        "data": [
                                                                                                "properties": [
                                                                                                        "collections": [
                                                                                                                "items": [
                                                                                                                        "properties": [
                                                                                                                                "surveyId": [
                                                                                                                                        "properties": [
                                                                                                                                                "projectId": [
                                                                                                                                                        "type": "string"
                                                                                                                                                ],
                                                                                                                                                "protocol" : [
                                                                                                                                                        "properties": [
                                                                                                                                                                "id"     : [
                                                                                                                                                                        "type": "string"
                                                                                                                                                                ],
                                                                                                                                                                "version": [
                                                                                                                                                                        "type": "integer"
                                                                                                                                                                ]
                                                                                                                                                        ]
                                                                                                                                                ]
                                                                                                                                        ]
                                                                                                                                ]
                                                                                                                        ]
                                                                                                                ]
                                                                                                        ]
                                                                                                ]
                                                                                        ]
                                                                                ]
                                                                        ]
                                                                ]
                                                        ]
                                                ]
                                ]
                        ]
                ]
        ]

        when:
        def result = service.findProtocolEndpointDefinition(protocol, documentation)

        then:
        result != null
        result.surveyId != null

        when: // no endpoint path is found should return null
        documentation = [
                paths: [
                        "test/bulk": [
                                post: [
                                        requestBody:
                                                [
                                                        content: [
                                                                "application/json": [
                                                                        schema: [
                                                                                "properties": [
                                                                                        "data": [
                                                                                                "properties": [
                                                                                                        "collections": [
                                                                                                                "items": [
                                                                                                                        "properties": [
                                                                                                                                "surveyId": [
                                                                                                                                        "properties": [
                                                                                                                                                "projectId": [
                                                                                                                                                        "type": "string"
                                                                                                                                                ],
                                                                                                                                                "protocol" : [
                                                                                                                                                        "properties": [
                                                                                                                                                                "id"     : [
                                                                                                                                                                        "type": "string"
                                                                                                                                                                ],
                                                                                                                                                                "version": [
                                                                                                                                                                        "type": "integer"
                                                                                                                                                                ]
                                                                                                                                                        ]
                                                                                                                                                ]
                                                                                                                                        ]
                                                                                                                                ]
                                                                                                                        ]
                                                                                                                ]
                                                                                                        ]
                                                                                                ]
                                                                                        ]
                                                                                ]
                                                                        ]
                                                                ]
                                                        ]
                                                ]
                                ]
                        ]
                ]
        ]
        result = service.findProtocolEndpointDefinition(protocol, documentation)

        then:
        result == null

        when: // schema has changed should throw exception
        documentation = [
                paths: [
                        "opportunes/bulk": [
                                post: [
                                        requestBody:
                                                [
                                                        content: [
                                                                "application/json": [:]
                                                        ]
                                                ]
                                ]
                        ]
                ]
        ]
        service.findProtocolEndpointDefinition(protocol, documentation)

        then:
        thrown(NullPointerException)
    }

    void "resolveReferences should resolve \$ref variables in schema"() {
        given:
        def schema = [
                "plot-layout"                       : [
                        "\$ref": "#/components/schemas/PlotLayoutRequest"
                ],
                "basal-area-dbh-measure-observation": [
                        "type" : "array",
                        "items": [
                                "\$ref": "#/components/schemas/BasalAreaDbhMeasureObservationRequest"
                        ]
                ]
        ]
        def components = [
                "PlotLayoutRequest"                    : [
                        "type"      : "object",
                        "properties": [
                                "transect_number": ["type": "string"],
                                "date_time"      : ["type": "string", "format": "date-time"]
                        ]
                ],
                "BasalAreaDbhMeasureObservationRequest": [
                        "type"      : "object",
                        "properties": [
                                "transect": ["type": "string"],
                                "date"    : ["type": "string", "format": "date"]
                        ]
                ]
        ]

        when:
        def result = service.resolveReferences(schema, components)

        then:
        result.size() == 2
        result["plot-layout"].size() == 2
        result["plot-layout"].properties.transect_number.type == "string"
        result["plot-layout"].properties.date_time.format == "date-time"
        result["basal-area-dbh-measure-observation"].size() == 2
        result["basal-area-dbh-measure-observation"].items.properties.date.format == "date"
        result["basal-area-dbh-measure-observation"].items.properties.transect.type == "string"
    }

    void "convertToDataModelAndViewModel should return correct data model"() {
        when:// when integer type is provided
        String name = "height"
        def component = [
                "type"          : "integer",
                "x-paratoo-unit": "m",
                "x-paratoo-hint": "height"
        ]
        def config = new ParatooProtocolConfig(
                "name"          : "Opportune",
                "usesPlotLayout": false,
                "tags"          : ["survey"],
                "apiEndpoint"   : "s1s",
                "insertions": [
                        "dataModel": [
                                "<root>": [
                                        [
                                                "dataType": "text",
                                                "name": "countTypeIndividual",
                                                "dwcAttribute": "individualsOrGroups",
                                                "dwcExpression": "'Individuals'",
                                                "constraints": [
                                                        "Individuals",
                                                        "Groups"
                                                ]
                                        ]
                                ]
                        ]
                ],
                "overrides"     : [
                        "dataModel": null,
                        "viewModel": null
                        ]
                )
        def result = service.convertToDataModelAndViewModel(component, [:], name, null, null, 0, "", config)
        service.cacheService.clear()
        then:
        result.dataModel[0] == [
                "dataType"     : "number",
                "units"        : "m",
                "name"         : "height",
                "description"  : "height",
                "decimalPlaces": 0
        ]
        result.dataModel[1] == [
                "dataType": "text",
                "name": "countTypeIndividual",
                "dwcAttribute": "individualsOrGroups",
                "dwcExpression": "'Individuals'",
                "constraints": [
                        "Individuals",
                        "Groups"
                ]
        ]
        result.viewModel[0] == [
                "type"    : "number",
                "source"  : "height",
                "preLabel": "Height"
        ]
        when:// when number type is provided
        component = [
                "type"          : "number",
                "x-paratoo-unit": "m",
                "x-paratoo-hint": "height"
        ]
        result = service.convertToDataModelAndViewModel(component, [:], name, null, null, 0, "", config)
        then:
        result.dataModel[0] == [
                "dataType"     : "number",
                "units"        : "m",
                "name"         : "height",
                "description"  : "height",
                "decimalPlaces": 6
        ]
        result.dataModel[1] == [
                "dataType": "text",
                "name": "countTypeIndividual",
                "dwcAttribute": "individualsOrGroups",
                "dwcExpression": "'Individuals'",
                "constraints": [
                        "Individuals",
                        "Groups"
                ]
        ]
        result.viewModel[0] == [
                "type"    : "number",
                "source"  : "height",
                "preLabel": "Height"
        ]

        when:// when string type is provided
        service.cacheService.clear()
        component = [
                "type"          : "string",
                "x-paratoo-unit": "m",
                "x-paratoo-hint": "height",
                "x-lut-ref"     : "lut1"
        ]
        result = service.convertToDataModelAndViewModel(component, [:], name, null, null, 0, "", config)
        then:
        1 * webService.getJson( {it.indexOf('/lut1s') >= 0}, _, _, _) >> [data: [
                [
                        attributes: [
                                "symbol": "1",
                                "label" : "one"
                        ]
                ],
                [
                        attributes: [
                                "symbol": "2",
                                "label" : "two"
                        ]
                ]
        ], meta                            : [pagination: [total: 0]]]
        1 * webService.getJson({ it.indexOf('/documentation/swagger.json') >= 0 }, _, _, _) >> [
         paths: ["/lut1s": [:]]
        ]
        result.dataModel[0] == [
                "dataType"      : "text",
                "units"         : "m",
                "name"          : "height",
                "description"   : "height",
                "constraints"   : [
                        "textProperty" : "label",
                        "type"         : "literal",
                        "valueProperty": "value",
                        "literal"      : [
                                [
                                        label: "one",
                                        value: "1"
                                ],
                                [
                                        label: "two",
                                        value: "2"
                                ]
                        ]
                ],
                "displayOptions": [
                        "placeholder": "Select an option",
                        "tags"       : true
                ],
                "x-lut-ref":"lut1"
        ]
        result.dataModel[1] == [
                "dataType": "text",
                "name": "countTypeIndividual",
                "dwcAttribute": "individualsOrGroups",
                "dwcExpression": "'Individuals'",
                "constraints": [
                        "Individuals",
                        "Groups"
                ]
        ]
        result.viewModel[0] == [
                "type"    : "selectOne",
                "source"  : "height",
                "preLabel": "Height"
        ]
    }

    def "isLocationObject should identify object with 'location' name in component name" (Map input, boolean expected) {
        when:
        boolean result = service.isLocationObject(input)

        then:
        result == expected

        where:
        input | expected
        [(service.PARATOO_COMPONENT): "location.location"] | true
        [(service.PARATOO_COMPONENT): "location.plot-location-point"] | true
        [(service.PARATOO_COMPONENT): "location.fauna-plot-points"] | true
        [(service.PARATOO_COMPONENT): "location.vegetation-association-nvis"] | false
        [(service.PARATOO_COMPONENT): "location-observation-3"] | false
    }

    def "cleanSwaggerDefinition should clean and standardise given definitions" () {
        given:
        def definition = getNormalDefinition()
        when:
        def result = service.cleanSwaggerDefinition(definition.input)
        then:
        result == definition.output
    }

    def "recursivelyTransformData should transform species data"() {

        def dataModel = [
                [
                        "dataType"     : "species",
                        "name"         : "lut",
                        "x-lut-ref"    : "lut1",
                ]
        ]
        def output = [
                lut: [
                    "id": 8,
                    "symbol": "Cat",
                    "label": "Cat",
                    "description": "",
                    "uri": "",
                    "createdAt": "2024-03-26T02:39:32.116Z",
                    "updatedAt": "2024-03-26T02:39:32.116Z"
                ]
        ]
        String formName = "form name"

        when:
        def result = service.recursivelyTransformData(dataModel, output, formName, 1, null)
        result.lut.remove('outputSpeciesId')

        then:
        1 * speciesReMatchService.searchByName(_) >> [
                commonName: "Cat",
                scientificName: "Felis catus",
                guid: "TAXON_ID",
                taxonRank: "species"
        ]
        result == [
                lut: [
                        commonName: "Cat",
                        name: "Felis catus (Cat)",
                        taxonRank: "species",
                        scientificName: "Felis catus",
                        guid: "TAXON_ID"
                ]
        ]

        when:
        dataModel = [
                [
                        "dataType"     : "species",
                        "name"         : "lut"
                ]
        ]
        output = [
                lut: "Cats [Species] (scientific: Felis catus)"
        ]
        result = service.recursivelyTransformData(dataModel, output, formName, 1, null)
        result.lut.remove('outputSpeciesId')
        then:
        1 * speciesReMatchService.searchByName("Felis catus") >> null
        1 * speciesReMatchService.searchByName("Cats", false, true) >> [
                commonName: "Cat",
                scientificName: "Felis catus",
                guid: "TAXON_ID",
                taxonRank: "species"
        ]
        result == [
                lut: [
                        commonName: "Cats",
                        name: "Felis catus (Cats)",
                        taxonRank: "species",
                        scientificName: "Felis catus",
                        guid: "TAXON_ID"
                ]
        ]
    }

    def "recursivelyTransformData should transform feature object based on provided protocol config"() {
        given:
        def dataModel = [
                [
                        "dataType"     : "feature",
                        "name"         : "line"
                ]
        ]
        def output = [
                line: [[lat: 1, lng: 2], [lat: 3, lng: 4]]
        ]
        String formName = "form name"
        ParatooProtocolConfig config = new ParatooProtocolConfig(geometryType: "LineString")

        when:
        def result = service.recursivelyTransformData(dataModel, output, formName, 1, config)

        then:
        result == [
                line: [
                        type: "Feature",
                        geometry: [
                                type: "LineString",
                                coordinates: [[2, 1], [4, 3]]
                        ],
                        properties: [
                                name: "LineString form name-1",
                                description: "LineString form name-1",
                                externalId: null,
                                notes: "LineString form name-1"
                        ]
                ]
        ]

        when:
        output = [
                line: [[lat: 1, lng: 2], [lat: 3, lng: 4]]
        ]
        config = new ParatooProtocolConfig(geometryType: "Polygon")
        result = service.recursivelyTransformData(dataModel, output, formName, 1, config)

        then:
        result == [
                line: [
                        type: "Feature",
                        geometry: [
                                type: "Polygon",
                                coordinates: [[[2, 1], [4, 3], [2, 1]]]
                        ],
                        properties: [
                                name: "Polygon form name-1",
                                description: "Polygon form name-1",
                                externalId: null,
                                notes: "Polygon form name-1"
                        ]
                ]
        ]
    }

    def "The data set name will be updated after the callback to Monitor core and be created from available information"() {
        expect:
        ParatooService.buildUpdatedDataSetSummaryName("site", "2024-05-14T00:00:00Z", "2024-05-14T10:00:00Z", "Protocol 1", null, new ParatooProtocolConfig()) == "Protocol 1 (site) - 2024-05-14 10:00 ${am} to 2024-05-14 8:00 ${pm}"
        ParatooService.buildUpdatedDataSetSummaryName("site", "2024-05-14T00:00:00Z", null, "Protocol 1", null, new ParatooProtocolConfig(usesPlotLayout: false)) == "Protocol 1 - 2024-05-14 10:00 ${am}"
        ParatooService.buildUpdatedDataSetSummaryName(null, "2024-05-14T00:00:00Z", null, "Protocol 1", null, new ParatooProtocolConfig()) == "Protocol 1 - 2024-05-14 10:00 ${am}"
        ParatooService.buildUpdatedDataSetSummaryName(null, null, null, "Protocol 1", new ParatooCollectionId(eventTime:DateUtil.parse("2024-05-14T00:00:00Z")), new ParatooProtocolConfig()) == "Protocol 1 - 2024-05-14 10:00 ${am}"
    }

    def "Users with either the project participant or editor role can read all protocols and write all expect Plot Selection"(AccessLevel accessLevel, String protocolId, boolean canRead, boolean canWrite) {
        setup:
        String userId = 'u2'
        String projectId = 'p1' // created during setup

        when:
        UserPermission up = new UserPermission(userId: userId, accessLevel: accessLevel, entityId: projectId, entityType: Project.class.name)
        up.save(flush:true, failOnError: true)
        boolean actualCanRead = service.protocolReadCheck(userId, 'p1', protocolId)
        boolean actualCanWrite = service.protocolWriteCheck(userId, 'p1', protocolId)

        then:
        actualCanRead == canRead
        actualCanWrite == canWrite

        where:
        protocolId | accessLevel                     | canRead | canWrite
        'plot-selection-guid' | AccessLevel.editor   | false   | false
        'plot-selection-guid' | AccessLevel.admin    | true    | true
        'guid-2'   | AccessLevel.admin               | true    | true
        'guid-2'   | AccessLevel.caseManager         | true    | true
        'guid-2'   | AccessLevel.editor              | true    | true
        'guid-2'   | AccessLevel.projectParticipant  | true    | true
        'guid-2'   | AccessLevel.readOnly            | false   | false

        'guid-10' | AccessLevel.admin                | false   | false // Note guid-10 doesn't exist/isn't attached to the project.
    }

    def "buildTemplateForProtocol must switch record generation on or off" (createSpeciesRecord, expected) {
        setup:
        def documentation = [
                components: [schemas: [:]],
                paths: [
                        "opportunes/bulk": [
                                post: [
                                        requestBody:
                                                [
                                                        content: [
                                                                "application/json": [
                                                                        schema: [
                                                                                "properties": [
                                                                                        "data": [
                                                                                                "properties": [
                                                                                                        "collections": [
                                                                                                                "items": [
                                                                                                                        "properties": [
                                                                                                                                "surveyId": [
                                                                                                                                        "properties": [
                                                                                                                                                "projectId": [
                                                                                                                                                        "type": "string"
                                                                                                                                                ],
                                                                                                                                                "protocol" : [
                                                                                                                                                        "properties": [
                                                                                                                                                                "id"     : [
                                                                                                                                                                        "type": "string"
                                                                                                                                                                ],
                                                                                                                                                                "version": [
                                                                                                                                                                        "type": "integer"
                                                                                                                                                                ]
                                                                                                                                                        ]
                                                                                                                                                ]
                                                                                                                                        ]
                                                                                                                                ]
                                                                                                                        ]
                                                                                                                ]
                                                                                                        ]
                                                                                                ]
                                                                                        ]
                                                                                ]
                                                                        ]
                                                                ]
                                                        ]
                                                ]
                                ]
                        ]
                ]
        ]
        def config = new ParatooProtocolConfig(
                "name"          : "Opportune",
                "usesPlotLayout": false,
                "tags"          : ["survey"],
                "apiEndpoint"   : "s1s",
                "overrides"     : [
                        "dataModel": null,
                        "viewModel": null
                ]
        )

        if (createSpeciesRecord != "DO NOT ADD" ) {
            config.createSpeciesRecord = createSpeciesRecord
        }

        def protocol = [
                attributes: [
                        endpointPrefix: "opportunes",
                        workflow: [
                                [modelName: 'plot-layout'],
                                [modelName: 'other-model']
                        ]
                ]
        ]

        when:
        Map result = service.buildTemplateForProtocol(protocol, documentation, config)

        then:
        result.record == expected

        where:
        createSpeciesRecord | expected
        true | true
        false | false
        null | false
        "DO NOT ADD" | true
    }

    void "userCollection should return orgMintedId/dataSetId" (userId, expectedResult) {
        expect:
        service.userCollections(userId) == expectedResult

        where:
        userId | expectedResult
        "user1" | ['abc', 'def']
        "user2" | []
    }

    private Map getNormalDefinition() {
        def input = """
{
    "plot_points": {
      "type": "array",
      "items": {
        "properties": {
          "lat": {
            "type": "number",
            "format": "float"
          },
          "lng": {
            "type": "number",
            "format": "float"
          },
          "name": {
            "type": "string",
            "format": "string"
          }
        },
        "type": "object",
        "x-paratoo-component": "location.plot-location-point"
      },
      "maxItems": 25
    }
}
"""
        Map inputObject = getGroovyObject(input)
        Map output = ["plot_points": [type: "object", properties: ["lat": [type: "number", format: "float"], "lng": [type: "number", format: "float"], "name": [type: "string", format: "string"]], "x-paratoo-component": "location.plot-location-point"]]
        [input: inputObject, output: output]
    }

    private getGroovyObject(String input, Class clazz = Map.class){
        ObjectMapper mapper = new ObjectMapper()
        mapper.readValue(input, clazz)
    }

}
