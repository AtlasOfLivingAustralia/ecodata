package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.test.spock.IntegrationSpec


class ProjectActivityControllerSpec extends IntegrationSpec {
    def projectActivityController = new ProjectActivityController()

    def setup() {
    }

    def cleanup() {
        ProjectActivity.collection.remove(new BasicDBObject())
    }

    void "test create project activity"() {
        setup:
        def projectActivity = [name           : 'Test Project Activity',
                               description    : 'Test description',
                               status         : 'active',
                               commentsAllowed: true,
                               startDate      : '2015-06-17T14:00:00Z',
                               endDate        : '2015-06-17T14:00:00Z',
                               publicAccess   : true,
                               dynamicProperty: 'dynamicProperty',
                               projectId      : 'test-project-id']
        projectActivityController.request.contentType = 'application/json;charset=UTF-8'
        projectActivityController.request.content = (projectActivity as JSON).toString().getBytes('UTF-8')
        projectActivityController.request.method = 'POST'

        when: "creating a project"
        def resp = projectActivityController.update('') // Empty or null ID triggers a create

        then: "ensure we get a response including a projectActivityId"
        def projectActivityId = resp.projectActivityId
        projectActivityController.response.contentType == 'application/json;charset=UTF-8'
        resp.message == 'created'
        projectActivityId != null

        when: "retrieving the new projectActivity"
        projectActivityController.response.reset()
        def savedProjectActivity = []
        savedProjectActivity.addAll(projectActivityController.getAllByProject(projectActivity.projectId)?.list) // To support JSONP the controller returns a model object, which is transformed to JSON via a filter.

        then: "ensure the properties are the same as the original"
        savedProjectActivity?.size() > 0
        savedProjectActivity[0].projectActivityId == projectActivityId
        savedProjectActivity[0].name == projectActivity.name
        savedProjectActivity[0].description == projectActivity.description
        savedProjectActivity[0].status == projectActivity.status
        savedProjectActivity[0].commentsAllowed == projectActivity.commentsAllowed
        savedProjectActivity[0].projectId == projectActivity.projectId
        savedProjectActivity[0].dynamicProperty == projectActivity.dynamicProperty
        savedProjectActivity[0].publicAccess == projectActivity.publicAccess
    }
}
