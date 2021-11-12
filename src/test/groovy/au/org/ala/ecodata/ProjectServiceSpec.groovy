package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller

import java.text.SimpleDateFormat

class ProjectServiceSpec extends MongoSpec implements ServiceUnitTest<ProjectService> {

    ProjectActivityService projectActivityServiceStub = Stub(ProjectActivityService)
    WebService webServiceStub = Stub(WebService)
    SiteService siteService = Mock(SiteService)
    DocumentService documentService = Mock(DocumentService)
    ActivityService activityService = Mock(ActivityService)
    ReportingService reportingService = Mock(ReportingService)
    MetadataService metadataService = Mock(MetadataService)

    String collectoryBaseUrl = ''
    String meritDataProvider = 'drMerit'
    String biocollectDataProvider = 'drBiocollect'
    String dataProviderId = 'dp1'
    String dataResourceId = 'dr1'

    boolean loadExternalBeans() {
        true
    }


    def setup() {

        deleteAll()

        defineBeans {
            commonService(CommonService)
            collectoryService(CollectoryService)
        }

        grailsApplication.config.collectory = [baseURL:collectoryBaseUrl, dataProviderUid:[merit:meritDataProvider, biocollect:biocollectDataProvider], collectoryIntegrationEnabled: true]
        grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
        grailsApplication.mainContext.collectoryService.grailsApplication = grailsApplication
        grailsApplication.mainContext.collectoryService.webService = webServiceStub
        grailsApplication.mainContext.collectoryService.projectService = service
        service.collectoryService = grailsApplication.mainContext.collectoryService
        service.projectActivityService = projectActivityServiceStub
        service.siteService = siteService
        service.activityService = activityService
        service.reportingService = reportingService
        service.documentService = documentService
        service.grailsApplication = grailsApplication
        service.metadataService = metadataService

        webServiceStub.doPost(collectoryBaseUrl+"ws/dataResource", _) >> [:]
        webServiceStub.extractIdFromLocationHeader(_) >> dataResourceId
        webServiceStub.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, _) >> [:]

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    def deleteAll() {
        Program.collection.remove(new BasicDBObject())
        Project.collection.remove(new BasicDBObject())
        ManagementUnit.collection.remove(new BasicDBObject())
        AuditMessage.collection.remove(new BasicDBObject())
    }

    def cleanup() {
        Project.findAll().each { it.delete(flush:true) }
        AuditMessage.findAll().each { it.delete(flush:true) }
    }

    def "test create and update project"() {
        given:
        def projData = [name:'test proj', description: 'test proj description', dynamicProperty: 'dynamicProperty', isBushfire:true, bushfireCategories: null, alaHarvest: true]
        def updatedData = projData + [description: 'test proj updated description', origin: 'atlasoflivingaustralia']

        def result, projectId
        when:
        Project.withNewTransaction {
            result = service.create(projData)
            projectId = result.projectId
        }
        then: "ensure the response contains the id of the new project"
        result.status == 'ok'
        projectId != null

        when: "select the new project back from the database"
        def savedProj = Project.findByProjectId(projectId)


        then: "ensure the properties are the same as the original"
        savedProj.name == projData.name
        savedProj.description == projData.description
        // The collectory update process is on a separate thread so the dataResourceId generally isn't updated in time.
        //savedProj.dataResourceId == dataResourceId
        //savedProj['dynamicProperty'] == projData.dynamicProperty  The dbo property on the domain object appears to be missing during unit tests which prevents dynamic properties from being retreived.

        when:
        Project.withNewTransaction {
            result = service.update(updatedData, projectId)
        }
        then: "ensure the response status is ok and the project was updated"
        result.status == 'ok'


        when: "select the updated project back from the database"
        savedProj = Project.findByProjectId(projectId)


        then: "ensure the unchanged properties are the same as the original"
        savedProj.name == projData.name
        //savedProj['dynamicProperty'] == projData.dynamicProperty  The dbo property on the domain object appears to be missing during unit tests which prevents dynamic properties from being retreived.

        then: "ensure the updated properties are the same as the change"
        savedProj.description == updatedData.description

        then: "categories can be null for tagged bushfire recovery projects"
        savedProj.isBushfire == updatedData.isBushfire
        savedProj.bushfireCategories == updatedData.bushfireCategories

    }

    def "test project validation"() {
        given:
        def projData = [description: 'test proj description', dynamicProperty: 'dynamicProperty']

        when:
        def result = service.create(projData)

        then:
        result.status == 'error'
        result.error != null

    }

    def "Program names will be returned in the ALL view if the project references the program by programId"() {

        setup:
        Project project = new Project(projectId: 'p1', name: "A project", programId: 'program2')
        Program program = new Program(programId: 'program1', name: "Program 1")
        Program child = new Program(programId: 'program2', name: "Child Program", parent: program)
        Project.withTransaction {
            program.save(failOnError: true)
            child.save(failOnError: true)
        }

        project.metaClass.getDbo = { new BasicDBObject(project.properties) }

        when:
        Map result = null
        Project.withTransaction {
            result = service.toMap(project, 'all')
        }

        then:
        result.projectId == project.projectId
        result.name == project.name
        result.programId == project.programId
        result.associatedProgram == program.name
        result.associatedSubProgram == child.name

    }

    def "The project supports Risks as an embedded mapping"() {
        setup:
        String projectId = 'p1-risks'
        Project project = new Project(projectId:projectId, name:"Project")
        Map risks = buildRisksData()

        when:
        Map result
        project.save(flush:true, failOnError: true)
        result = service.update([risks:risks], projectId, false)


        then:
        result == [status:"ok"]

        when:
        Project project2 = Project.findByProjectId(projectId)

        then:
        project2.risks.overallRisk == risks.overallRisk
        project2.risks.dateUpdated ==  DateUtil.parse(risks.dateUpdated)
        project2.risks.rows.size() == risks.rows.size()
        int i=0
        for (Risk risk : project2.risks.rows) {
            risk.consequence == risks.rows[i].consequence
            risk.likelihood == risks.rows[i].consequence
            risk.residualRisk == risks.rows[i].consequence
            risk.currentControl == risks.rows[i].consequence
            risk.description == risks.rows[i].consequence
            risk.threat == risks.rows[i].consequence
            risk.riskRating == risks.rows[i].consequence
            i++
        }

        when:
        Map projectData
        Project.withNewTransaction { tx ->
            Project p = Project.findByProjectId(projectId)
            projectData = service.toMap(p, ProjectService.FLAT)
        }


        then:
        projectData.risks.overallRisk == risks.overallRisk
        projectData.risks.dateUpdated == DateUtil.parse(risks.dateUpdated)
        projectData.risks.rows.size() == risks.rows.size()
        int j=0
        for (Map risk : projectData.risks.rows) {
            risk.consequence == risks.rows[j].consequence
            risk.likelihood == risks.rows[j].consequence
            risk.residualRisk == risks.rows[j].consequence
            risk.currentControl == risks.rows[j].consequence
            risk.description == risks.rows[j].consequence
            risk.threat == risks.rows[j].consequence
            risk.riskRating == risks.rows[j].consequence
            j++
        }
    }

    private Map buildRisksData() {
        List risks = [["consequence" : "Moderate",
                       "likelihood" : "Likely",
                       "residualRisk" : "Low",
                       "currentControl" : "programme control works to occur over a range of seasons throughout the year to minimise the risk of not being able to control invasive weeds",
                       "description" : "Seasonal Drought leading to inability to control weeds due to drought stress",
                       "threat" : "Seasonal conditions (eg. drought, flood, etc.)",
                       "riskRating" : "Significant"
                      ],[
                        "consequence" : "Major",
                        "likelihood" : "Possible",
                        "residualRisk" : "Medium",
                        "currentControl" : "review SWMS and undertake site risk assessment in accordance with Council policy",
                        "description" : "injury or death to staff or project partner",
                        "threat" : "Workplace health & safety (eg. Project staff and / or delivery partner injury or death)",
                        "riskRating" : "Significant"
                ]]
        [overallRisk:'High', dateUpdated:'2020-07-01T14:00:00Z', rows: risks]
    }


    void "Get brief"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        Project project2 = new Project(projectId: 'p2', name: "A project 2", status:  Status.DELETED)
        project2.save(flush: true, failOnError: true)

        def listOfIds = ['p1', 'p2']

        when:
        def response = service.getBrief(listOfIds)

        then:
        response != null
        response.size() == 1
        response[0].projectId == 'p1'
        response[0].name == 'A project 1'
    }

    void "Get brief - empty list"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        Project project2 = new Project(projectId: 'p2', name: "A project 2", status:  Status.DELETED)
        project2.save(flush: true, failOnError: true)

        def listOfIds = []

        when:
        def response = service.getBrief(listOfIds)

        then:
        response != null
        response.size() == 0
    }

    void "Get brief - with version"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        Project project2 = new Project(projectId: 'p2', name: "A project 2", status:  Status.DELETED)
        project2.save(flush: true, failOnError: true)

       def listOfIds = ['p1', 'p2']

        when:
        def response = service.getBrief(listOfIds, '1')

        then:
        response != null
    }

    void "Get by project id"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        when:
        def response = service.get('p1', [service.BASIC])

        then:
        response != null
        response.projectId == 'p1'
        response.name == 'A project 1'
    }

    void "Get by project id -  invalid id"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        when:
        def response = service.get('p2')

        then:
        response == null
    }

    void "Get project services"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        when:
        def response = service.getProjectServicesWithTargets('p1')
        def p = service.get('p1')

        then:
        1 * metadataService.getProjectServicesWithTargets(service.get('p1')) >> []
        response != null
    }

    void "Get project services -  invalid id"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1")
        project1.save(flush: true, failOnError: true)

        when:
        def response = service.getProjectServicesWithTargets('p2')

        then:
        response == null
    }

    void "Get by data resource id"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1')
        project1.save(flush: true, failOnError: true)

        when:
        def response = service.getByDataResourceId('1', 'active', [service.BASIC])

        then:
        response != null
        response.projectId == 'p1'
        response.name == 'A project 1'
    }

    void "Get by data resource id -  invalid id"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1')
        project1.save(flush: true, failOnError: true)

        when:
        def response = service.getByDataResourceId('2')

        then:
        response == null
    }

    void "list"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', isCitizenScience: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', isCitizenScience: true, status: Status.DELETED)
        project2.save(flush: true, failOnError: true)

        when:
        def response = service.list([service.BASIC])

        then:
        response != null
        response.projectId == ['p1']
        response.name == ['A project 1']
    }

    void "list -  include deleted"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', isCitizenScience: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', isCitizenScience: false, status: Status.DELETED)
        project2.save(flush: true, failOnError: true)

        when:
        def response = service.list([service.BASIC], true, false)

        then:
        response != null
        response.size() == 2
        response[0].projectId == 'p1'
        response[0].name == 'A project 1'
        response[1].projectId == 'p2'
        response[1].name == 'A project 2'
    }

    void "list citizen Science projects"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', isCitizenScience: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', isCitizenScience: true, status: Status.DELETED)
        project2.save(flush: true, failOnError: true)

        when:
        def response = service.list([service.BASIC], false, true)

        then:
        response != null
        response.projectId == ['p1']
        response.name == ['A project 1']
    }

    void "list citizen Science projects -  include deleted"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', isCitizenScience: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', isCitizenScience: true, status: Status.DELETED)
        project2.save(flush: true, failOnError: true)
        when:
        def response = service.list([service.BASIC], true, true)

        then:
        response != null
        response.size() == 2
        response[0].projectId == 'p1'
        response[0].name == 'A project 1'
        response[1].projectId == 'p2'
        response[1].name == 'A project 2'
    }

    void "list - empty"() {
        setup:
        when:
        def response = service.list([service.BASIC], true, true)

        then:
        response != null
        response.size() == 0
    }

    void "list merit projects"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', isMERIT: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', isMERIT: true, status: Status.DELETED)
        project2.save(flush: true, failOnError: true)

        when:
        def response = service.listMeritProjects([service.BASIC], false)

        then:
        response != null
        response.projectId == ['p1']
        response.name == ['A project 1']
    }

    void "list merit projects -  include deleted"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', isMERIT: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', isMERIT: true, status: Status.DELETED)
        project2.save(flush: true, failOnError: true)
        when:
        def response = service.listMeritProjects([service.BASIC], true)

        then:
        response != null
        response.size() == 2
        response[0].projectId == 'p1'
        response[0].name == 'A project 1'
        response[1].projectId == 'p2'
        response[1].name == 'A project 2'
    }

    void "list promoted"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', promoteOnHomepage: 'yes')
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', promoteOnHomepage: 'no')
        project2.save(flush: true, failOnError: true)

        when:
        def response = service.promoted()

        then:
        response != null
        response.projectId == ['p1']
        response.name == ['A project 1']
    }

    void "list projects for Ala harvesting"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', alaHarvest: true)
        project1.save(flush: true, failOnError: true)
        Project project2 = new Project(projectId: 'p2', name: "A project 2", dataResourceId: '1', alaHarvest: false)
        project2.save(flush: true, failOnError: true)

        when:
        def response = service.listProjectForAlaHarvesting([max : 10, offset: 1, order: 'projectId', sort: 'desc'])

        then:
        response != null
        response.total == 1
    }

    void "list projects for Ala harvesting - empty"() {
        setup:

        when:
        def response = service.listProjectForAlaHarvesting([max : 10, offset: 1, order: 'projectId', sort: 'desc'])

        then:
        response != null
        response.total == 0
        response.list.size() == 0
    }

    void "load all"() {
        setup:
        def props = [[projectId: 'p1', name: "A project 1", dataResourceId: '1']]

        when:
        def response = service.loadAll(props)

        then:
        response != null
        response.size() == 1
        response[0].projectId == 'p1'
    }

    void "create - invalid"() {
        setup:
        Project project1 = new Project(projectId: 'p1', name: "A project 1", dataResourceId: '1', alaHarvest: true)
        project1.save(flush: true, failOnError: true)
        def props = [projectId: 'p1', name: "A project 1", dataResourceId: '1']

        when:
        def response = service.create(props)

        then:
        response != null
        response.status == 'error'
        response.error == 'Duplicate project id for create p1'
    }

}
