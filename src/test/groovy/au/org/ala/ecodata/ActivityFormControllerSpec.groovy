package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class ActivityFormControllerSpec extends Specification implements ControllerUnitTest<ActivityFormController>, DataTest {

    ActivityFormService activityFormService = Mock(ActivityFormService)

    Class[] getDomainClassesToMock() {
        [ActivityForm, Activity]
    }

    def setup() {
        controller.activityFormService = activityFormService
    }

    def cleanup() {
    }

    void "Get activity form by name"() {
        when:
        params.name = 'test'
        controller.get()

        then:
        1 * activityFormService.findActivityForm('test', null) >> new ActivityForm(name:'test', formVersion:1, status: Status.ACTIVE, type:'Activity')
        response.status == HttpStatus.SC_OK
    }

    void "Get activity form by name and version"() {
        when:
        params.name = 'test'
        params.formVersion = 1
        controller.get()

        then:
        1 * activityFormService.findActivityForm('test', 1) >> new ActivityForm(name:'test', formVersion:1, status: Status.ACTIVE, type:'Activity')
        response.status == HttpStatus.SC_OK
    }

    void "Get activity form by name - invalid activity form"() {
        when:
        params.name = 'test'
        controller.get()

        then:
        1 * activityFormService.findActivityForm('test', null) >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    void "New draft"() {
        when:
        params.name = 'test'
        controller.newDraftForm()

        then:
        1 * activityFormService.newDraft('test') >> new ActivityForm(name:'test', formVersion:1, status: Status.ACTIVE, type:'Activity')
        response.status == HttpStatus.SC_OK
    }

    void "New draft - invalid form"() {
        when:
        params.name = 'test'
        controller.newDraftForm()

        then:
        1 * activityFormService.newDraft('test') >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    void "publish"() {
        when:
        params.name = 'test'
        params.formVersion = 1
        controller.publish()

        then:
        1 * activityFormService.publish('test', 1) >> new ActivityForm(name:'test', formVersion:1, status: Status.ACTIVE, type:'Activity')
        response.status == HttpStatus.SC_OK
    }

    void "publish - invalid form"() {
        when:
        params.name = 'test'
        params.formVersion = 1
        controller.publish()

        then:
        1 * activityFormService.publish('test', 1) >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    void "unpublish"() {
        when:
        params.name = 'test'
        params.formVersion = 1
        controller.unpublish()

        then:
        1 * activityFormService.unpublish('test', 1) >> new ActivityForm(name:'test', formVersion:1, status: Status.ACTIVE, type:'Activity')
        response.status == HttpStatus.SC_OK
    }

    void "unpublish - invalid form"() {
        when:
        params.name = 'test'
        params.formVersion = 1
        controller.unpublish()

        then:
        1 * activityFormService.unpublish('test', 1) >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    void "Find uses of form"() {
        setup:
        new Activity(activityId:'1', formVersion:1, status: Status.ACTIVE, type:'Activity').save()
        new Activity(activityId:'2', formVersion:1, status: Status.DELETED, type:'Activity').save()

        when:
        params.name = 'Activity'
        params.formVersion = 1
        controller.findUsesOfForm()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().count == 1
    }

    void "Find uses of form - invalid form"() {
        when:
        params.name = 'test'
        params.formVersion = 1
        controller.findUsesOfForm()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().count == 0
    }

    void "Create activity form"() {
        setup:
        Map props = [
               formVersion:1, status: Status.ACTIVE, type:'Activity', name:'a1'
        ]
        ActivityForm activityForm = new ActivityForm(formVersion:1, status: Status.ACTIVE, type:'Activity',  name:'a1')

        when:
        request.method = 'POST'
        request.json = props
        controller.create()

        then:
        response.status == HttpStatus.SC_OK
        ActivityForm responseData = response.json as ActivityForm
        responseData.name == 'a1'
        responseData.status == Status.ACTIVE
    }

    void "Update activity form"() {
        setup:
        Map props = [
                formVersion:1, status: Status.ACTIVE, type:'ActivityUpdated', name:'a1'
        ]
        ActivityForm activityForm = new ActivityForm( formVersion:1, status: Status.ACTIVE, type:'Activity', name:'a1')

        when:
        request.method = 'PUT'
        request.json = props
        controller.update()

        then:
        1 * activityFormService.findActivityForm('a1', 1) >> activityForm

        response.status == HttpStatus.SC_OK
        ActivityForm responseData = response.json as ActivityForm
        responseData.name == 'a1'
        responseData.status == Status.ACTIVE
        responseData.type == 'ActivityUpdated'
    }

    void "The controller delegates a search request to the ActivityFormService"() {
        setup:
        Map criteria = [type:"Protocol", options:[max:10]]

        when:
        request.method = 'POST'
        request.json = criteria
        controller.search()

        then:
        1 * activityFormService.search([type:"Protocol"], [max:10]) >> [[name:"Test form", formVersion:2]]
        response.status == HttpStatus.SC_OK
        List<Map> responseData = response.json
        responseData.size() == 1
        responseData[0].name == "Test form"
        responseData[0].formVersion == 2
    }

    void "The search request requires at least one criteria"() {

        when:
        request.method = 'POST'
        request.json = [:]
        controller.search()

        then:
        0 * activityFormService.search(_,_)
        response.status == HttpStatus.SC_BAD_REQUEST
    }
}
