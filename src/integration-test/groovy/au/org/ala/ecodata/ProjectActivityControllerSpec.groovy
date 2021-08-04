package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import grails.util.GrailsWebMockUtil
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse

@Integration
class ProjectActivityControllerSpec extends Specification {

    @Autowired
    ProjectActivityController projectActivityController

    @Autowired
    WebApplicationContext ctx
   // def projectActivityController = new ProjectActivityController()

    def setup() {
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)
    }

    def cleanup() {
        ProjectActivity.collection.remove(new BasicDBObject())
    }

    void "test create project activity"() {
        setup:

        def visibility = [:]
        visibility.embargoOption = 'NONE'
        visibility.alaAdminEnforcedEmbargo = true
        def projectActivity = [name           : 'Test Project Activity',
                               description    : 'Test description',
                               status         : 'active',
                               commentsAllowed: true,
                               startDate      : '2015-06-17T14:00:00Z',
                               endDate        : '2015-06-17T14:00:00Z',
                               publicAccess   : true,
                               visibility     : visibility,
                               dynamicProperty: 'dynamicProperty',
                               projectId      : 'test-project-id',
                               methodType     : "opportunistic",
                               methodName     : "Opportunistic/ad-hoc observation recording",
                               isDataManagementPolicyDocumented: false,
                               dataAccessMethods: ["na"],
                               dataQualityAssuranceMethods: ["dataownercurated"],
                               "nonTaxonomicAccuracy": "low",
                               "temporalAccuracy": "low",
                               "speciesIdentification": "low",
                               "spatialAccuracy": "low",
                               dataSharingLicense: "CC BY"
        ]
        projectActivityController.request.contentType = 'application/json;charset=UTF-8'
        projectActivityController.request.content = (projectActivity as JSON).toString().getBytes('UTF-8')
        projectActivityController.request.method = 'POST'

        when: "creating a project"
        projectActivityController.update('') // Empty or null ID triggers a create
        def resp = extractJson (projectActivityController.response.text)

        then: "ensure we get a response including a projectActivityId"
        def projectActivityId = resp.projectActivityId
        projectActivityController.response.contentType == 'application/json;charset=UTF-8'
        resp.message == 'created'
        projectActivityId != null

        when: "retrieving the new projectActivity"
        projectActivityController.response.reset()
        def savedProjectActivity = []
        def project
        ProjectActivity.withTransaction {
            projectActivityController.getAllByProject(projectActivity.projectId)
        }
        project = extractJson(projectActivityController.response.text)
        savedProjectActivity.addAll(project?.list)
            // To support JSONP the controller returns a model object, which is transformed to JSON via a filter.


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
        savedProjectActivity[0].visibility.alaAdminEnforcedEmbargo == projectActivity.visibility.alaAdminEnforcedEmbargo
        savedProjectActivity[0].visibility.embargoOption == projectActivity.visibility.embargoOption
    }

    def extractJson (String str) {
        if(str.indexOf('{') > -1 && str.indexOf('}') > -1) {
            String jsonStr = str.substring(str.indexOf('{'), str.lastIndexOf('}') + 1)
            new JsonSlurper().parseText(jsonStr)
        }
    }

}
