package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.test.GrailsMock
import grails.test.spock.IntegrationSpec


class ActivityControllerSpec extends IntegrationSpec {

    def activityController = new ActivityController()
    // The original services
    def recordService

    def recordServiceStub = Stub(RecordService)

    def setup() {
        deleteAll()
        activityController.activityService.outputService.recordService = recordServiceStub
    }

    def cleanup() {
        deleteAll()
        activityController.activityService.outputService.recordService = recordService
    }

    private void deleteAll() {
        Activity.collection.remove(new BasicDBObject())
        Output.collection.remove(new BasicDBObject())
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
        def requestJson = [activityId:activityId,  outputs:[[name:'Revegetation Details', data:[prop1:'prop1', prop2:'prop2']],[name:'Participant Details', data:[prop3:'prop3', prop4:'prop4']]]]
        activityController.request.json = (requestJson as JSON).toString()

        recordServiceStub.updateRecord(_,_) >> {//Do nothing
        }

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

        def reveg = savedActivity.outputs.find{it.name == 'Revegetation Details'}
        reveg != null
        reveg.data.prop1 == 'prop1'
        reveg.data.prop2 == 'prop2'

        def particpantDetails = savedActivity.outputs.find{it.name == 'Participant Details'}
        particpantDetails != null
        particpantDetails.data.prop3 == 'prop3'
        particpantDetails.data.prop4 == 'prop4'

    }


}
