package au.org.ala.ecodata

import grails.converters.JSON
//import grails.test.mixin.Mock
import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import grails.util.GrailsWebMockUtil
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse

@Integration
class ProjectControllerIntegrationSpec extends Specification {

    @Autowired
    ProjectController projectController

    @Autowired
    WebApplicationContext ctx

    def commonService
    def projectService

    def setup() {
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)

        projectController.projectService = projectService
        projectController.projectService.collectoryService = Mock(CollectoryService)
        projectController.projectService.webService = Mock(WebService)
        //projectController.projectService.grailsApplication = [mainContext: [commonService: commonService],config: [collectory: [baseURL: "test"]]]
        projectController.projectService.grailsApplication.config.collectory.baseURL = "test"
    }

    def cleanup() {
    }

//    void "test create project"() {
//
//        setup:
//        projectController.projectService.collectoryService.createDataProviderAndResource(_, _) >> [:]
//        def project = [name: 'Test Project', description: 'Test description', dynamicProperty: 'dynamicProperty']
//        projectController.request.contentType = 'application/json;charset=UTF-8'
//        projectController.request.content = (project as JSON).toString().getBytes('UTF-8')
//        projectController.request.method = 'POST'
//
//        when: "creating a project"
//        projectController.update('') // Empty or null ID triggers a create
//        def resp = extractJson(projectController.response.text)
//
//        then: "ensure we get a response including a projectId"
//        projectController.response.text.contains("created")
//        projectController.response.status == 200
//        resp != null
//        def projectId = resp.projectId
//        projectController.response.contentType == 'application/json;charset=UTF-8'
//        resp.message == 'created'
//        projectId != null
//
//
//        when: "retrieving the new project"
//        projectController.response.reset()
//        Project.withTransaction {
//            projectController.get(projectId)
//            // To support JSONP the controller returns a model object, which is transformed to JSON via a filter.
//        }
//        def savedProject = extractJson(projectController.response.text)
//
//        then: "ensure the properties are the same as the original"
//        savedProject.projectId == projectId
//        savedProject.name == project.name
//        savedProject.description == project.description
//        savedProject.dynamicProperty == project.dynamicProperty
//    }

    def extractJson (String str) {
        if(str.indexOf('{') > -1 && str.indexOf('}') > -1) {
            String jsonStr = str.substring(str.indexOf('{'), str.lastIndexOf('}') + 1)
            new JsonSlurper().parseText(jsonStr)
        }
    }

}
