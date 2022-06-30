package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec

import grails.testing.services.ServiceUnitTest
import grails.core.GrailsApplication
import spock.lang.Specification
import grails.testing.gorm.DomainUnitTest
import spock.lang.Unroll

import java.text.DateFormat
import java.text.SimpleDateFormat

import static au.org.ala.ecodata.Status.ACTIVE

/**
 * Specification for the ActivityService
 * N.B. The unit tests won't work without the MongoDBTestMixin due to explicit use of the Mongo API.
 * It also requires a running mongo instance and will use the ecodata-test database by default - all
 * activities in that database will be dropped by these tests. (Having trouble getting fongo to work with
 * the service due to transactions being cleaned up after the first test execution - need to figure that out
 * at some point).
 */
class ActivityServiceSpec extends MongoSpec implements ServiceUnitTest<ActivityService>, DomainUnitTest<Activity> {

    OutputService outputService = Mock(OutputService)
    UserService userService = Mock(UserService)
    DocumentService documentService = Mock(DocumentService)
    CommentService commentService = Mock(CommentService)

    /** Insert some activities into the database to work with */
    def setup() {

        DateFormat format = new SimpleDateFormat('yyyy/MM/dd')
        def types = ['type 1', 'type 1', 'type 2', 'type 3', 'type 3', 'type 3']
        def plannedStartDates = ['2014/01/01', '2014/07/01', '2014/01/01', '2014/07/01', '2015/01/01', '2015/07/01']
        def plannedEndDates = ['2014/07/01', '2015/01/01', '2014/07/01', '2015/01/01', '2015/07/01', '2015/12/31']
        def startDates = ['2014/02/01', '2014/06/01', '2014/03/01', '2014/08/01', '2015/02/01', '2015/09/13']
        def endDates = ['2014/06/30', '2015/01/01', '2014/04/01', '2015/01/01', '2015/06/01', '2015/09/27']

        for (int i=0; i<types.size(); i++) {
            def props = [activityId: 'activity' + i, type: types[i], description: 'description ' + i,
                         plannedStartDate:format.parse(plannedStartDates[i]),
                         plannedEndDate:format.parse(plannedEndDates[i]),
                         startDate:format.parse(startDates[i]),
                         endDate:format.parse(endDates[i])]
            createActivity(props)
        }
        Activity.metaClass.getDbo = {
            delegate.properties
        }

        service.lockService = Stub(LockService)
        service.outputService = outputService
        service.userService = userService
        service.documentService = documentService
        service.commentService = commentService

        CommonService commonService = new CommonService()
        commonService.grailsApplication = Stub(GrailsApplication)
        service.commonService = commonService
    }

    void cleanup() {
        Activity.withNewTransaction {
            Activity.findAll().each {
                it.delete()
            }

            Activity.metaClass.getDbo = null
        }

        ProjectActivity.withNewTransaction {
            ProjectActivity.findAll().each {
                it.delete()
            }
        }
    }

    private def createActivity(props) {
        Activity.withNewTransaction {
            Activity activity = new Activity(props)
            activity.save(failOnError: true, flush: true)
        }
    }


    @Unroll
    def "activities can be searched for without supplying dates"(criteria, expectedActivityIds) {

        when:
        def results
        Activity.withNewSession {
            results = service.search(criteria, LevelOfDetail.NO_OUTPUTS.name())
            results.sort { a1, a2 -> a1.activityId <=> a2.activityId }
        }


        then:
        results.collect {it.activityId} == expectedActivityIds

        where:
        criteria                                       | expectedActivityIds
        [type: 'type 1']                               | ['activity0', 'activity1']
        [description: 'description 3']                 | ['activity3']
        [type: 'type 1', description: 'description 1'] | ['activity1']
        [type: 'type 2', description: 'description 1'] | []


    }



    @Unroll
    def "activities can be searched by start date #startDate"(startDate, planned, criteria, expectedActivityIds) {
        when:
        def results
        def dateProperty = planned?'plannedStartDate':'startDate'
        Activity.withNewTransaction { // There is probably a neater way to do this but the MongoDbTestMixin doesn't work with the ServiceUnitTestMixin

            DateFormat df = new SimpleDateFormat('yyyy/MM/dd')
            results = service.search(criteria, df.parse(startDate), null, dateProperty, LevelOfDetail.NO_OUTPUTS.name())
            results.sort { a1, a2 -> a1.activityId <=> a2.activityId }
        }
        then:
        results.collect {it.activityId} == expectedActivityIds

        where:
        startDate    | planned       | criteria        | expectedActivityIds
        '2014/01/01' | Boolean.TRUE  | [:]             | ['activity0', 'activity1', 'activity2', 'activity3', 'activity4', 'activity5']
        '2014/01/02' | Boolean.TRUE  | [:]             | ['activity1', 'activity3', 'activity4', 'activity5']
        '2016/01/02' | Boolean.TRUE  | [:]             | []
        '2014/01/02' | Boolean.FALSE | [:]             | ['activity0', 'activity1', 'activity2', 'activity3', 'activity4', 'activity5']
        '2014/01/02' | Boolean.FALSE | [type:'type 1'] | ['activity0', 'activity1']


    }



    def "activities can be searched by end date"(endDate, planned, criteria, expectedActivityIds) {
        when:
        def results
        def dateProperty = planned?'plannedEndDate':'endDate'
        Activity.withNewTransaction { // There is probably a neater way to do this but the MongoDbTestMixin doesn't work with the ServiceUnitTestMixin

            DateFormat df = new SimpleDateFormat('yyyy/MM/dd')
            results = service.search(criteria, null, df.parse(endDate), dateProperty, LevelOfDetail.NO_OUTPUTS.name())
            results.sort { a1, a2 -> a1.activityId <=> a2.activityId }
        }
        then:
        results.collect {it.activityId} == expectedActivityIds

        where:
        endDate      | planned       | criteria        | expectedActivityIds
        '2014/01/02' | Boolean.TRUE  | [:]             | []
        '2016/01/02' | Boolean.TRUE  | [:]             | ['activity0', 'activity1', 'activity2', 'activity3', 'activity4', 'activity5']
        '2015/01/02' | Boolean.FALSE | [:]             | ['activity0', 'activity1', 'activity2', 'activity3']
        '2015/01/01' | Boolean.FALSE | [type:'type 1'] | ['activity0'] // End dates are exclusive
    }

    def "activities can be searched by start and end date"(startDate, endDate, planned, criteria, expectedActivityIds) {


        when:
        def results
        def dateProperty = planned?'plannedEndDate':'endDate'
        Activity.withNewTransaction {
            // There is probably a neater way to do this but the MongoDbTestMixin doesn't work with the ServiceUnitTestMixin

            DateFormat df = new SimpleDateFormat('yyyy/MM/dd')
            results = service.search(criteria, null, df.parse(endDate), dateProperty, LevelOfDetail.NO_OUTPUTS.name())
            results.sort { a1, a2 -> a1.activityId <=> a2.activityId }
        }
        then:
        results.collect { it.activityId } == expectedActivityIds

        where:
        startDate    | endDate | planned | criteria | expectedActivityIds
        '2014/01/01' | '2014/01/02' | Boolean.TRUE  | [:] | []
        '2014/01/01' | '2016/01/02' | Boolean.TRUE  | [:] | ['activity0', 'activity1', 'activity2', 'activity3', 'activity4', 'activity5']

    }

    def "when an activity is cancelled or deferred, existing Output data should be deleted"(String progressToAssign, boolean shouldBeDeleted) {
        setup:
        String id = 'activity1'
        createActivity([activityId:id, type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date()])

        when:
        service.update([activityId: id, progress:progressToAssign], id)

        then:
        if (shouldBeDeleted) {
            1 * outputService.getAllOutputIdsForActivity(id) >> ['output1']
            1 * outputService.delete('output1', false)
        }
        else {
            0 * outputService._
        }

        where:
        progressToAssign   | shouldBeDeleted
        Activity.PLANNED   | false
        Activity.STARTED   | false
        Activity.FINISHED  | false
        Activity.DEFERRED  | true
        Activity.CANCELLED | true
    }

    def "when an activity type is changed, existing Output data should be deleted"() {
        setup:
        String id = 'activity1'
        createActivity([activityId:id, type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date()])

        when:
        service.update([activityId: id, type:'Type 2'], id)

        then:
        1 * outputService.getAllOutputIdsForActivity(id) >> ['output1']
        1 * outputService.delete('output1', false)
    }

    def "Get activity by Id"() {
        setup:
        String id = 'activity1'

        when:
        Activity activity = service.get(id, ActivityService.FLAT)

        then:
        activity.activityId == id
    }

    def "Get activity by Id and version"() {
        setup:
        String id = 'activity1'

        when:
        def response = service.get(id, ActivityService.FLAT, 1)

        then:
        response.status == 404
        response.error == 'Activity cannot be found'
    }

    def "Get activity by Id by hiding Member Only Fields"() {
        setup:
        String id = 'activity1'

        when:
        Activity activityRetrieved = service.get(id, ActivityService.FLAT, null, null, true)

        then:
        activityRetrieved.activityId == id
    }

    def "Get All - include deletes activities"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED])

        when:
        List activitiesRetrieved = service.getAll(true, ActivityService.FLAT)

        then:
        activitiesRetrieved.size() == 7
        activitiesRetrieved.activityId.contains('activity6')
    }

    def "Get All - without deleted activities"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED])

        when:
        List activitiesRetrieved = service.getAll(false, ActivityService.FLAT)

        then:
        activitiesRetrieved.size() == 6
        !activitiesRetrieved.activityId.contains('activity6')
    }

    def "Is user owner"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), userId: '1234'])

        when:
        boolean isUserOwner = service.isUserOwner('1234', 'activity6')

        then:
        isUserOwner
    }

    def "Is not user owner"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), userId: '6789'])

        when:
        boolean isUserOwner = service.isUserOwner('1234', 'activity6')

        then:
        !isUserOwner
    }

    def "Get All by list of Ids"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED])
        List ids = ['activity1', 'activity2', 'activity6']

        when:
        List activitiesRetrieved = service.getAll(ids, ActivityService.FLAT)

        then:
        //activities in deleted status will not be returned by this
        activitiesRetrieved.size() == 2
        !activitiesRetrieved.activityId.contains('activity6')
    }

    def "Get All for site Id"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, siteId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '1234'])

        when:
        List activitiesRetrieved = service.findAllForSiteId('1234', ActivityService.FLAT)

        then:
        //activities in deleted status will not be returned by this
        activitiesRetrieved.size() == 1
        !activitiesRetrieved.activityId.contains('activity6')
        activitiesRetrieved.activityId.contains('activity7')
    }

    def "Get All for project Id - including deleted activities"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, projectId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectId: '1234'])

        when:
        List activitiesRetrieved = service.findAllForProjectId('1234', ActivityService.FLAT, true)

        then:
        activitiesRetrieved.size() == 2
        activitiesRetrieved.activityId.contains('activity6')
        activitiesRetrieved.activityId.contains('activity7')
    }

    def "Get All for project Id - without deleted activities"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, projectId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectId: '1234'])

        when:
        List activitiesRetrieved = service.findAllForProjectId('1234', ActivityService.FLAT, false)

        then:
        activitiesRetrieved.size() == 1
        !activitiesRetrieved.activityId.contains('activity6')
        activitiesRetrieved.activityId.contains('activity7')
    }

    def "Get All for project activity Id"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, projectActivityId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])

        when:
        List activitiesRetrieved = service.findAllForProjectActivityId('1234', [:], ActivityService.FLAT)

        then:
        //activities in deleted status will not be returned by this
        activitiesRetrieved.size() == 1
        !activitiesRetrieved.activityId.contains('activity6')
        activitiesRetrieved.activityId.contains('activity7')
    }

    def "Get All for project activity Id for given activity ids"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, projectActivityId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])
        List ids = ['activity1', 'activity2', 'activity6', 'activity7']

        when:
        List activitiesRetrieved = service.findAllForActivityIdsInProjectActivity(ids, '1234', [:], ActivityService.FLAT)

        then:
        //activities in deleted status will not be returned by this
        activitiesRetrieved.size() == 1
        !activitiesRetrieved.activityId.contains('activity6')
        activitiesRetrieved.activityId.contains('activity7')
    }

    def "Get All for user id"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), userId: '1234',  status: Status.DELETED])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), userId: '1234',  status: Status.ACTIVE])

        when:
        def response = service.findAllForUserId('1234', [max: 10], ActivityService.FLAT)

        then:
        //activities in deleted status will not be returned by this
        response.total == 1
        !response.list.activityId.contains('activity6')
        response.list.activityId.contains('activity7')
    }

    def "Find by project Id - with restricted project activity ids"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, projectId: '1234', projectActivityId: '1234', userId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectId: '1234', projectActivityId: '6789', userId: '1234'])
        createActivity([activityId:'activity8', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectId: '1234', projectActivityId: '3456', userId: '2345'])
        createActivity([activityId:'activity9', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectId: '1234', projectActivityId: '3456', userId: '1234'])
        List<String> restrictedIds = ['3456']

        when:
        def response = service.listByProjectId('1234', [max: 10], restrictedIds, ActivityService.FLAT)

        then:
        1 * userService.getCurrentUserDetails() >> new UserDetails(userId:  '1234')

        response.total == 2
        //activities in deleted status will not be returned by this
        !response.list.activityId.contains('activity6')
        //activities in restricted list  and different user will not be returned by this
        !response.list.activityId.contains('activity8')
        response.list.activityId.contains('activity7')
        //even though activity is in restricted list since current user is the user for activity, the activity will be returned
        response.list.activityId.contains('activity9')
    }

    def "Count by project activity id"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, projectActivityId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])

        when:
        def response = service.countByProjectActivityId('1234')

        then:
        //activities in deleted status will not be returned by this
        response == 1
    }

    def "Get distinct sites for project activity id"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, siteId: '3456', projectActivityId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '1234', projectActivityId: '1234'])
        createActivity([activityId:'activity8', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '1234', projectActivityId: '1234'])
        createActivity([activityId:'activity9', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '', projectActivityId: '1234'])
        createActivity([activityId:'activity10', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: null, projectActivityId: '1234'])

        when:
        def response = service.getDistinctSitesForProjectActivity('1234')

        then:
        response.size() == 2
    }

    def "Get distinct sites for project id"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.DELETED, siteId: '3456', projectId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '1234', projectId: '1234'])
        createActivity([activityId:'activity8', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '1234', projectId: '1234'])
        createActivity([activityId:'activity9', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: '', projectId: '1234'])
        createActivity([activityId:'activity10', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, siteId: null, projectId: '1234'])

        when:
        def response = service.getDistinctSitesForProject('1234')

        then:
        response.size() == 1
    }

    def "Delete by project activity id - invalid project activity"() {
        setup:

        when:
        def response = service.deleteByProjectActivity('1234', true)

        then:
        response.status == 'not found'
    }

    def "Delete by project activity id - destroy false"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])
        ProjectActivity pActivity = new ProjectActivity(projectActivityId:'1234', projectId:'1234', name:"Project Activity 1", description: "d", startDate: new Date(), isDataManagementPolicyDocumented: false, dataAccessMethods: ["na"], dataQualityAssuranceMethods:
                ["dataownercurated"], "nonTaxonomicAccuracy": "low", "temporalAccuracy": "low", "speciesIdentification": "low", "spatialAccuracy": "low", methodType : "opportunistic", methodName: "Opportunistic/ad-hoc observation recording",
                dataSharingLicense: "CC BY", status: ACTIVE)
        pActivity.save(failOnError: true, flush: true)

        when:
        def response = service.deleteByProjectActivity('1234', false)

        then:
        response.status == 'ok'
    }

    def "Delete by project activity id - destroy true"() {
        setup:
        createActivity([activityId:'activity6', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])
        createActivity([activityId:'activity7', type:'Type 1', description:'Test', progress:Activity.FINISHED, plannedStartDate:new Date(), plannedEndDate: new Date(), status: Status.ACTIVE, projectActivityId: '1234'])
        ProjectActivity pActivity = new ProjectActivity(projectActivityId: '1234', projectId:'1234', name:"Project Activity 1", description: "d", startDate: new Date(), isDataManagementPolicyDocumented: false, dataAccessMethods: ["na"], dataQualityAssuranceMethods:
                ["dataownercurated"], "nonTaxonomicAccuracy": "low", "temporalAccuracy": "low", "speciesIdentification": "low", "spatialAccuracy": "low", methodType : "opportunistic", methodName: "Opportunistic/ad-hoc observation recording",
                dataSharingLicense: "CC BY", status: ACTIVE)
        pActivity.save(failOnError: true, flush: true)

        when:
        def response = service.deleteByProjectActivity('1234', true)

        then:
        response.status == 'ok'
    }
}
