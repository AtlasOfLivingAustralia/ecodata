package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

import static au.org.ala.ecodata.Status.DELETED


class ActivityServiceIntegrationSpec extends IntegrationSpec {
    ActivityService activityService

    def grailsApplication

    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def "delete should soft delete the project and all related records when destroy = false"() {
        setup:
        Activity activity = createHierarchy()

        when:
        activityService.delete(activity.activityId, false)

        then:
        Project.count() == 1
        Document.count() == 2
        Activity.count() == 1
        Output.count() == 2
        Record.count() == 2
        Document.findAll().each { assert it.status == DELETED }
        Activity.findAll().each { assert it.status == DELETED }
        Output.findAll().each { assert it.status == DELETED }
        Record.findAll().each { assert it.status == DELETED }
    }

    def "delete should hard delete the project and all related records when destroy = true"() {
        setup:
        Activity activity = createHierarchy()

        expect:
        Activity.count() == 1

        when:
        activityService.delete(activity.activityId, true)

        then:
        Project.count() == 1
        Document.count() == 0
        Activity.count() == 0
        Output.count() == 0
        Record.count() == 0
    }

    private static createHierarchy() {
        Project project = new Project(projectId: "project1", name: "project1").save(failOnError: true, flush: true)
        Activity activity1 = new Activity(activityId: "act1", projectId: project.projectId).save(failOnError: true, flush: true)
        new Document(documentId: "doc1", activityId: activity1.activityId).save(failOnError: true, flush: true)
        new Document(documentId: "doc2", activityId: activity1.activityId).save(failOnError: true, flush: true)
        Output output1 = new Output(outputId: "out1", activityId: activity1.activityId).save(failOnError: true, flush: true)
        Output output2 = new Output(outputId: "out2", activityId: activity1.activityId).save(failOnError: true, flush: true)
        new Record(outputId: output1.outputId).save(failOnError: true, flush: true)
        new Record(outputId: output2.outputId).save(failOnError: true, flush: true)

        activity1
    }
}
