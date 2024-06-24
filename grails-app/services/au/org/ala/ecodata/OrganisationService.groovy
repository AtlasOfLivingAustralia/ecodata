package au.org.ala.ecodata

import com.mongodb.client.model.Filters
import grails.validation.ValidationException
import org.bson.conversions.Bson

import static au.org.ala.ecodata.Status.DELETED

/**
 * Works with Organisations, mostly CRUD operations at this point.
 */
class OrganisationService {

    /** Use to include related projects in the toMap method */
    public static final String PROJECTS = 'projects'

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
        return Organisation.findAllByStatusNotEqual('deleted').collect{toMap(it, levelOfDetail)}
    }

    def create(Map props, boolean createInCollectory = true) {

        def organisation = new Organisation(organisationId: Identifiers.getNew(true, ''), name:props.name)

        if (createInCollectory) {
            organisation.collectoryInstitutionId = createCollectoryInstitution(props)
        }
        try {
            // name is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            organisation.save(failOnError: true, flush:true)
            props.remove('id')
            props.remove('organisationId')
            props.remove('collectoryInstitutionId')
            commonService.updateProperties(organisation, props)

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

    def update(String id, props) {

        def organisation = Organisation.findByOrganisationId(id)
        if (organisation) {

            try {
                String oldName = organisation.name
                commonService.updateProperties(organisation, props)
                // if no collectory institution exists for this organisation, create one
                if (!organisation.collectoryInstitutionId ||  organisation.collectoryInstitutionId == 'null' || organisation.collectoryInstitutionId == '') {
                    props.collectoryInstitutionId = createCollectoryInstitution(props)
                }

                getCommonService().updateProperties(organisation, props)
                if (props.name && (oldName != props.name)) {
                    projectService.updateOrganisationName(organisation.organisationId, props.name)
                }
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
                    organisation.status = 'deleted'
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
            mapOfProperties.projects += projectService.search([orgIdSvcProvider: org.organisationId], ['flat'])
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
    void doWithAllOrganisations(Closure action, List<Filters> filters = []) {
        // Due to various memory & performance issues with GORM mongo plugin 1.3, this method uses the native API.
        def collection = Organisation.getCollection()
        //DBObject siteQuery = new QueryBuilder().start('status').notEquals(DELETED).get()
        Bson query = Filters.ne("status", DELETED)
        filters.add(query)
        query = Filters.and(filters)
        def results = collection.find(query).batchSize(100)

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
    def organisationMetrics(String id, targetsOnly = false, approvedOnly = false, List scoreIds = null, Map aggregationConfig = null, boolean includeTargets = true) {
        def org = Organisation.findByOrganisationId(id)
        if (org) {
            def organisation = toMap(org, OrganisationService.FLAT)

            List toAggregate
            if (scoreIds && targetsOnly) {
                toAggregate = Score.findAllByScoreIdInListAndIsOutputTarget(scoreIds, true)
            } else if (scoreIds) {
                toAggregate = Score.findAllByScoreIdInList(scoreIds)
            } else {
                toAggregate = targetsOnly ? Score.findAllByIsOutputTarget(true) : Score.findAll()
            }

            List outputSummary = reportService.organisationSummary(id, toAggregate, approvedOnly, aggregationConfig) ?: []

            // Add project output target information where it exists.
            if (includeTargets) {
                organisation.outputTargets?.each { target ->
                    // Outcome targets are text only and not mapped to a score.
                    if (target.outcomeTarget != null) {
                        return
                    }
                    def result = outputSummary.find { it.scoreId == target.scoreId }
                    if (result) {
                        if (!result.target || result.target == "0") {
                            // Workaround for multiple outputs inputting into the same score.  Need to update how scores are defined.
                            result.target = target.target
                        }

                    } else {
                        // If there are no Outputs recorded containing the score, the results won't be returned, so add
                        // one in containing the target.
                        def score = toAggregate.find { it.scoreId == target.scoreId }
                        if (score) {
                            outputSummary << [scoreId: score.scoreId, label: score.label, target: target.target, isOutputTarget: score.isOutputTarget, description: score.description, outputType: score.outputType, category: score.category]
                        } else {
                            // This can happen if the meta-model is changed after targets have already been defined for a project.
                            // Once the project output targets are re-edited and saved, the old targets will be deleted.
                            log.warn "Can't find a score for existing output target: $target.outputLabel $target.scoreLabel, projectId: $project.projectId"
                        }
                    }
                }
            }

            return outputSummary
        } else {
            def error = "Error retrieving metrics for project - no such id ${id}"
            log.error error
            return [status: 'error', error: error]
        }
    }

}
