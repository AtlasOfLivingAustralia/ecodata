package au.org.ala.ecodata

import grails.transaction.Transactional

/**
 * This service works with the Report domain object.  Need to fix this up!.
 */
@Transactional
class ReportingService {

    def permissionService, userService

    def get(String reportId, includeDeleted = false) {
        if (includeDeleted) {
            return Report.findByReportId(reportId)
        }
        Report.findByReportIdAndStatusNotEqual(reportId, 'deleted')
    }

    def findAllForProject(String projectId) {
        Report.findAllByProjectId(projectId)
    }

    def findAllForUser(String userId) {
        // Join on project & organisation ids.
        List permissions = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqual(userId, Project.class.name, AccessLevel.starred)

        def projectReports = Report.findAllByProjectIdInList(permissions.collect{it.entityId})

        permissions = UserPermission.findAllByUserIdAndEntityType(userId, Organisation.class)

        def organisationReports = Report.findAllByOrganisationIdInList(permissions.collect{it.entityId})

        [projectReports:projectReports, organisationReports:organisationReports]
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

        results
    }

    Report create(Map properties) {

        properties.reportId = Identifiers.getNew(true, '')
        Report report = new Report(properties)
        report.save(flush:true)
        return report
    }

    def submit(String id) {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.submit(user.userId)
        r.save()
    }

    def approve(String id) {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.approve(user.userId)
        r.save()
    }

    def returnForRework(String id) {
        def user = userService.getCurrentUserDetails()
        Report r = get(id)
        r.returnForRework(user.userId)
        r.save()
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
