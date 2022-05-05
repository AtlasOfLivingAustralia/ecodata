package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class ReportingControllerSpec extends Specification implements ControllerUnitTest<ReportController>, DomainUnitTest<Report> {
    ReportingService reportingService

    def setup(){
        reportingService = Mock(ReportingService)
        controller.reportingService = reportingService
    }

    def "can submit report"(){
        setup:
        def reportId = "1"
        def params = [comment: null]
        def report = [reportId: reportId, managementUnit: "test_mu", name: "My Report", description: "Report Description", type: "Administrative", category: "Core Service", activityId: "12",
                        activityType: "RLP", fromDate: new Date(), toDate: new Date(), dueDate: new Date(), submissionDate: new Date(), dateSubmitted: new Date(), progress: "finish", submittedBy: "1",
                        publicationStatus: "pendingApproval", status: "active", dateCreated: new Date(), lastUpdated:  new Date()]

        when:
        request.method = "POST"
        controller.submit(reportId)
        def results = response.getJson()

        then:
        1* reportingService.submit(reportId, params.comment) >> report

        and:
        results.size() == 19
        results.reportId == report.reportId
        results.name == report.name
        results.type == report.type
        results.progress == report.progress
        results.publicationStatus == report.publicationStatus
    }

    def "can approve report"(){
        setup:
        def reportId = "1"
        def params = [comment: null]
        def report = [reportId: reportId, managementUnit: "test_mu", name: "My Report", description: "Report Description", type: "Administrative", category: "Core Service", activityId: "12",
                      activityType: "RLP", fromDate: new Date(), toDate: new Date(), dueDate: new Date(), submissionDate: new Date(), dateSubmitted: new Date(), progress: "finish", submittedBy: "1",
                      publicationStatus: "approved", status: "active", dateCreated: new Date(), lastUpdated:  new Date(), dateApproved: new Date(), approvedBy: "1", dateReturned: new Date()]

        when:
        request.method = "POST"
        controller.approve(reportId)
        def results = response.getJson()

        then:
        1* reportingService.approve(reportId, params.comment) >> report

        and:
        results.size() == 22
        results.reportId == report.reportId
        results.name == report.name
        results.type == report.type
        results.progress == report.progress
        results.publicationStatus == report.publicationStatus
    }

    def "can return for rework report"(){
        setup:
        def reportId = "1"
        def params = [comment: null, category: null]
        def report = [reportId: reportId, managementUnit: "test_mu", name: "My Report", description: "Report Description", type: Report.REPORT_APPROVED, category: "Core Service", activityId: "12",
                      activityType: "RLP", fromDate: new Date(), toDate: new Date(), dueDate: new Date(), submissionDate: new Date(), dateSubmitted: new Date(), progress: "finish", submittedBy: "1",
                      publicationStatus: "approved", status: "active", dateCreated: new Date(), lastUpdated:  new Date(), dateApproved: new Date(), approvedBy: "1", dateReturned: new Date()]

        when:
        request.method = "POST"
        controller.returnForRework(reportId)
        def results = response.getJson()

        then:
        1* reportingService.returnForRework(reportId, params.comment, params.category) >> report

        and:
        results.size() == 22
        results.reportId == report.reportId
        results.name == report.name
        results.type == report.type
        results.progress == report.progress
    }

    def "can cancel outcomes report"(){
        setup:
        def reportId = "1"
        def params = [comment: null, category: null]
        def report = [reportId: reportId, managementUnit: "test_mu", name: "My Report", description: "Report Description", type: Report.REPORT_NOT_APPROVED, category: "Core Service", activityId: "12",
                      activityType: "RLP", fromDate: new Date(), toDate: new Date(), dueDate: new Date(), submissionDate: new Date(), dateSubmitted: new Date(), progress: "planned", submittedBy: "1",
                      publicationStatus: "approved", status: "active", dateCreated: new Date(), lastUpdated:  new Date(), cancelledBy: "1", dateCancelled: new Date()]

        when:
        request.method = "POST"
        controller.cancel(reportId)
        def results = response.getJson()

        then:
        1* reportingService.cancel(reportId, params.comment, params.category) >> report

        and:
        results.size() == 21
        results.reportId == report.reportId
        results.name == report.name
        results.type == report.type
        results.progress == report.progress
    }

//    def "can adjust report"(){
//        setup:
//        def reportId = "1"
//        def params = [comment: null, category: null, adjustmentActivityType: null]
//        def report = [reportId: reportId, managementUnit: "test_mu", name: "My Report", description: "Report Description", type: Report.TYPE_ADJUSTMENT, category: "Core Service", activityId: "12",
//                      activityType: "RLP", fromDate: new Date(), toDate: new Date(), dueDate: new Date(), submissionDate: new Date(), dateSubmitted: new Date(), progress: "finish", submittedBy: "1",
//                      publicationStatus: "pendingApproval", status: "active", dateCreated: new Date(), lastUpdated:  new Date(), adjustedBy: "1", dateAdjusted: new Date()]
//
//        when:
//        request.method = "POST"
//        controller.adjust(reportId)
//        def results = response.getJson()
//
//        then:
//        1* reportingService.adjust(reportId, params.comment, params.category, params.adjustmentActivityType) >> report
//
//        and:
//        results.size() == 22
//        results.reportId == report.reportId
//        results.name == report.name
//        results.type == report.type
//        results.progress == report.progress
//        results.ajustedBy == report.adjustedBy
//    }




}
