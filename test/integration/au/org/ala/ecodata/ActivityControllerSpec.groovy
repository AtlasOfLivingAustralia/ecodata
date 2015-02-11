package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.spock.IntegrationSpec


class ActivityControllerSpec extends IntegrationSpec {

    def activityController = new ActivityController()

    def setup() {
        Activity.deleteAll()
        Output.deleteAll()
    }

    def cleanup() {
    }

    void "test create an activity"() {
        setup:
        def activity = [type: 'Revegetation', projectId:'a project', description: 'Test activity', dynamicProperty: 'dynamicProperty']
        activityController.request.json = (activity as JSON).toString()

        when: "creating the activity"
        def response = activityController.update('')
        def activityId = response.activityId

        then:
        activityController.response.contentType == 'application/json;charset=UTF-8'
        response.message == 'created'
        activityId != null


        when: "retrieving the new activity"
            activityController.response.reset()
            def savedActivity = activityController.get(activityId) // To support JSONP the controller returns a model object, which is transformed to JSON via a filter.

        then: "ensure the properties are the same as the original"
            savedActivity.projectId == activity.projectId
            savedActivity.description == activity.description
            savedActivity.type == activity.type
            savedActivity.dynamicProperty == activity.dynamicProperty


    }

    void "update an activity - including outputs"() {
        setup:
        def activityId = 'activity_1'
        Activity activity = new Activity(type: 'Revegetation', projectId:'a project', description: 'Test activity', dynamicProperty: 'dynamicProperty', activityId:activityId)
        activity.save(flush: true, failOnError: true)
        def outputs = [outputs:[[name:'Revegetation Details', data:[prop1:'prop1', prop2:'prop2']],[name:'Participant Details', data:[prop3:'prop3', prop4:'prop4']]]]
        activityController.request.json = (outputs as JSON).toString()

        when: "update the activity to include the output details"
        def response = activityController.update(activityId)

        then:
        response.message == 'updated'

        when: "retrieving the updated activity"
        activityController.response.reset()
        def savedActivity = activityController.get(activityId)

        then: "ensure the properties are correct, including the outputs"
        savedActivity.projectId == activity.projectId
        savedActivity.description == activity.description
        savedActivity.type == activity.type
        savedActivity.dynamicProperty == activity.dynamicProperty
        savedActivity.outputs.size() == 2
        savedActivity.outputs[0].name == 'Revegetation Details'
        savedActivity.outputs[0].data.prop1 == 'prop1'
        savedActivity.outputs[0].data.prop1 == 'prop2'

        savedActivity.outputs[1].name == 'Participant Details'
        savedActivity.outputs[1].data.prop3 == 'prop3'
        savedActivity.outputs[1].data.prop4 == 'prop4'

    }


}
