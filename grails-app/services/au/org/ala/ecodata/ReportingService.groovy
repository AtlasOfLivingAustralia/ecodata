package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.AggregationResult
import au.org.ala.ecodata.reporting.AggregatorFactory
import au.org.ala.ecodata.reporting.AggregatorIf
import grails.transaction.Transactional
import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.*

/**
 * This service works with the Report domain object.  Need to fix this up!.
 */
@Transactional
class ReportingService {

    def permissionService, userService, activityService, commonService

    AggregatorFactory aggregatorFactory = new AggregatorFactory()

    private String INVALID_STATUS_FOR_UPDATE_ERROR_KEY = 'report.cannotUpdateSubmittedOrApprovedReport'

    def get(String reportId, includeDeleted = false) {

        Report report = null
        if (includeDeleted) {
            report = Report.findByReportId(reportId)
        }
        else {
            report = Report.findByReportIdAndStatusNotEqual(reportId, DELETED)
        }

        populateActivityInformation([report])
        report
    }

    Map toMap(Report report, levelOfDetail = []) {
        def dbo = report.dbo
        def mapOfProperties = dbo.toMap()
        mapOfProperties.findAll {k,v -> v != null}
    }

    List findAllForProject(String projectId) {
        List projectReports = Report.findAllByProjectIdAndStatusNotEqual(projectId, DELETED)
        populateActivityInformation(projectReports)
        projectReports
    }

    def findAllForUser(String userId) {
        // Join on project & organisation ids.
        List permissions = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqual(userId, Project.class.name, AccessLevel.starred)

        def projectReports = Report.findAllByProjectIdInListAndStatusNotEqual(permissions.collect{it.entityId}, DELETED)
        populateActivityInformation(projectReports)

        permissions = UserPermission.findAllByUserIdAndEntityType(userId, Organisation.class)

        def organisationReports = Report.findAllByOrganisationIdInListAndStatusNotEqual(permissions.collect{it.entityId}, DELETED)

        [projectReports:projectReports, organisationReports:organisationReports]
    }

    int getActivityCountForReport(Report report) {
        Activity.countByProjectIdAndPlannedEndDateGreaterThanAndPlannedEndDateLessThanEqualsAndStatusNotEqual(report.projectId, report.fromDate, report.toDate, DELETED)
    }

    def findAllByOwner(ownerType, owner, includeDeleted = false) {
        def query = Report.createCriteria()

        def results = query {
            eq(ownerType, owner)
            if (!includeDeleted) {
                ne('status', 'deleted')
            }
            order("toDate", "asc")
        }

        populateActivityInformation(results)
    }

    /**
     * Populates the activityCount property for each report in the supplied List.
     * @param reports the reports of interest
     * @return returns the reports parameter (not a copy)
     */
    private List<Report> populateActivityInformation(List<Report> reports) {
        for (Report report : reports) {

            if (report.isSingleActivityReport() && report.activityId) {
                Activity activity = Activity.findByActivityId(report.activityId)
                report.progress = activity.progress
            }
            else if (report.isActivityReport()) {
                report.activityCount = getActivityCountForReport(report)
            }

        }
        reports
    }

    Report create(Map properties) {

        properties.reportId = Identifiers.getNew(true, '')
        Report report = new Report(reportId:properties.reportId)
        commonService.updateProperties(report, properties)

        if (!report.hasErrors() && report.activityType) {
            syncReportActivity(report)
        }
        if (!report.hasErrors()) {
            report.save(flush:true)
        }
        return report
    }

    Report update(String id, Map properties) {
        Report report = get(id)
        if (!report) {
            return null
        }

        if (report.isSubmittedOrApproved()) {
            report.errors.reject(INVALID_STATUS_FOR_UPDATE_ERROR_KEY)
        }
        else {
            commonService.updateProperties(report, properties)
            report.save(flush:true)

            if (!report.hasErrors() && report.isSingleActivityReport()) {
                syncReportActivity(report)
            }
        }

        return report
    }

    /**
     * Creates an activity to be associated with this report.
     * @param report the Report to create an activity for, assumed to be valid.
     */
    private void syncReportActivity(Report report) {

        Map activity = [plannedStartDate:report.fromDate, plannedEndDate:report.toDate, startDate: report.fromDate, endDate:report.toDate, type:report.activityType, description:report.name, projectId:report.projectId, programId:report.programId]
        Map syncResult
        if (report.activityId) {
            syncResult = activityService.update(activity, report.activityId)
        }
        else {
            syncResult = activityService.create(activity)
        }

        if (syncResult.error) {
            report.errors.reject('report.activity.creationFailed', [syncResult.error])
        }
        else {
            report.activityId = syncResult.activityId
        }
    }

    def delete(String id, boolean destroy) {
        Report report = get(id)
        if (report) {
            try {
                if (destroy) {
                    report.delete()
                } else {
                    report.status = DELETED
                    report.save(flush: true, failOnError: true)
                }
                if (report.activityId) {
                    activityService.delete(report.activityId, destroy)
                }

                return [status: 'ok']

            } catch (Exception e) {
                Organisation.withSession { session -> session.clear() }
                def error = "Error deleting report ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    /**
     * If the report is completed via a single activity, this method deletes any activity output data and
     * resets the progress to planned.  Otherwise, no changes are made.
     * @param id the report id to reset.
     * @return the report, after the update.
     */
    Report reset(String id) {
        Report report = get(id)
        if (!report) {
            return null
        }
        if (report.isSubmittedOrApproved()) {
            report.errors.reject(INVALID_STATUS_FOR_UPDATE_ERROR_KEY)
        }
        else {
            if (report.isSingleActivityReport()) {
                activityService.update([progress:Activity.PLANNED, activityId:report.activityId], report.activityId)
                activityService.deleteActivityOutputs(report.activityId)
            }
            populateActivityInformation([report])

        }
        return report
    }

    def submit(String id, String comment = '') {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.submit(user.userId, comment)
        r.save()
        return r
    }

    def approve(String id, String comment = '') {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.approve(user.userId, comment)
        r.save()
        return r
    }

    def returnForRework(String id, String comment = '', String category = '') {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.returnForRework(user.userId, comment, category)
        r.save()
        return r
    }

    /**
     * A report adjustment can be performed to modify the results of an approved report via the creation of
     * another report that contributes to the same scores as the original report.  This is sometimes required in
     * MERIT if changes need to be made after a report has been approved and the original report is for some
     * reason unable to have the approval withdrawn and the data updated via the standard workflow.
     * This routine adds a status change to the report to indicate the adjustment and also creates a new
     * report of the supplied type to record the required adjustments.
     *
     * @param id the reportId of the report that needs to be adjusted.
     * @param comment the reason for the adjustment
     * @param adjustmentActivityType the type of activity to be associated with the adjustment report that is to be created.
     * @return the new adjustment report, or the original report if it is unable to be adjusted
     */
    Report adjust(String id, String comment = '', String adjustmentActivityType) {
        def user = userService.getCurrentUserDetails()
        Report toAdjust = get(id)
        if (!toAdjust) {
            return null
        }

        if (toAdjust.type == Report.TYPE_ADJUSTMENT || toAdjust.isAdjusted() || !toAdjust.isApproved()) {
            toAdjust.errors.reject('report.adjustment.invalid', toAdjust.name)
            return toAdjust
        }

        Report adjustmentReport = null
        if (toAdjust.type != Report.TYPE_ADJUSTMENT && toAdjust.dateAdjusted == null) {

            Map adjustmentReportProps = [
                    name            : "Adjustment: " + toAdjust.name,
                    description     : "Adjustment: " + toAdjust.description,
                    fromDate        : toAdjust.fromDate,
                    toDate          : toAdjust.toDate,
                    type            : Report.TYPE_ADJUSTMENT,
                    adjustedReportId: toAdjust.reportId,
                    category        : "Adjustments",
                    activityType    : adjustmentActivityType,
                    projectId       : toAdjust.projectId,
                    programId       : toAdjust.programId,
                    organisationId  : toAdjust.organisationId,
                    submissionDate  : toAdjust.submissionDate
            ]
            adjustmentReport = create(adjustmentReportProps)

            if (!adjustmentReport.hasErrors()) {

                toAdjust.adjust(user.userId, comment)
                toAdjust.save()
            }
        }

        return adjustmentReport
    }

    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a list of the projects that match the supplied criteria
     */
    public search(Map searchCriteria, levelOfDetail = []) {

        def startDate = null, endDate = null, planned = null
        def dateProperty = searchCriteria.remove('dateProperty')
        if (dateProperty && searchCriteria.startDate) {
            def startDateStr = searchCriteria.remove('startDate')
            startDate = commonService.parse(startDateStr)
        }
        if (dateProperty && searchCriteria.endDate) {
            def endDateStr = searchCriteria.remove('endDate')
            endDate = commonService.parse(endDateStr)
        }

        def criteria = Report.createCriteria()
        def reports = criteria.list {
            ne("status", "deleted")
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }

            if (dateProperty && startDate) {
                ge(dateProperty, startDate)
            }
            if (dateProperty && endDate) {
                lt(dateProperty, endDate)
            }
        }
        reports
    }

    public AggregationResult aggregateReports(Map searchCriteria, Map reportConfig) {

        AggregatorIf aggregatorIf = aggregatorFactory.createAggregator(reportConfig)
        search(searchCriteria).each {
            aggregatorIf.aggregate(toMap(it))
        }
        aggregatorIf.result()
    }

}
