package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(MongoDbTestMixin)
@Domain(Report)
class ReportSpec extends Specification {

    def setup() {
        Report.collection.remove(new BasicDBObject())
        UserService._currentUser.set(['userId':'1234'])
    }

    def cleanup() {
        Report.collection.remove(new BasicDBObject())
    }

    void "a new report can be saved if mandatory fields are supplied"() {
        when:
        Report report = new Report(reportId:'report1', name:'My report', type:'Activity', fromDate:new Date(), toDate: new Date(), dueDate: new Date())
        report.save(flush:true, failOnError:true)

        then:
        def savedReport = Report.findByReportId('report1')
        savedReport.reportId == 'report1'
        savedReport.name == 'My report'
        savedReport.fromDate != null
        savedReport.toDate != null
        savedReport.dueDate != null
    }

    void "when a report is submitted, history should be recorded"() {
        when:
        Report report = new Report(reportId:'blah', name:'My report', type:'Activity', fromDate:new Date(), toDate: new Date(), dueDate: new Date())
        report.submit('1234')
        report.save(flush:true, failOnError:true)

        then:
        def savedReport = Report.findByReportId('blah')
        savedReport.submittedBy == '1234'
        savedReport.publicationStatus == Report.REPORT_SUBMITTED
        savedReport.dateSubmitted != null
        savedReport.statusChangeHistory.size() == 1
    }

    void "when a report is approved, history should be recorded"() {
        when:
        Report report = new Report(reportId:'blah', name:'My report', type:'Activity', fromDate:new Date(), toDate: new Date(), dueDate: new Date())
        report.submit('1234')
        report.approve('1234')
        report.save(flush:true, failOnError:true)

        then:
        def savedReport = Report.findByReportId('blah')
        savedReport.approvedBy == '1234'
        savedReport.publicationStatus == Report.REPORT_APPROVED
        savedReport.dateApproved != null
        savedReport.statusChangeHistory.size() == 2
    }

    void "when a report is returned to a user, history should be recorded"() {
        when:
        Report report = new Report(reportId:'blah', name:'My report', type:'Activity', fromDate:new Date(), toDate: new Date(), dueDate: new Date())
        report.returnForRework('1234')
        report.save(flush:true, failOnError:true)

        then:
        def savedReport = Report.findByReportId('blah')
        savedReport.returnedBy == '1234'
        savedReport.publicationStatus == Report.REPORT_NOT_APPROVED
        savedReport.dateReturned != null
        savedReport.statusChangeHistory.size() == 1
    }
}
