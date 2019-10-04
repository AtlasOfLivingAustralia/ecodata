package au.org.ala.ecodata

import grails.transaction.Rollback

import static au.org.ala.ecodata.Status.DELETED
import spock.lang.Specification
import grails.testing.mixin.integration.Integration

@Integration
//@Rollback
class ActivityServiceIntegrationSpec extends Specification {
    ActivityService activityService

    def grailsApplication

    // The original services
    def recordService

    def recordServiceStub = Stub(RecordService)

    def setup() {
        activityService.outputService.recordService = recordServiceStub
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def cleanup() {
        //  deleteAll()
        activityService.outputService.recordService = recordService
    }


    void "test create an activity"() {
        setup:
        def activity = [type: 'Revegetation', projectId:'a project', description: 'Test activity', dynamicProperty: 'dynamicProperty']
        //activityController.request.json = (activity as JSON).toString()

        when: "creating the activity"
        def response
        Activity.withTransaction {
            response = activityService.create(activity)
        }
        //message = [message: 'created', activityId: result.activityId]
        //activityController.update('')
        // def response = extractJson(activityController.response.text)
        def activityId = response.activityId

        then:
        //activityController.response.contentType == 'application/json;charset=UTF-8'
        response.status == 'ok'
        activityId != null


        when: "retrieving the new activity"
        def savedActivity
        Activity.withTransaction {
            savedActivity = activityService.get(activityId)
        }

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
        //def requestJson = [activityId:activityId,  outputs:[[name:'Revegetation Details', data:[prop1:'prop1', prop2:'prop2']],[name:'Participant Details', data:[prop3:'prop3', prop4:'prop4']]]]
        def requestJson = [activityId:activityId,  outputs:[[name:'Revegetation Details', data:[prop1:'prop1', prop2:'prop2']],[name:'Participant Details', data:[prop3:'prop3', prop4:'prop4']]]]
        recordServiceStub.updateRecord(_,_) >> {//Do nothing
        }

        when: "update the activity to include the output details"
        //activityController.response.reset()
        //activityController.update(activityId)
        def response
        Activity.withTransaction {
            response = activityService.update(requestJson, activityId)
        }
//        def response = extractJson(activityController.response.text)

        then:
        response.status == 'ok'

        when: "retrieving the updated activity"
//        activityController.response.reset()
        def savedActivity
        Activity.withTransaction {
            savedActivity = activityService.get(activityId)
        }
        //def savedActivity = extractJson(activityController.response.text)

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

    def "delete should soft delete the project and all related records when destroy = false"() {
        setup:
        Activity activity = createHierarchy()

        when:
        Activity.withTransaction {
            activityService.delete(activity.activityId, false)
        }

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
        Activity.withTransaction {
            activityService.delete(activity.activityId, true)
        }

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
