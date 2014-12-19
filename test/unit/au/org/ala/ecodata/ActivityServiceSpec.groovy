package au.org.ala.ecodata

import grails.test.mixin.*
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler
import spock.lang.Specification
import spock.lang.Unroll

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Specification for the ActivityService
 * N.B. The unit tests won't work without the MongoDBTestMixin due to explicit use of the Mongo API.
 * It also requires a running mongo instance and will use the ecodata-test database by default - all
 * activities in that database will be dropped by these tests. (Having trouble getting fongo to work with
 * the service due to transactions being cleaned up after the first test execution - need to figure that out
 * at some point).
 */
@TestMixin(MongoDbTestMixin)
@Domain(Activity)
class ActivityServiceSpec extends Specification {

    ActivityService service

    /** Insert some activities into the database to work with */
    def setup() {

        Activity.withNewTransaction {
            Activity.findAll().each {
                it.delete()
            }
        }

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

        service = enhanceService(ActivityService)
    }

    private def enhanceService(serviceClass) {

        final serviceArtefact = grailsApplication.addArtefact(ServiceArtefactHandler.TYPE, serviceClass)

        defineBeans(true) {
            "${serviceArtefact.propertyName}"(serviceClass) { bean ->
                bean.autowire = true
            }
        }

        applicationContext.getBean(serviceArtefact.propertyName, serviceClass)
    }


    private def createActivity(props) {
        Activity activity = new Activity(props)
        activity.save(failOnError: true, flush:true)
    }

    @Unroll
    def "activities can be searched for without supplying dates"(criteria, expectedActivityIds) {

        when:
        def results
        Activity.withNewTransaction {
            results = service.search(criteria, LevelOfDetail.NO_OUTPUTS.name())
        }
        results.sort { a1, a2 -> a1.activityId <=> a2.activityId }

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

}
