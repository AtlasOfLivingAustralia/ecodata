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
    CollectoryService stubbedCollectoryService = Stub(CollectoryService)
    DocumentService documentService = Mock(DocumentService)
    UserService userService = Mock(UserService)

    def setup() {
        Fongo fongo = new Fongo("ecodata-test")
        mongoDomain(fongo.mongo, [Project])

        defineBeans {
            commonService(CommonService)
        }
        grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
        service.grailsApplication = grailsApplication
        service.collectoryService = stubbedCollectoryService
        service.documentService = documentService
        service.userService = userService
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

    def "The most recent entry in a project MERI plan approval history can be found and returned"() {
        setup:
        String projectId = 'p1'
        List documents = []
        (1..5).each {
            documents << buildApprovalDocument(it, projectId)
        }
        userService.lookupUserDetails('1234') >> [displayName:'test']
        int count = 0

        when:
        Map mostRecentMeriPlanApproval = service.getMostRecentMeriPlanApproval(projectId)

        then:
        1 * documentService.search([projectId:projectId, role:'approval', labels:'MERI']) >> [documents:documents]
        5 * documentService.readJsonDocument(_) >> {documents[count++].content}

        mostRecentMeriPlanApproval.approvalDate == '2019-07-01T00:00:05Z'
        mostRecentMeriPlanApproval.approvedBy == 'test'
    }

    private Map buildApprovalDocument(int i, String projectId) {
        Map approval = [
                dateApproved:"2019-07-01T00:00:0${i}Z",
                approvedBy:'1234',
                reason:'r',
                referenceDocument: 'c',
                project: [projectId:projectId]
        ]
        Map document = [documentId:i, projectId:projectId, url:'url'+i, content:approval]

        document
    }
}
