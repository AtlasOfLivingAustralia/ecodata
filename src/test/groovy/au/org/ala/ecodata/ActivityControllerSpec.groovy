package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.elasticsearch.action.search.SearchResponse
import spock.lang.Specification

import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX

class ActivityControllerSpec extends Specification implements ControllerUnitTest<ActivityController>, DataTest {

    ActivityService activityService = Mock(ActivityService)
    ProjectActivityService projectActivityService = Mock(ProjectActivityService)
    CommonService commonService = new CommonService()
    ElasticSearchService elasticSearchService = Mock(ElasticSearchService)

    Class[] getDomainClassesToMock() {
        [Activity]
    }

    def setup() {
        controller.activityService = activityService
        controller.projectActivityService = projectActivityService
        controller.commonService = commonService
        controller.commonService.grailsApplication = grailsApplication
        controller.elasticSearchService = elasticSearchService
        controller.elasticSearchService.grailsApplication = grailsApplication
    }

    def cleanup() {
    }

    void "Get activities"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        controller.index()

        then:
        response.status == HttpStatus.SC_OK
        response.text == '1 activities'
    }

    void "Get activity by id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.get()

        then:
        1 * activityService.get('1', [], null, null, false) >> activity
        response.status == HttpStatus.SC_OK
        Activity responseData = response.json as Activity
        responseData.activityId == '1'
    }

    void "Get activity by id - invalid id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '2'
        controller.get()

        then:
        1 * activityService.get('2', [], null, null, false) >> null
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "Get activity by id - without providing id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        controller.get()

        then:
        1 * activityService.getAll(null, null) >> [activity]
        response.status == HttpStatus.SC_OK
        response.getJson().list.size() == 1
        response.getJson().list[0].activityId == '1'
    }

    void "delete activity by id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.delete()

        then:
        1 * activityService.delete('1', false) >> [status: 'ok']
        response.status == HttpStatus.SC_OK
        response.text == 'deleted'
    }

    void "delete activity by id - invalid id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '2'
        controller.delete()

        then:
        1 * activityService.delete('2', false) >> [status: 'not found']
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "delete by project activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.deleteByProjectActivity()

        then:
        1 * activityService.deleteByProjectActivity('1', false) >> [status: 'ok']
        response.status == HttpStatus.SC_OK
        response.text == 'deleted'
    }

    void "delete by project activity - invalid id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '2'
        controller.deleteByProjectActivity()

        then:
        1 * activityService.deleteByProjectActivity('2', false) >> [status: 'not found']
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

//    void "delete by project activity - error"() {
//        setup:
//        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
//                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
//
//        when:
//        params.id = '1'
//        controller.deleteByProjectActivity()
//
//        then:
//        1 * activityService.deleteByProjectActivity('1', false) >> [status: 'error', error: "Error deleting activities"]
//        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
//        response.text == 'Error deleting activities'
//    }

    void "bulk delete"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [
                ids:['1']
        ]

        when:
        request.json = props
        controller.bulkDelete()

        then:
        1 * activityService.bulkDelete(['1'], false) >> [ success : true]
        response.status == HttpStatus.SC_OK
        response.text == '{"message":"deleted","details":{"success":true}}'
    }

    void "bulk delete - invalid ids"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [  ids:null ]

        when:
        request.json = props
        controller.bulkDelete()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == '{"message":"Please provide property \\"ids\\" in JSON payload"}'
    }

    void "Create activity"() {
        setup:
        Map props = [
                activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED, plannedStartDate:new Date(), plannedEndDate: new Date()
        ]

        when:
        request.json = props
        controller.update()

        then:
        1 * activityService.create(request.JSON) >> [status: 'ok', activityId: '1']

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'created'
        response.getJson().activityId == '1'
    }

    void "Update activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [
                type:'Type 1 updated'
        ]

        when:
        request.json = props
        controller.update('1')

        then:
        1 * activityService.update(request.JSON, '1', false) >> [status: 'ok']

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'updated'
    }

    void "Update activity - error"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [
                type:'Type 1 updated'
        ]

        when:
        request.json = props
        controller.update('1')

        then:
        1 * activityService.update(request.JSON, '1', false) >> [status:'error', error:"error"]

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'error'
        response.getJson().errors[0].error == 'error'
    }

    void "update bulk activity - without ids"() {
        setup:
        Map props = [
                type:'Type 1 updated'
        ]

        when:
        params.id = null
        request.json = props
        controller.bulkUpdate()

        then:
        1 * activityService.bulkUpdate(props, []) >> [status: 'error']

        response.status == HttpStatus.SC_BAD_REQUEST
        response.getJson().message == 'The id parameter is mandatory'
    }

    void "update bulk activity - without params"() {
        setup:

        when:
        params.id = ['1']
        controller.bulkUpdate()

        then:
        1 * activityService.bulkUpdate(request.JSON, ['1']) >> [status: 'error']

        response.status == HttpStatus.SC_BAD_REQUEST
        response.getJson().message == 'The properties to be updated must be supplied in the request body'
    }

    void "Update bulk activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [
                type:'Type 1 updated'
        ]

        when:
        params.id = ['1']
        request.json = props
        controller.bulkUpdate()

        then:
        1 * activityService.bulkUpdate(props, ['1']) >> [status: 'ok']

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'updated'
    }

    void "Update bulk activity - error"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [
                type:'Type 1 updated'
        ]

        when:
        params.id = ['1']
        request.json = props
        controller.bulkUpdate()

        then:
        1 * activityService.bulkUpdate(props, ['1']) >> [status:'error', error:"error"]

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'error'
        response.getJson().errors[0].error == 'error'
    }

    void "Get activities for project"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.activitiesForProject()

        then:
        1 * activityService.findAllForProjectId('1', ['scores']) >> [activity]
        response.status == HttpStatus.SC_OK
        response.getJson().list.size() == 1
        response.getJson().list[0].activityId == '1'
    }

    void "Get activities for project - invalid id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = null
        controller.activitiesForProject()

        then:
        0 * activityService.findAllForProjectId('1', ['scores'])
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "Get activities for user"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.listForUser()

        then:
        1 * activityService.findAllForUserId('1', [max: 10,offset:0,order:'desc',sort:'lastUpdated']) >> [total: 1, list:[activity]]
        response.status == HttpStatus.SC_OK
        response.getJson().activities.size() == 1
        response.getJson().activities[0].activityId == '1'
        response.getJson().total == 1
    }

    void "Get activities for user - invalid id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = null
        controller.listForUser()

        then:
        0 * activityService.findAllForUserId()
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "List activities for project"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        params.userId = '1'
        controller.listByProject()

        then:
        1 * projectActivityService.listRestrictedProjectActivityIds('1') >> []
        1 * activityService.listByProjectId('1', [max: 10,offset:0,order:'desc',sort:'lastUpdated'], []) >> [total: 1, list:[activity]]
        response.status == HttpStatus.SC_OK
        response.getJson().activities.size() == 1
        response.getJson().activities[0].activityId == '1'
        response.getJson().total == 1
    }

    void "List activities for project - invalid id"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = null
        params.userId = '1'
        controller.listByProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "is User Owner For Activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        params.userId = '1'
        controller.isUserOwnerForActivity()

        then:
        1 * activityService.isUserOwner('1', '1') >> true
        response.status == HttpStatus.SC_OK
        response.getJson().userIsOwner == true
    }

    void "is User Owner For Activity - without id"() {

        when:
        params.id = null
        params.userId = '1'
        controller.isUserOwnerForActivity()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "is User Owner For Activity - without user id"() {
        when:
        params.id = '1'
        params.userId = null
        controller.isUserOwnerForActivity()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Invalid userId'
    }

    void "count by project Activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.countByProjectActivity()

        then:
        1 * activityService.countByProjectActivityId('1') >> 1
        response.status == HttpStatus.SC_OK
        response.getJson().total == 1
    }

    void "count by project activity - without id"() {

        when:
        params.id = null
        controller.countByProjectActivity()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "Get sites for project Activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.getDistinctSitesForProjectActivity()

        then:
        1 * activityService.getDistinctSitesForProjectActivity('1') >> [activity]
        response.status == HttpStatus.SC_OK
    }

    void "Get sites for project activity - without id"() {

        when:
        params.id = null
        controller.getDistinctSitesForProjectActivity()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "Get sites for project"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        controller.getDistinctSitesForProject()

        then:
        1 * activityService.getDistinctSitesForProject('1') >> [activity]
        response.status == HttpStatus.SC_OK
    }

    void "Get sites for project - without id"() {

        when:
        params.id = null
        controller.getDistinctSitesForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "search" () {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()
        Map props = [
                startDate:'2020-07-01T14:00:00Z', endDate:'2020-08-01T14:00:00Z', dateProperty:'2020-08-01T14:00:00Z'
        ]

        when:
        request.json = props
        controller.search()

        then:
        1 * activityService.search([:], commonService.parse('2020-07-01T14:00:00Z'), commonService.parse('2020-08-01T14:00:00Z'), '2020-08-01T14:00:00Z', 'all', [:]) >> [activity]
        response.status == HttpStatus.SC_OK
        response.getJson().activities.size() == 1
    }

    void "list" () {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        params.userId = '1'
        params.projectId = '1'
        controller.list()

        then:
        1 * elasticSearchService.buildProjectActivityQuery(params)
        1 * elasticSearchService.search(params.query, params, PROJECT_ACTIVITY_INDEX) >> GroovyMock(SearchResponse)
        response.status == HttpStatus.SC_OK
    }

    void "Get activity"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        params.view = 'flat'
        controller.getActivity()

        then:
        1 * activityService.get('1', 'flat') >> activity
        response.status == HttpStatus.SC_OK
        Activity responseData = response.json as Activity
        responseData.activityId == '1'
    }

    void "Get activity - error"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '2'
        params.view = 'test'
        controller.getActivity()

        then:
        response.status == HttpStatus.SC_FORBIDDEN
        response.text == 'Invalid view parameter. (Accepted value: all, flat, site)'
    }

    void "Get Default Facets"() {
        setup:
        Activity activity = new Activity(activityId:'1', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date()).save()

        when:
        params.id = '1'
        params.view = 'flat'
        controller.getDefaultFacets()

        then:
        response.status == HttpStatus.SC_OK
    }

}
