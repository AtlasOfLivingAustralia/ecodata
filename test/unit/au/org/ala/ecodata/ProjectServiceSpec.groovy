package au.org.ala.ecodata

import com.github.fakemongo.Fongo
import com.mongodb.BasicDBObject
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
// Added ControllerUnitTestMixin to include grails JSON converter. It is used in a service class.
@TestMixin([MongoDbTestMixin,ControllerUnitTestMixin])
class ProjectServiceSpec extends Specification {

    ProjectService service = new ProjectService()
    ProjectActivityService projectActivityServiceStub = Stub(ProjectActivityService)
    WebService webServiceStub = Stub(WebService)
    SiteService siteService = Mock(SiteService)
    DocumentService documentService = Mock(DocumentService)
    ActivityService activityService = Mock(ActivityService)
    ReportingService reportingService = Mock(ReportingService)

    String collectoryBaseUrl = ''
    String meritDataProvider = 'drMerit'
    String biocollectDataProvider = 'drBiocollect'
    String dataProviderId = 'dp1'
    String dataResourceId = 'dr1'

    static doWithConfig(config) {
        config.grails.databinding.dateFormats = ["yyyy-MM-dd'T'HH:mm:ss'Z'"]
    }

    def setup() {
        mongoDomain([Project, Program, ManagementUnit])
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

        webServiceStub.doPost(collectoryBaseUrl+"ws/dataResource", _) >> [:]
        webServiceStub.extractIdFromLocationHeader(_) >> dataResourceId
        webServiceStub.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, _) >> [:]

    }

    def deleteAll() {
        Program.collection.remove(new BasicDBObject())
        Project.collection.remove(new BasicDBObject())
        ManagementUnit.collection.remove(new BasicDBObject())
    }

    def cleanup() {
        deleteAll()
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
        savedProj.dataResourceId == dataResourceId
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
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        String projectId = 'p1'
        Project project = new Project(projectId:projectId, name:"Project")
        Map risks = buildRisksData()

        when:
        Map result
        Project.withTransaction {
            project.save(flush:true, failOnError: true)
            result = service.update([risks:risks], projectId, false)
        }

        then:
        result == [status:"ok"]

        when:
        Project.withTransaction {
            project = Project.findByProjectId(projectId)
        }

        then:
        project.risks.overallRisk == risks.overallRisk
        project.risks.dateUpdated ==  f.parse(risks.dateUpdated)
        project.risks.rows.size() == risks.rows.size()
        int i=0
        for (Risk risk : project.risks.rows) {
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
        Project.withTransaction {
            projectData = service.get(projectId, ProjectService.FLAT)
        }

        then:
        projectData.risks.overallRisk == risks.overallRisk
        projectData.risks.dateUpdated == f.parse(risks.dateUpdated)
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
}
