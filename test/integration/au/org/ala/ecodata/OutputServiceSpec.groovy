package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

import static au.org.ala.ecodata.Status.DELETED

class OutputServiceSpec extends IntegrationSpec {

    OutputService outputService
    def grailsApplication

    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    void "test create output with no parent activity"() {
        when:
            def response = outputService.create(data:[prop2:'prop2'])
        then:
            response.error != null
            response.status == 'error'
    }

    void "test create output"() {
        setup:
            def activityId = 'a test activity id'
            Activity activity = new Activity(activityId:activityId, type: 'Revegetation', description: 'A test activity')
            activity.save(flush:true, failOnError: true)

        when:
            def response = outputService.create(activityId:activityId, data:[prop2:'prop2'])
            def outputId = response.outputId
        then:
            outputId != null
            response.status == 'ok'

        when: "retrieving the saved output"
            Output savedOutput = Output.findByOutputId(outputId)

        then:
            savedOutput.outputId == outputId
            savedOutput.activityId == activityId
            savedOutput['data']['prop2'] == 'prop2'
    }

    def "deleteProject should soft delete the project and all related records when destroy = false"() {
        setup:
        Output output = createHierarchy()

        when:
        outputService.delete(output.outputId, false)

        then:
        Output.count() == 1
        Document.count() == 2
        Record.count() == 2
        Document.findAll().each { assert it.status == DELETED }
        Output.findAll().each { assert it.status == DELETED }
        Record.findAll().each { assert it.status == DELETED }
    }

    def "deleteProject should hard delete the project and all related records when destroy = true"() {
        setup:
        Output output = createHierarchy()

        when:
        outputService.delete(output.outputId, true)

        then:
        Document.count() == 0
        Output.count() == 0
        Record.count() == 0
    }

    private static createHierarchy() {
        Output output = new Output(outputId: "out2", activityId: "bla").save(failOnError: true, flush: true)
        new Record(outputId: output.outputId).save(failOnError: true, flush: true)
        new Record(outputId: output.outputId).save(failOnError: true, flush: true)
        new Document(documentId: "doc5", outputId: output.outputId).save(failOnError: true, flush: true)
        new Document(documentId: "doc6", outputId: output.outputId).save(failOnError: true, flush: true)

        output
    }
}
