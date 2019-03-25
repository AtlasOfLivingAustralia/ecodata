package au.org.ala.ecodata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.h2.engine.User
import org.springframework.context.MessageSource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import spock.lang.Specification

@TestMixin(MongoDbTestMixin)
@Domain([Report, Activity])
@TestFor(ReportingService)
class ReportingServiceSpec extends Specification {

    CommonService commonService = new CommonService()
    ActivityService activityService = Mock(ActivityService)
    UserService userService = Mock(UserService)

    def setup() {
        commonService.grailsApplication = grailsApplication
        commonService.messageSource = Mock(MessageSource)
        service.commonService = commonService
        service.activityService = activityService
        service.userService = userService

        service.transactionManager = Mock(PlatformTransactionManager) {
            getTransaction(_) >> Mock(TransactionStatus)
        }

        userService.getCurrentUserDetails() >> ['userId':'1234']

        Report.findAll().each { it.delete(flush:true) }
    }

    def cleanup() {
        Report.findAll().each { it.delete(flush:true) }
        Activity.findAll().each {it.delete(flush:true) }
    }

    void "Resetting a single activity report should delete associated output data"() {
        setup:
        String reportId = '1'
        Date now = new Date()

        Activity activity = new Activity(activityId:'activity1', name:"Activity", description:"activity 1")
        activity.save(flush:true, failOnError:true)

        Report report = new Report(reportId: reportId, name: 'test 1', description: 'description 1', activityType: "Activity", activityId:'activity1', type: Report.TYPE_ACTIVITY, fromDate: now, toDate: now)
        report.save(flush: true, failOnError: true)

        when:
        Report result = service.reset(reportId)

        then:
        1 * activityService.deleteActivityOutputs(report.activityId)
        1 * activityService.update([progress:Activity.PLANNED, activityId:report.activityId], report.activityId)
    }

    void "Resetting a report should fail if the report is submitted or approved"() {
        setup:
        String reportId = '1'
        Date now = new Date()

        Report report = new Report(reportId: reportId, name: 'test 1', description: 'description 1', activityType: "Activity", type: Report.TYPE_ACTIVITY, fromDate: now, toDate: now)
        report.submit('1234')
        report.save(flush: true, failOnError: true)

        when:
        Report result = service.reset(reportId)

        then:
        result.hasErrors() == true

        when:
        report.approve('1234')
        result = service.reset(reportId)

        then:
        result.hasErrors() == true

    }

    void "A submitted or approved report should not be able to be updated"() {
        setup:
        String reportId = '1'
        Date now = new Date()

        Report report = new Report(reportId: reportId, name: 'test 1', description: 'description 1', activityType: "Activity", type: Report.TYPE_ACTIVITY, fromDate: now, toDate: now)
        report.submit('1234')
        report.save(flush: true, failOnError: true)

        when:
        Report result = service.update(reportId, [description:"new description"])

        then:
        result.hasErrors() == true
        report.description == 'description 1'

        when:
        report.approve('1234')
        result = service.update(reportId, [description:"new description"])

        then:
        result.hasErrors() == true
        report.description == 'description 1'

    }

    void "A report of type adjustment cannot be adjusted"() {
        setup:
        String reportId = '1'
        Date now = new Date()
        Activity activity = new Activity(activityId:'a1', name:"Activity", description:"activity 1")
        activity.save(flush:true, failOnError:true)
        Report report = new Report(reportId: reportId, adjustedReportId: '2', name: 'test 1', description: 'description 1', activityType: "Activity", type: Report.TYPE_ADJUSTMENT, fromDate: now, toDate: now, activityId:activity.activityId)
        report.save(flush:true, failOnError: true)

        when:
        Report adjustmentReport = service.adjust(reportId, "", "test")

        then:
        adjustmentReport.hasErrors() == true
        adjustmentReport.statusChangeHistory.find{it.status == 'adjusted'} == null
    }

    void "A report that is already adjusted cannot be adjusted again"() {
        setup:
        String reportId = '1'
        Date now = new Date()
        Activity activity = new Activity(activityId:'a1', name:"Activity", description:"activity 1")
        activity.save(flush:true, failOnError:true)
        Report report = new Report(reportId: reportId, name: 'test 1', description: 'description 1', activityType: "Activity", type: Report.TYPE_ACTIVITY, fromDate: now, toDate: now, activityId:activity.activityId, dateAdjusted: now, adjustedBy:'1234')
        report.save(flush:true, failOnError: true)

        when:
        Report adjustmentReport = service.adjust(reportId, "", "test")

        then:
        adjustmentReport.hasErrors() == true

    }

    void "Adjusting a report will create a new report of type Adjustment and update the status history of the original report"() {
        setup:
        String reportId = '1'
        Date now = new Date()
        Activity activity = new Activity(activityId:'a1', name:"Activity", description:"activity 1")
        activity.save(flush:true, failOnError:true)
        Report report = new Report(reportId: reportId, name: 'test 1', description: 'description 1', publicationStatus:'published', activityType: "Activity", type: Report.TYPE_ACTIVITY, fromDate: now, toDate: now, activityId:activity.activityId)
        report.save(flush:true, failOnError: true)
        Report adjustmentReport = null
        Report original = null

        when:
        Report.withSession { session ->
            adjustmentReport = service.adjust(reportId, "", "test")
            session.flush()
            original = Report.findByReportId(report.reportId)
        }

        then:
        adjustmentReport.hasErrors() == false
        adjustmentReport.type == Report.TYPE_ADJUSTMENT
        adjustmentReport.adjustedReportId == reportId
        adjustmentReport.activityType == "test"
        1 * activityService.create(_) >> [:]
        original.isAdjusted() == true
        original.adjustedBy == "1234"
        original.dateAdjusted != null
    }

}
