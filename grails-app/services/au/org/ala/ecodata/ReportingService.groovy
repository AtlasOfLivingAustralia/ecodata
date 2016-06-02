package au.org.ala.ecodata

import grails.transaction.Transactional
import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.*

/**
 * This service works with the Report domain object.  Need to fix this up!.
 */
@Transactional
class ReportingService {

    def permissionService, userService, activityService, commonService

    def get(String reportId, includeDeleted = false) {
        if (includeDeleted) {
            return Report.findByReportId(reportId)
        }
        Report report = Report.findByReportIdAndStatusNotEqual(reportId, 'deleted')
        report.activityCount = getActivityCountForReport(report)
        report
    }

    def findAllForProject(String projectId) {
        Report.findAllByProjectId(projectId)
    }

    def findAllForUser(String userId) {
        // Join on project & organisation ids.
        List permissions = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqual(userId, Project.class.name, AccessLevel.starred)

        def projectReports = Report.findAllByProjectIdInList(permissions.collect{it.entityId})
        populateActivityCounts(projectReports)

        permissions = UserPermission.findAllByUserIdAndEntityType(userId, Organisation.class)

        def organisationReports = Report.findAllByOrganisationIdInList(permissions.collect{it.entityId})

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

        populateActivityCounts(results)
    }

    /**
     * Populates the activityCount property for each report in the supplied List.
     * @param reports the reports of interest
     * @return returns the reports parameter (not a copy)
     */
    private List<Report> populateActivityCounts(List<Report> reports) {
        for (Report report : reports) {
            report.activityCount = getActivityCountForReport(report)
        }
        reports
    }

    Report create(Map properties) {

        properties.reportId = Identifiers.getNew(true, '')
        Report report = new Report(reportId:properties.reportId)
        commonService.updateProperties(report, properties)
        report.save(flush:true)
        return report
    }

    Report update(String id, Map properties) {
        Report report = get(id)
        commonService.updateProperties(report, properties)
        report.save(flush:true)
        return report
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

    def returnForRework(String id, String comment = '') {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.returnForRework(user.userId, comment)
        r.save()
        return r
    }

    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a list of the projects that match the supplied criteria
     */
    public search(Map searchCriteria, levelOfDetail = []) {

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

        }
        reports
    }


}
