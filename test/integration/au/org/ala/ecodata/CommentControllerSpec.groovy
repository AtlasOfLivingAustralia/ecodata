package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class CommentControllerSpec extends IntegrationSpec {

    CommentController commentController = new CommentController()

    def grailsApplication

    def setup() {
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
        commentController.list()

        then:
        commentController.response.json.items.size() == 2
    }
}
