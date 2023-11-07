package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooProtocolId
import au.org.ala.ecodata.paratoo.ParatooSurveyId
import au.org.ala.ws.tokens.TokenService
import com.nimbusds.oauth2.sdk.token.AccessToken
import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller

/**
 * Tests for the ParatooService.
 * The tests are incomplete as some of the behaviour needs to be specified.
 */
class ParatooServiceSpec extends MongoSpec implements ServiceUnitTest<ParatooService>{

    String userId = 'u1'
    SiteService siteService = Mock(SiteService)
    ProjectService projectService = Mock(ProjectService)
    WebService webService = Mock(WebService)
    TokenService tokenService = Mock(TokenService)
    SettingService settingService = Mock(SettingService)

    static Map DUMMY_POLYGON = [type:'Polygon', coordinates: [[[1,2], [2,2], [2, 1], [1,1], [1,2]]]]

    def setup() {

        deleteAll()
        setupData()

        service.webService = webService
        service.siteService = siteService
        service.projectService = projectService
        service.permissionService = new PermissionService() // Using the real permission service for this test
        service.tokenService = tokenService
        service.settingService = settingService

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    private void deleteAll() {
        Hub.findAll().each {it.delete()}
        Project.findAll().each {it.delete()}
        ActivityForm.findAll().each {it.delete()}
        Service.findAll().each {it.delete()}
        UserPermission.findAll().each {it.delete()}
        Program.findAll().each {it.delete()}
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
        projects[0].protocols*.name == ["aParatooForm 1", "aParatooForm 2", "aParatooForm 3"]

        and:
        1 * siteService.geometryAsGeoJson({it.siteId == 's1'}) >> DUMMY_POLYGON

    }

    void "Starred projects won't be included unless the user has a hub permission"() {

        setup:
        UserPermission userPermission = UserPermission.findByUserId(userId)
        userPermission.accessLevel = AccessLevel.starred
        userPermission.save(flush:true, failOnError:true)

        when:
        List<ParatooProject> projects = service.userProjects(userId)

        then:
        projects.size() == 0

        when: "The user has the MERIT grant manager role"
        UserPermission meritGrantManager = new UserPermission(userId:userId, entityId:'merit', entityType: 'au.org.ala.ecodata.Hub', accessLevel:AccessLevel.caseManager)
        meritGrantManager.save(flush:true, failOnError:true)
        projects = service.userProjects(userId)

        then:
        projects.size() == 1
    }

    void "If the user has starred a project and also has a role, the role will be used"() {

            setup:
            UserPermission userPermission = new UserPermission(userId:userId, entityId:'p1', entityType: 'au.org.ala.ecodata.Project', accessLevel:AccessLevel.starred)
            userPermission.save(flush:true, failOnError:true)

            when:
            List<ParatooProject> projects = service.userProjects(userId)

            then:
            projects.size() == 1
            projects[0].accessLevel == AccessLevel.admin

    }

    void "The service can create a data set from a submitted collection"() {
        setup:

        String projectId = 'p1'
        ParatooProtocolId protocol = new ParatooProtocolId(id:"guid-2", version: 1)
        ParatooSurveyId surveyId = new ParatooSurveyId(projectId:projectId, protocol:protocol, surveyType:"api", time:new Date(), randNum:1l)
        ParatooCollectionId collectionId = new ParatooCollectionId(surveyId:surveyId)

        when:
        Map result = service.mintCollectionId('u1', collectionId)

        then:
        1 * projectService.update(_, projectId, false) >> {data, pId, updateCollectory ->
            Map dataSet = data.custom.dataSets[1]  // The stubbed project already has a dataSet, so the new one will be index=1
            assert dataSet.surveyId.time == surveyId.timeAsISOString()
            assert dataSet.surveyId.randNum == surveyId.randNum
            assert dataSet.surveyId.surveyType == surveyId.surveyType
            assert dataSet.protocol == surveyId.protocol.id
            assert dataSet.grantId == "g1"
            assert dataSet.progress == 'planned'
            assert dataSet.name == "aParatooForm 1 - ${DateUtil.formatAsDisplayDate(surveyId.time)} (Project 1)"

            [status:'ok']
        }

        and:
        result.status == 'ok'
        result.orgMintedIdentifier != null
    }

    void "The service can create a data set from a submitted collection"() {
        setup:
        String projectId = 'p1'
        ParatooProtocolId protocol = new ParatooProtocolId(id:1, version: 1)
        ParatooCollection collection = new ParatooCollection(projectId:projectId, orgMintedIdentifier:"org1", userId:'u1', protocol:protocol)
        Map dataSet =  [dataSetId:'d1', orgMintedIdentifier:'org1', grantId:'g1', surveyId:[surveyType:'s1', randNum:1, projectId:projectId, protocol: protocol, time:'2023-09-01T00:00:00.123Z']]
        Map expectedDataSet = dataSet+[progress:Activity.STARTED]
        ParatooProject project = new ParatooProject(id:projectId, project:new Project(projectId:projectId, custom:[dataSets:[dataSet]]))
        when:
        Map result = service.submitCollection(collection, project)

        then:
        1 * webService.getJson({it.indexOf('/s1s') >= 0}, null, _, false) >> [data:[], meta:[pagination:[total:0]]]
        1 * tokenService.getAuthToken(true) >> Mock(AccessToken)
        1 * projectService.update([custom:[dataSets:[expectedDataSet]]], 'p1', false) >> [status:'ok']

        and:
        result == [status:'ok']
    }
    
    void "The service can create a site from a submitted plot-selection"() {
        setup:
        Map data = [
                "plot_name":["state":1,"program":9,"bioregion":3,"unique_digits":"2222"],
                "plot_label":"CTMAUA2222",
                "recommended_location":["lat":-35.2592424,"lng":149.0651439],
                "recommended_location_point":12,
                "uuid":"lmpisy5p9g896lad4ut",
                "comment":"Test",
                "plot_selection_survey":5]

        Map expected = ['name':'CTMAUA2222', 'description':'CTMAUA2222', 'externalId':'lmpisy5p9g896lad4ut', 'notes':'Test', 'extent':['geometry':['type':'Point', 'coordinates':[149.0651439, -35.2592424], 'decimalLatitude':-35.2592424, 'decimalLongitude':149.0651439], 'source':'point'], 'projects':[], 'type':'surveyArea']

        String userId = 'u1'

        when:
        service.addOrUpdatePlotSelections(userId, data)

        then:
        1 * siteService.create(expected)
    }

    void "The service can link a site to a project"() {
        setup:
        String projectId = 'p1'
        ParatooProject project = new ParatooProject(id:projectId, project:new Project(projectId:projectId))
        Map data = [plot_selections:['s2']]

        when:
        service.updateProjectSites(project, data)
        Site s2 = Site.findBySiteId('s2')

        then:
        s2.projects.indexOf(projectId) >= 0
    }

    void "The service can create a project area"() {
        setup:
        String projectId = 'p1'
        ParatooProject project = new ParatooProject(id:projectId, project:new Project(projectId:projectId))
        Map data = [project_area_type:'polygon', project_area_coordinates: [
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
        Map expectedSite = [name:"Monitor project area", type:Site.TYPE_PROJECT_AREA, projects:[projectId],
            extent:[source:"drawn", geometry: [type:'Polygon', coordinates:[[[138.6845397949219, -34.96643621094802], [138.66394042968753, -35.003565839769166], [138.59973907470706, -34.955744257334246], [138.6845397949219, -34.96643621094802]]]]]]

        when:
        service.updateProjectSites(project, data)

        then:
        1 * siteService.create(expectedSite)
    }

    private void setupData() {
        Hub hub = new Hub(hubId:"merit", urlPath:"merit")
        hub.save(failOnError:true, flush:true)
        Project project = new Project(projectId:"p1", name:"Project 1", grantId:"g1", programId:"prog1", hubId:"merit",
                custom:[details:[
                        serviceIds:[1],
                        baseline:[rows:[[protocols:['protocol category 1']]]],
                        monitoring:[rows:[[protocols:['protocol category 2', 'protocol category 3']]]]
                ], dataSets: [[
                    dataSetId:'c1'
                ]]])
        project.save(failOnError:true, flush:true)
        UserPermission userPermission = new UserPermission(accessLevel: AccessLevel.admin, userId: userId, entityId:'p1', entityType:Project.name)
        userPermission.save(failOnError:true, flush:true)

        Site projectArea = new Site(siteId:'s1', name:'Site 1', type:Site.TYPE_PROJECT_AREA, extent: [geometry:DUMMY_POLYGON])
        projectArea.save(failOnError:true, flush:true)
        Site plot = new Site(siteId:'s2', name:"Site 2", type:Site.TYPE_SURVEY_AREA, extent: [geometry:DUMMY_POLYGON], projects:['p1'])
        plot.save(failOnError:true, flush:true)
        siteService.sitesForProject('p1') >> [projectArea, plot]

        Program program = new Program(programId: "prog1", name:"A program", config:[(ParatooService.PROGRAM_CONFIG_PARATOO_ITEM):true])
        program.save(failOnError:true, flush:true)

        Service service = new Service(name:"S1", serviceId:'1', legacyId: 1, outputs:[new ServiceForm(externalId:"guid-2", formName:"aParatooForm", sectionName:null)])
        service.save(failOnError:true, flush:true)

        ActivityForm activityForm = new ActivityForm(name:"aParatooForm 1", type:'EMSA', category:'protocol category 1', external: true)
        activityForm.externalIds = [new ExternalId(externalId: "guid-2", idType:ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError:true, flush:true)

        activityForm = new ActivityForm(name:"aParatooForm 2 ", type:'EMSA', category:'protocol category 2', external: true)
        activityForm.externalIds = [new ExternalId(externalId: "guid-3", idType:ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError:true, flush:true)

        activityForm = new ActivityForm(name:"aParatooForm 3", type:'EMSA', category:'protocol category 3', external: true)
        activityForm.externalIds = [new ExternalId(externalId: "guid-4", idType:ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        activityForm.save(failOnError:true, flush:true)
    }
}
