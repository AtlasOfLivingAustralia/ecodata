package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooProtocolId
import au.org.ala.ecodata.paratoo.ParatooSurveyId
import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Tests for the ParatooService.
 * The tests are incomplete as some of the behaviour needs to be specified.
 */
class ParatooServiceSpec extends MongoSpec implements ServiceUnitTest<ParatooService>{

    String userId = 'u1'
    SiteService siteService = Mock(SiteService)
    ProjectService projectService = Mock(ProjectService)
    WebService webService = Mock(WebService)

    static Map DUMMY_POLYGON = [type:'Polygon', coordinates: [[[1,2], [2,2], [2, 1], [1,1], [1,2]]]]

    def setup() {
//        mockDomain(Project)
//        mockDomain(ActivityForm)
//        mockDomain(Service)
//        mockDomain(UserPermission)
//        mockDomain(Program)
//        mockDomain(Hub)
        deleteAll()
        setupData()

        service.webService = webService
        service.siteService = siteService
        service.projectService = projectService
        service.permissionService = new PermissionService() // Using the real permission service for this test

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

        when: "The user has the MERIT read only role"
        UserPermission meritReadOnly = new UserPermission(userId:userId, entityId:'merit', entityType: 'au.org.ala.ecodata.Hub', accessLevel:AccessLevel.readOnly)
        meritReadOnly.save(flush:true, failOnError:true)
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

        ParatooProtocolId protocol = new ParatooProtocolId(id:"guid-2", version: 1)
        ParatooSurveyId surveyId = new ParatooSurveyId(surveyType:"api", time:new Date(), randNum:1l)
        String projectId = 'p1'
        ParatooCollectionId collectionId = new ParatooCollectionId(projectId:projectId, surveyId:surveyId, protocol:protocol)

        when:
        Map result = service.mintCollectionId(collectionId)

        then:
        1 * projectService.update(_, projectId, false) >> {data, pId, updateCollectory ->
            Map dataSet = data.custom.dataSets[1]  // The stubbed project already has a dataSet, so the new one will be index=1
            assert dataSet.surveyId.time == surveyId.timeAsISOString()
            assert dataSet.surveyId.randNum == surveyId.randNum
            assert dataSet.surveyId.surveyType == surveyId.surveyType
            assert dataSet.grantId == "g1"
            assert dataSet.activitesStartDate == DateUtil.format(surveyId.time)
            assert dataSet.progress == 'started'
            assert dataSet.name == "aParatooForm 1 - ${DateUtil.formatAsDisplayDate(surveyId.time)} (Project 1)"

            [status:'ok']
        }

        and:
        result.status == 'ok'
        result.orgMintedIdentifier != null
    }

    @Ignore // Ignoring this while doing actual integration testing.
    void "The service can create a data set from a submitted collection"() {
        setup:
        ParatooProtocolId protocol = new ParatooProtocolId(id:1, version: 1)
        ParatooCollection collection = new ParatooCollection(projectId:'p1', mintedCollectionId:"c1", userId:'u1', protocol:protocol, eventTime:DateUtil.parse('2023-01-01T00:00:00Z'))
        Map expectedDataSet = [dataSetId:'c1', grantId:'g1']

        when:
        Map result = service.submitCollection(collection)

        then:
        1 * webService.getJson(_, null, null, false) >> [data:[:]]
        1 * projectService.update([custom:[dataSets:[expectedDataSet]]], 'p1', false) >> [status:'ok']

        and:
        result == [status:'ok']
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
        Site plot = new Site(siteId:'s2', name:"Site 2", type:Site.TYPE_WORKS_AREA, extent: [geometry:DUMMY_POLYGON])
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
