package au.org.ala.ecodata

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import grails.validation.ValidationException
import grails.web.databinding.DataBinder
import org.bson.conversions.Bson

import static au.org.ala.ecodata.Status.DELETED

/**
 * Works with Organisations, mostly CRUD operations at this point.
 */
class OrganisationService implements DataBinder {

    /** Use to include related projects in the toMap method */
    public static final String PROJECTS = 'projects'

    private static final List EXCLUDE_FROM_BINDING = ['organisationId', 'collectoryInstitutionId', 'status', 'id']

    static transactional = 'mongo'
    static final FLAT = 'flat'

    def commonService, projectService, userService, permissionService, documentService, collectoryService, messageSource, emailService, grailsApplication
    ReportingService reportingService
    ActivityService activityService
    ReportService reportService

    def get(String id, levelOfDetail = [], includeDeleted = false) {
        Organisation organisation
        if (includeDeleted) {
            organisation = Organisation.findByOrganisationId(id)
        }
        else {
            organisation = Organisation.findByOrganisationIdAndStatusNotEqual(id, 'deleted')
        }
        return organisation ? toMap(organisation, levelOfDetail):null
    }

    def findByName(String name) {
        Organisation organisation =  Organisation.findByNameAndStatusNotEqual(name, 'deleted')
        organisation ? toMap(organisation) : [:]
    }

    def list(levelOfDetail = []) {
        return Organisation.findAllByStatusNotEqual(DELETED).collect{toMap(it, levelOfDetail)}
    }

    def create(Map props, boolean createInCollectory = false) {

        def organisation = new Organisation(organisationId: Identifiers.getNew(true, ''), name:props.name)

        if (createInCollectory) {
            organisation.collectoryInstitutionId = createCollectoryInstitution(props)
        }
        try {
            bindData(organisation, props, [exclude:EXCLUDE_FROM_BINDING])
            organisation.save(failOnError: true, flush:true)

            // Assign the creating user as an admin.
            permissionService.addUserAsRoleToOrganisation(userService.getCurrentUserDetails()?.userId, AccessLevel.admin, organisation.organisationId)

            [status:'ok',organisationId:organisation.organisationId]
        }
        catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Organisation.withSession { session -> session.clear() }
            def error = "Error creating organisation - ${e.message}"
            log.error error, e

            def errors = (e instanceof ValidationException)?e.errors:[error]
            return [status:'error',errors:errors]
        }
    }

    private String createCollectoryInstitution(Map organisationProperties) {

        String institutionId = null
        if (Boolean.valueOf(grailsApplication.config.getProperty('collectory.collectoryIntegrationEnabled'))) {
            try {
                institutionId = collectoryService.createInstitution(organisationProperties)
            }
            catch (Exception e) {
                // We don't want this to prevent the organisation from being created.
                String message = "Failed to establish collectory link for organisation ${organisationProperties.name}"
                log.error(message, e)
                emailService.sendEmail(message, "Error: ${e.message}", [grailsApplication.config.getProperty('ecodata.support.email.address')])
            }
        }
        return institutionId
    }

    def update(String id, props, boolean createInCollectory = false) {

        def organisation = Organisation.findByOrganisationId(id)
        if (organisation) {

            try {
                // if no collectory institution exists for this organisation, create one
                // We shouldn't be doing this unless the org is attached to a project that exports data
                // to the ALA.
                if (createInCollectory && (!organisation.collectoryInstitutionId ||  organisation.collectoryInstitutionId == 'null' || organisation.collectoryInstitutionId == '')) {
                    organisation.collectoryInstitutionId = createCollectoryInstitution(props)
                }
œ
                String oldName = organisation.name
                List contractNameChanges = props.remove('contractNameChanges')
                bindData(organisation, props, [exclude:EXCLUDE_FROM_BINDING])

                if (props.name && (oldName != props.name)) {
                    projectService.updateOrganisationName(organisation.organisationId, oldName, props.name)
                }
                contractNameChanges?.each { Map change ->
                    projectService.updateOrganisationName(organisation.organisationId, change.oldName, change.newName)
                }
                organisation.save(failOnError:true)
                return [status:'ok']
            } catch (Exception e) {
                Organisation.withSession { session -> session.clear() }
                def error = "Error updating organisation ${id} - ${e.message}"
                log.error error, e
                if (e instanceof ValidationException) {
                    error = messageSource.getMessage(e.errors.fieldError, Locale.getDefault())
                }
                return [status:'error',errors:error]
            }
        } else {
            def error = "Error updating organisation - no such id ${id}"
            log.error error
            return [status:'error',errors:error]
        }
    }

    def delete(String id, boolean destroy) {
        def organisation = Organisation.findByOrganisationId(id)
        if (organisation) {

            // Delete any user associations or permissions associated with the organisation - how exactly will permissions work?
            //permissionService.deleteAllForOrganisation(id)

            try {
                if (destroy) {
                    organisation.delete()
                } else {
                    organisation.status = DELETED
                    organisation.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                Organisation.withSession { session -> session.clear() }
                def error = "Error deleting organisation ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    def toMap(Organisation org, levelOfDetail = []) {
        def mapOfProperties = GormMongoUtil.extractDboProperties(org.getProperty('dbo'))
       // def mapOfProperties = dbo.toMap()

        if ('projects' in levelOfDetail) {
            mapOfProperties.projects = []
            mapOfProperties.projects += projectService.search([organisationId: org.organisationId], ['flat'])
        }
        if ('documents' in levelOfDetail) {
            mapOfProperties.documents = documentService.findAllByOwner('organisationId', org.organisationId)
            mapOfProperties.links = documentService.findAllLinksByOwner('organisationId', org.organisationId)

        }

        mapOfProperties.findAll {k,v -> v != null}
    }

    /**
     * Accepts a closure that will be called once for each (not deleted) Organisation in the system,
     * passing the site (as a Map) as the single parameter.
     * Implementation note, this uses the Mongo API directly as using GORM incurs a
     * significant memory and performance overhead when dealing with so many entities
     * at once.
     * @param action the action to be performed on each Organisation.
     * @param filters list of filters
     */
    void doWithAllOrganisations(Closure action, List<Bson> filters = [], int batchSize = 100) {
        // Due to various memory & performance issues with GORM mongo plugin 1.3, this method uses the native API.
        MongoCollection collection = Organisation.getCollection()
        //DBObject siteQuery = new QueryBuilder().start('status').notEquals(DELETED).get()
        Bson query = Filters.ne("status", DELETED)
        filters.add(query)
        query = Filters.and(filters)
        def results = collection.find(query).batchSize(batchSize)

        results.each { dbObject ->
            action.call(dbObject)
        }
    }

    /**
     *  Get reports of all management units in a period
     *
     * @param start
     * @param end
     * @param hubId
     * @return
     */
    List<Map> getReportingActivities (String startDate, String endDate, String hubId) {
        List<Map> organisationDetails = []
        doWithAllOrganisations({ org ->
            List<Report> reports = reportingService.search([organisationId: org.organisationId, dateProperty:'toDate', startDate:startDate, endDate:endDate])

             List<Map> activities = activityService.search([activityId:reports.activityId], ['all'])

            Map result = new HashMap(org)
            result.reports = reports
            result.activities = activities

            organisationDetails << result

        }, [Filters.eq("hubId", hubId)])

        organisationDetails
    }

    /**
     * Returns the reportable metrics for a organisation as determined by the organisation output targets and activities
     * that have been undertaken.
     * @param id identifies the organisation.
     * @return a Map containing the aggregated results.
     *
     */
    def organisationMetrics(String id, approvedOnly = false, List scoreIds = null, Map aggregationConfig = null) {
        def org = Organisation.findByOrganisationId(id)
        if (org) {
            List toAggregate = Score.findAllByScoreIdInList(scoreIds)
            List outputSummary = reportService.organisationSummary(id, toAggregate, approvedOnly, aggregationConfig) ?: []

            return outputSummary
        } else {
            def error = "Error retrieving metrics for project - no such id ${id}"
            log.error error
            return [status: 'error', error: error]
        }
    }

}
