package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.ParatooService
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooProtocolId
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

/**
 * Tests for the ParatooService.
 * The tests are incomplete as some of the behaviour needs to be specified.
 */
class ParatooServiceSpec extends Specification implements ServiceUnitTest<ParatooService>, DataTest {

    String userId = 'u1'
    SiteService siteService = Mock(SiteService)
    ProjectService projectService = Mock(ProjectService)

    static Map DUMMY_POLYGON = [type:'Polygon', coordinates: [[[1,2], [2,2], [2, 1], [1,1], [1,2]]]]

    def setup() {
        mockDomain(Project)
        mockDomain(ActivityForm)
        mockDomain(Service)
        mockDomain(UserPermission)
        mockDomain(Program)
        setupData()

        service.siteService = siteService
        service.projectService = projectService
    }

    def cleanup() {
    }

    void "The service can map user projects into a format useful for the paratoo API"() {

        when:
        List<ParatooProject> projects = service.userProjects(userId)

        then:
        projects.size() == 1
        projects[0].id == "p1"
        projects[0].name == "Project 1"
        projects[0].accessLevel == AccessLevel.admin
        projects[0].projectArea.siteId == 's1'
        projects[0].plots.size() == 1
        projects[0].plots[0].siteId == 's2'

    }

    void "The service can create a data set from a submitted collection"() {
        setup:
        ParatooProtocolId protocol = new ParatooProtocolId(id:1, version: 1)
        ParatooCollection collection = new ParatooCollection(projectId:'p1', mintedCollectionId:"c1", userId:'u1', protocol:protocol, eventTime:DateUtil.parse('2023-01-01T00:00:00Z'))
        Map expectedDataSet = [dataSetId:'c1', grantId:'g1']

        when:
        Map result = service.createCollection(collection)

        then:
        1 * projectService.update([custom:[dataSets:[expectedDataSet]]], 'p1', false) >> [status:'ok']

        and:
        result == [status:'ok']
    }

    private void setupData() {
        Project project = new Project(projectId:"p1", name:"Project 1", grantId:"g1", programId:"prog1", custom:[details:[serviceIds:[1]]])
        project.save(failOnError:true, flush:true)
        UserPermission userPermission = new UserPermission(accessLevel: AccessLevel.admin, userId: userId, entityId:'p1', entityType:Project.name)
        userPermission.save(failOnError:true, flush:true)

        Site projectArea = new Site(siteId:'s1', name:'Site 1', type:Site.TYPE_PROJECT_AREA, extent: DUMMY_POLYGON)
        Site plot = new Site(siteId:'s2', name:"Site 2", type:Site.TYPE_WORKS_AREA, extent: DUMMY_POLYGON)
        siteService.sitesForProject('p1') >> [projectArea, plot]

        Program program = new Program(programId: "prog1", name:"A program", config:[(ParatooService.PROGRAM_CONFIG_PARATOO_ITEM):true])
        program.save(failOnError:true, flush:true)

        Service service = new Service(name:"S1", serviceId:'1', legacyId: 1, outputs:[new ServiceForm(externalId:2, formName:"aParatooForm", sectionName:null)])
        service.save(failOnError:true, flush:true)

        ActivityForm activityForm = new ActivityForm(name:"aParatooForm", externalId: 2, type:'Paratoo')
        activityForm.save(failOnError:true, flush:true)
    }
}
