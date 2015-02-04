package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

class OutputServiceSpec extends IntegrationSpec {

    def outputService
    def grailsApplication

    def setup() {
        Output.deleteAll()
        Activity.deleteAll()
    }

    def cleanup() {
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

}
