package au.org.ala.ecodata

import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import spock.lang.Specification

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import grails.util.GrailsWebMockUtil
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse

@Integration
class CommentControllerIntegrationSpec extends Specification {

    @Autowired
    CommentController commentController

    @Autowired
    WebApplicationContext ctx
//    CommentController commentController = new CommentController()

    def grailsApplication

    def setup() {
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)

        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def "list() should not return DELETED comments"() {
        setup:
        new Comment(status: ACTIVE, entityType: "bla", entityId: "1", text: "bla", userId: "111").save(failOnError: true, flush: true)
        new Comment(status: DELETED, entityType: "bla", entityId: "1", text: "bla", userId: "111").save(failOnError: true, flush: true)
        new Comment(status: ACTIVE, entityType: "bla", entityId: "1", text: "bla", userId: "111").save(failOnError: true, flush: true)
        new Comment(status: DELETED, entityType: "bla", entityId: "1", text: "bla", userId: "111").save(failOnError: true, flush: true)
        new Comment(status: ACTIVE, entityType: "bla2", entityId: "1", text: "bla", userId: "111").save(failOnError: true, flush: true)
        new Comment(status: ACTIVE, entityType: "bla", entityId: "12", text: "bla", userId: "111").save(failOnError: true, flush: true)

        when:
        commentController.params.entityType = "bla"
        commentController.params.entityId = "1"
        Comment.withTransaction {
            commentController.list()
        }

        then:
        def resp = extractJson(commentController.response.text)
        resp.items.size() == 2
       // commentController.response.json.items.size() == 2
    }

    def extractJson (String str) {
        if(str.indexOf('{') > -1 && str.indexOf('}') > -1) {
            String jsonStr = str.substring(str.indexOf('{'), str.lastIndexOf('}') + 1)
            new JsonSlurper().parseText(jsonStr)
        }
    }
}
