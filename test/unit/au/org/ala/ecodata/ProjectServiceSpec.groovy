package au.org.ala.ecodata

import com.github.fakemongo.Fongo
import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */

@TestMixin(MongoDbTestMixin)
class ProjectServiceSpec extends Specification {

    ProjectService service = new ProjectService()
    def stubbedCollectoryService = Stub(CollectoryService)

    def setup() {
        Fongo fongo = new Fongo("ecodata-test")
        mongoDomain(fongo.mongo, [Project])

        defineBeans {
            commonService(CommonService)
        }
        grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
        service.grailsApplication = grailsApplication
        service.collectoryService = stubbedCollectoryService
    }

    def "test create and update project"() {
        given:
        def projData = [name:'test proj', description: 'test proj description', dynamicProperty: 'dynamicProperty']
        def dataProviderId = 'dp1'
        def dataResourceId = 'dr1'
        stubbedCollectoryService.createDataResource(_,_) >> [dataResourceId: dataResourceId]
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

    }

    def "test project validation"() {
        given:
        def projData = [description: 'test proj description', dynamicProperty: 'dynamicProperty']
        stubbedCollectoryService.createDataProviderAndResource(_,_) >> ""

        when:
        def result = service.create(projData)

        then:
        result.status == 'error'
        result.error != null

    }
}
