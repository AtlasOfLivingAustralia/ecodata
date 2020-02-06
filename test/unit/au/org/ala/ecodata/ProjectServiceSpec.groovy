package au.org.ala.ecodata

import com.github.fakemongo.Fongo
import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Specification
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
// Added ControllerUnitTestMixin to include grails JSON converter. It is used in a service class.
@TestMixin([MongoDbTestMixin,ControllerUnitTestMixin])
class ProjectServiceSpec extends Specification {

    ProjectService service = new ProjectService()
    def webServiceStub = Stub(WebService)
    String collectoryBaseUrl = ''
    String meritDataProvider = 'drMerit'
    String biocollectDataProvider = 'drBiocollect'
    String dataProviderId = 'dp1'
    String dataResourceId = 'dr1'

    def setup() {
        Fongo fongo = new Fongo("ecodata-test")
        mongoDomain(fongo.mongo, [Project])

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
        service.grailsApplication = grailsApplication

        webServiceStub.doPost(collectoryBaseUrl+"ws/dataResource", _) >> [:]
        webServiceStub.extractIdFromLocationHeader(_) >> dataResourceId
        webServiceStub.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, _) >> [:]

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
}
