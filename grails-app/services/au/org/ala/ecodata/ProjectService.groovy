package au.org.ala.ecodata

import au.org.ala.ecodata.converter.SciStarterConverter
import grails.converters.JSON
import grails.core.GrailsApplication
import groovy.json.JsonSlurper
import org.springframework.context.MessageSource
import org.springframework.web.servlet.i18n.SessionLocaleResolver

import java.lang.reflect.UndeclaredThrowableException

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED
import static grails.async.Promises.task

class ProjectService {

    static transactional = false
    static final BASIC = 'basic'
    static final BRIEF = 'brief'
    static final FLAT = 'flat'
    static final ALL = 'all'
    static final PROMO = 'promo'
    static final OUTPUT_SUMMARY = 'outputs'
    static final ENHANCED = 'enhanced'
    static final PRIVATE_SITES_REMOVED = 'privatesitesremoved'

    /** A Map containing a per-project lock for synchronizing locks for updates.  The purpose of this
     * is to support concurrent edits on different project data set summaries which are currently modelled as
     * an embedded array but can be added and updated by both the UI and the Monitor (Parataoo) application API */
    static final Map PROJECT_UPDATE_LOCKS = Collections.synchronizedMap([:].withDefault{ new Object() })
    public static final String PRIMARY_STATE = "primarystate"
    public static final String PRIMARY_ELECT = "primaryelect"
    public static final String PROJECT_STATE_FACET = "projectStateFacet"
    public static final String PROJECT_ELECT_FACET = "projectElectFacet"
    public static final String OTHER_STATES_LIST = "otherStates"
    public static final String OTHER_ELECTORATES_LIST = "otherElectorates"
    public static final String OTHER_STATE = "otherstate"
    public static final String OTHER_ELECT = "otherelect"
    public static final String GEOGRAPHIC_RANGE_OVERRIDDEN = "geographicRangeOverridden"

    GrailsApplication grailsApplication
    MessageSource messageSource
    SessionLocaleResolver localeResolver
    SiteService siteService
    DocumentService documentService
    MetadataService metadataService
    CommonService commonService
    ReportService reportService
    ActivityService activityService
    ProjectActivityService projectActivityService
    PermissionService permissionService
    CollectoryService collectoryService
    WebService webService
    EmailService emailService
    ReportingService reportingService
    OrganisationService organisationService
    UserService userService
    ActivityFormService activityFormService
    RecordService recordService
    LockService lockService
    HubService hubService

  /*  def getCommonService() {
        grailsApplication.mainContext.commonService
    }*/



    def getBrief(listOfIds, version = null) {
        if (listOfIds) {
            if (version) {
                def all = AuditMessage.findAllByProjectIdInListAndEntityTypeAndDateLessThanEquals(listOfIds, Project.class.name, new Date(version as Long), [sort: 'date', order: 'desc'])
                def projects = []
                def found = []
                all?.each {
                    if (!found.contains(it.projectId)) {
                        found << it.projectId
                        if (it.entity.status != DELETED &&
                                (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                            projects << [projectId: it.projectId, name: it.entity.name]
                        }
                    }
                }
            } else {
                Project.findAllByProjectIdInListAndStatusNotEqual(listOfIds, DELETED).collect {
                    [projectId: it.projectId, name: it.name]
                }
            }
        } else {
            []
        }
    }

    def get(String id, levelOfDetail = [], version = null) {
        def p = version ?
                AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(id, Project.class.name, new Date(version as Long), [sort: 'date', order: 'desc', max: 1])[0].entity :
                Project.findByProjectId(id)
        return p ? toMap(p, levelOfDetail, version) : null
    }

    /**
     * Returns a the List of services being delivered by this project with target information for each score.
     * @param projectId the projectId of the project
     * @return
     */
    List<Map> getProjectServicesWithTargets(String projectId) {
        def project = get(projectId)
        if (project)
            return metadataService.getProjectServicesWithTargets(project)
        else
            return null
    }

    def getByDataResourceId(String id, String status = "active", levelOfDetail = []) {
        def project = Project.findByDataResourceIdAndStatus(id, status)
        project ? toMap(project, levelOfDetail) : null
    }

    def list(levelOfDetail = [], includeDeleted = false, citizenScienceOnly = false) {
        def list
        if (!citizenScienceOnly)
            list = includeDeleted ? Project.list() : Project.findAllByStatus(ACTIVE)
        else if (includeDeleted)
            list = Project.findAllByIsCitizenScience(true)
        else
            list = Project.findAllByIsCitizenScienceAndStatus(true, ACTIVE)
        list?.collect { toMap(it, levelOfDetail) }
    }

    def listMeritProjects(levelOfDetail = [], includeDeleted = false) {
        def list = []

        if (includeDeleted) {
            list = Project.findAllByIsMERIT(true)
        } else {
            list = Project.findAllByIsMERITAndStatusNotEqual(true, DELETED)
        }
        list.collect { toMap(it, levelOfDetail) }
    }

    def promoted() {
        def list = Project.findAllByPromoteOnHomepage("yes")
        list.collect { toMap(it, PROMO) }
    }

    def listProjectForAlaHarvesting(Map params, List status = ['active']) {

        def list = Project.createCriteria().list(max: params.max, offset: params.offset) {
            and {
                isNotNull('dataResourceId')
                'eq'("alaHarvest", true)
            }
            order(params.sort, params.order)
        }

        def total = list?.totalCount
        list = list?.collect { toMap(it, "basic") }
        addArchiveLink(list)
        [total: total, list: list]
    }

    /**
     * Adds archive URL to projects
     * @param projects
     * @return
     */
    def addArchiveLink (List projects) {
        projects?.each { it.archiveURL = grailsApplication.config.getProperty("grails.serverURL") + "/ws/project/${it.projectId}/archive" }
    }

    def listProjects(Map params) {
        params = params.clone()
        Map arrange = params.remove('arrange')
        def list = Project.createCriteria().list(max: params.max, offset: params.offset) {

            params.searchCriteria?.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }

            if(arrange?.sort && arrange?.order) {
                order(arrange.sort, arrange.order)
            }
        }

        [total: list?.totalCount, list: list]
    }


    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param prj a Project instance
     * @return map of properties
     */
    Map toMap(project, levelOfDetail = [], includeDeletedActivities = false, version = null) {
        Map result

        Map mapOfProperties = project instanceof Project ? GormMongoUtil.extractDboProperties(project.getProperty("dbo")) : project

        if (levelOfDetail instanceof List) {
            levelOfDetail = levelOfDetail[0]
        }

        if (levelOfDetail == BASIC) {
            result = [
                    projectId     : project.projectId,
                    name          : project.name,
                    dataResourceId: project.dataResourceId,
                    dataProviderId: project.dataProviderId,
                    status        : project.status,
                    alaHarvest    : project.alaHarvest
            ]
        } else if (levelOfDetail == BRIEF) {
            result = [
                    projectId           : project.projectId,
                    name                : project.name,
                    grantId             : project.grantId,
                    externalId          : project.externalId,
                    funding             : project.funding,
                    description         : project.description,
                    status              : project.status,
                    plannedStartDate    : project.plannedStartDate,
                    plannedEndDate      : project.plannedEndDate,
                    associatedProgram   : project.associatedProgram,
                    associatedSubProgram: project.associatedSubProgram
            ]
        } else if (levelOfDetail == PROMO) {
            result = [
                    projectId       : project.projectId,
                    name            : project.name,
                    organisationName: project.organisationName,
                    description     : project.description?.take(200),
                    documents       : documentService.findAllForProjectIdAndIsPrimaryProjectImage(project.projectId, ALL)
            ]
        } else {
            String id = mapOfProperties["_id"].toString()
            mapOfProperties["id"] = id
            mapOfProperties["status"] = mapOfProperties["status"]?.capitalize();
            mapOfProperties.remove("_id")

            if (levelOfDetail != FLAT) {
                mapOfProperties.remove("sites")
                if (levelOfDetail == PRIVATE_SITES_REMOVED) {
                    mapOfProperties.sites = siteService.findAllNonPrivateSitesForProjectId(project.projectId, [SiteService.FLAT])
                } else {
                    mapOfProperties.sites = siteService.findAllForProjectId(project.projectId, [SiteService.FLAT], version)
                }

                // add geographic info attributes such as primarystate, otherstate, primaryelect and otherelect
                mapOfProperties << findAndFormatStatesAndElectoratesForProject(mapOfProperties)
                mapOfProperties.documents = documentService.findAllForProjectId(project.projectId, levelOfDetail, version)
                mapOfProperties.links = documentService.findAllLinksForProjectId(project.projectId, levelOfDetail, version)
                Lock lock = lockService.get(project.projectId)
                if (lock) {
                    mapOfProperties.lock = lock
                }

                if (levelOfDetail == ALL) {
                    mapOfProperties.activities = activityService.findAllForProjectId(project.projectId, levelOfDetail, includeDeletedActivities)
                    List<Report> reports = reportingService.findAllForProject(project.projectId)
                    if (reports) {
                        mapOfProperties.reports = reports
                    }
                } else if (levelOfDetail == OUTPUT_SUMMARY) {
                    mapOfProperties.outputSummary = projectMetrics(project.projectId, false, true)
                }
                if (levelOfDetail == ENHANCED) {
                    project.activities = activityService.findAllForProjectId(project.projectId, ActivityService.FLAT, includeDeletedActivities)

                    mapOfProperties.actualStartDate = project.actualStartDate ?: ''
                    mapOfProperties.actualEndDate = project.actualEndDate ?: ''
                    mapOfProperties.plannedDurationInWeeks = project.plannedDurationInWeeks
                    mapOfProperties.actualDurationInWeeks = project.actualDurationInWeeks
                    mapOfProperties.contractDurationInWeeks = project.contractDurationInWeeks
                }
            }

            result = mapOfProperties.findAll { k, v -> v != null }
            //result = GormMongoUtil.deepPrune(mapOfProperties)

            //Fetch name of MU
            if (result?.managementUnitId) {
                ManagementUnit mu = ManagementUnit.findByManagementUnitId(result.managementUnitId)
                result['managementUnitName'] = mu?.name
            }
            // Populate the associatedProgram and associatedSubProgram properties if the programId exists.
            if (result?.programId) {
                Program program = Program.findByProgramId(result.programId)
                if (program) {
                    if (program.parent) {
                        result['associatedProgram'] = program.parent.name
                        result['associatedSubProgram'] = program.name
                    } else {
                        result['associatedProgram'] = program.name
                    }
                }
            }

            // look up current associated organisation details
            result.associatedOrgs?.each {
                if (it.organisationId) {
                    Organisation org = Organisation.findByOrganisationId(it.organisationId)
                    if (org) {
                        if (!it.name) { // Is this going to cause BioCollect an issue?
                            it.name = org.name
                        }
                        it.url = org.url
                        it.logo = Document.findByOrganisationIdAndRoleAndStatus(it.organisationId, "logo", ACTIVE)?.thumbnailUrl
                    }
                }
            }
        }

        result
    }

    /**
     * Converts the domain object into a highly detailed map of properties, including
     * dynamic properties, and linked components.
     * @param prj a Project instance
     * @return map of properties
     */
    def toRichMap(prj) {
        def mapOfProperties = prj.getProperty("dbo")
        //def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties["status"] = mapOfProperties["status"]?.capitalize();
        mapOfProperties.remove("_id")
        mapOfProperties.remove("sites")
        mapOfProperties.sites = siteService.findAllForProjectId(prj.projectId, true)
        // remove nulls
        mapOfProperties.findAll { k, v -> v != null }
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props, boolean collectoryLink = true, boolean overrideUpdateDate = false) {

        try {
            if (props.projectId && Project.findByProjectId(props.projectId)) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Project.withSession { session -> session.clear() }
                return [status: 'error', error: 'Duplicate project id for create ' + props.projectId]
            }

            props = includeProjectFundings(props)
            // name is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            Project project = new Project(projectId: props.projectId ?: Identifiers.getNew(true, ''), name: props.name)
            // Not flushing on create was causing that further updates to fields were overriden by old values
            project.save(flush: true, failOnError: true)

            props.remove('sites')
            props.remove('id')

            if (collectoryLink) {
                List projectActivities = projectActivityService.getAllByProject(props.projectId)
                props = includeProjectActivities(props, projectActivities)
                updateCollectoryLinkForProject(project, props)
            }
            bindEmbeddedProperties(project, props)
            commonService.updateProperties(project, props, overrideUpdateDate)
            return [status: 'ok', projectId: project.projectId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Project.withSession { session -> session.clear() }
            def error = "Error creating project - ${e.message}"
            log.error error, e
            return [status: 'error', error: error]
        }
    }

    // Use Grails data binding here as simply assigning the property can't
    // correctly convert the list of maps to a list of AssociatedOrg.
    // Ideally, the whole Project entity would be mapped using standard data binding
    // instead of the common service, but that is a bit risky for a quick fix.
    // See https://github.com/AtlasOfLivingAustralia/ecodata/issues/708
    private void bindEmbeddedProperties(Project project, Map properties) {
        List embeddedPropertyNames = ['associatedOrgs', 'externalIds', 'geographicInfo', 'outputTargets']
        for (String prop in embeddedPropertyNames) {
            if (properties[prop]) {
                project.properties = [(prop):properties.remove(prop)]
            }
        }
    }


    /**
     * Include project funding data.
     * @param props Project properties
     * @return
     */
    private Map includeProjectFundings(Map props) {
        if (props?.fundings) {
            List fundings = []
            props.fundings.each {
                fundings.add(new Funding(it));
            }
            props.fundings = fundings;
        }
        return props
    }


    /**
     * Include project activities specific to BioCollect projects.
     * @param props Project properties
     * @param projectActivities Project Activity/ Survey data
     * @return
     */
    private Map includeProjectActivities(Map props, List projectActivities) {
        if (props && projectActivities) {
            props.citation = buildProjectCitation(projectActivities)
            props.methodStepDescription = buildMethodDescription(projectActivities)
            props.qualityControlDescription = buildQualityControlDescription(projectActivities)
        }
        return props
    }

    private buildProjectCitation(List projectActivities) {

        String citation = ""
        projectActivities.each {
            citation += it.name + ": " + projectActivityService.generateCollectoryAttributionText(it as ProjectActivity) + "\n"
        }
        return citation
    }

    private buildMethodDescription(List projectActivities) {
        String method = ""
        projectActivities.each {
            String name = it.name + " method:"
            method = [method, name, it.methodType, it.methodName, it.methodUrl].findAll({ it != null }).join("\n")
        }
        return method
    }

    private buildQualityControlDescription(List projectActivities) {
        String qualityDescription = ""
        String assurance_methods = null
        String assurance_description = null
        String policy_description = null
        String policy_url = null

        projectActivities.each {
            String name = it.name + " data quality description:"

            if (it.dataQualityAssuranceMethods) {
                String method_string = it.dataQualityAssuranceMethods.join(", ")
                assurance_methods = "Data quality assurance methods: " + method_string
            }
            if (it.dataQualityAssuranceDescription) {
                assurance_description = "Data quality assurance description: " + it.dataQualityAssuranceDescription
            }

            if (it.dataManagementPolicyDescription) {
                policy_description = "Data Management policy description: " + it.dataManagementPolicyDescription
            }

            if (it.dataManagementPolicyURL) {
                policy_url = "Data Management policy url: " + it.dataManagementPolicyURL
            }
            qualityDescription = [qualityDescription, name, assurance_methods, assurance_description, policy_description, policy_url].findAll({ it != null }).join("\n")
        }
        return qualityDescription
    }

    private updateCollectoryLinkForProject(Project project, Map props) {


        if (!project.isExternal && Boolean.valueOf(grailsApplication.config.getProperty('collectory.collectoryIntegrationEnabled'))) {

            Map projectProps = toMap(project, FLAT)
            task {
                Project.withNewSession { session ->
                    collectoryService.updateDataResource(projectProps, props)
                    session.flush()
                }
            }.onComplete {
                log.info("Completed task to link project with collectory - ${project.name} (id = ${project.projectId})")
            }.onError { Throwable error ->
                if (error instanceof UndeclaredThrowableException) {
                    error = error.undeclaredThrowable
                }
                String message = "Failed to update collectory link for project ${project.name} (id = ${project.projectId})"
                log.error(message, error)
                emailService.sendEmail(message, "Error: ${error.message}", [grailsApplication.config.getProperty('ecodata.support.email.address')])
            }
        }
    }

    def update(Map props, String id, Boolean shouldUpdateCollectory = true) {
        synchronized (PROJECT_UPDATE_LOCKS.get(id)) {
            Project project = Project.findByProjectId(id)
            if (project) {
                // retrieve any project activities associated with the project
                List projectActivities = projectActivityService.getAllByProject(id)
                props = includeProjectFundings(props)
                props = includeProjectActivities(props, projectActivities)

                try {
                    // Custom currently holds keys "details" and "dataSets".  Only update the "custom" properties
                    // that are supplied in the update, leaving the others intact.
                    if (project.custom && props.custom) {
                        project.custom.putAll(props.remove('custom'))
                    }
                    bindEmbeddedProperties(project, props)
                    commonService.updateProperties(project, props)
                    if (shouldUpdateCollectory) {
                        updateCollectoryLinkForProject(project, props)
                    }
                    return [status: 'ok']
                } catch (Exception e) {
                    Project.withSession { session -> session.clear() }
                    def error = "Error updating project ${id} - ${e.message}"
                    log.error error, e
                    return [status: 'error', error: error]
                }
            } else {
                def error = "Error updating project - no such id ${id}"
                log.error error
                return [status: 'error', error: error]
            }
        }
    }

    /**
     * Deletes a project and any associated activities, outputs and user permissions.  The
     * project is removed from any sites it it associated with.  Orphaned sites are not
     * deleted.
     * @param id the id of the project to delete.
     * @param destroy if false, all deletes will be status updates (a soft delete).  Note that
     * the permissions will be deleted and site associations removed, even in the soft delete case.
     */
    Map delete(String id, boolean destroy) {
        Map result

        Project project = Project.findByProjectId(id)

        if (project) {
            getActivityIdsForProject(id).each {
                activityService.delete(it, destroy)
            }

            projectActivityService.getAllByProject(id).each {
                projectActivityService.delete(it.projectActivityId, destroy)
            }

            permissionService.deleteAllForProject(id, destroy)

            documentService.deleteAllForProject(id, destroy)

            siteService.deleteSitesFromProject(id)

            if (destroy) {
                project.delete(flush: true)
                webService.doDelete(grailsApplication.config.getProperty('collectory.baseURL') + 'ws/dataProvider/' + id)
            } else {
                project.status = DELETED
                project.save(flush: true)
            }

            if (project.hasErrors()) {
                result = [status: 'error', error: project.getErrors()]
            } else {
                result = [status: 'ok']
            }
        } else {
            result = [status: 'error', error: 'No such id']
        }

        result
    }

    /**
     * Returns the reportable metrics for a project as determined by the project output targets and activities
     * that have been undertaken.
     * @param id identifies the project.
     * @return a Map containing the aggregated results.
     *
     */
    def projectMetrics(String id, targetsOnly = false, approvedOnly = false, List scoreIds = null, Map aggregationConfig = null, boolean includeTargets = true) {
        def p = Project.findByProjectId(id)
        if (p) {
            def project = toMap(p, ProjectService.FLAT)

            List toAggregate
            if (scoreIds && targetsOnly) {
                toAggregate = Score.findAllByScoreIdInListAndIsOutputTarget(scoreIds, true)
            } else if (scoreIds) {
                toAggregate = Score.findAllByScoreIdInList(scoreIds)
            } else {
                toAggregate = targetsOnly ? Score.findAllByIsOutputTarget(true) : Score.findAll()
            }

            List outputSummary = reportService.projectSummary(id, toAggregate, approvedOnly, aggregationConfig) ?: []

            // Add project output target information where it exists.
            if (includeTargets) {
                project.outputTargets?.each { target ->
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

    /**
     * This method calculates the current scores for the project identified by the supplied activity id
     * and separately calculates the contribution to the score from either the supplied activityData or
     * the saved output data for that activity.
     * The purpose is to allow the client to detect where a score has over-delivered a target to allow action
     * to be taken.
     * @param activityId the activity of interest
     * @param activityData if supplied, this data will be used instead of any saved Output data for the activity.
     * @return a Map [projectScores:<score data>, activityScores:<score data>] where <score data> is in the format
     * returned by ReportService::aggregateActivities
     */
    Map scoreDataForActivityAndProject(String activityId, Map activityData = null) {
        Map activity = activityService.get(activityId)

        ActivityForm form = activityFormService.findActivityForm(activity.type, activity.formVersion)
        List<Score> scores = activityFormService.findScoresThatReferenceForm(form)

        List projectResults = projectMetrics(activity.projectId, true, false, scores.collect{it.scoreId})
        List activityResults = reportService.aggregateActivities([activityData ?: activity], scores)

        [projectScores:projectResults, activityScores:activityResults]

    }

    List<String> getActivityIdsForProject(String projectId) {
        Activity.withCriteria {
            eq("projectId", projectId)
            projections {
                property("activityId")
            }
        }
    }

    List<String> getAllMERITProjectIds() {
        Project.withCriteria {
            eq("isMERIT", true)
            projections {
                property("projectId")
            }
        }
    }

    /**
     * Performs a case-insensitive search by project name
     * @param name The project name to search for
     * @return List of 'brief' projects with the same name (case-insensitive)
     */
    List<Map> findByName(String name) {
        List<Map> matches = []

        if (name) {
            name = name.replaceAll(" +", " ").trim()
            matches = Project.withCriteria {
                ne "status", DELETED
                rlike "name", "(?i)^${name}\$"
            }.collect { toMap(it, LevelOfDetail.brief) }
        }

        matches
    }

    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a list of the projects that match the supplied criteria
     */
    List<Map> search(Map searchCriteria, levelOfDetail = []) {

        def criteria = Project.createCriteria()

        def projects = criteria.list {
            ne("status", DELETED)
            searchCriteria.each { prop, value ->
                // Special case for organisationId - also included embedded associatedOrg relationships.
                if (prop == 'organisationId') {
                    or {
                        if (value instanceof List) {
                            inList(prop, value)
                        } else {
                            eq(prop, value)
                        }

                        associatedOrgs {
                            if (value instanceof List) {
                                inList(prop, value)
                            } else {
                                eq(prop, value)
                            }
                        }
                    }
                }
                else {
                    if (value instanceof List) {
                        inList(prop, value)
                    } else {
                        eq(prop, value)
                    }
                }

            }

        }
        projects.collect { toMap(it, levelOfDetail) }
    }

    /**
     * Returns all projects with the specified owner field
     * @param ownerProperty the property that specifies the project relationship (e.g organisationId)
     * @param id the id of the related entity.
     * @param levelOfDetail the amount of data to return for each project.
     * @return a List of projects matching the supplied property
     */
    List<Map> findAllByAssociation(String property, String id, levelOfDetail = []) {
        search([(property): id], levelOfDetail)
    }

    /**
     * Updates the organisation name for all projects with the organisation id.
     * (The name is stored alongside the id in the project because not all organisations have entries in the database).
     * @param orgId identifies the organsation that has changed name
     * @param orgName the new organisation name
     */
    void updateOrganisationName(String orgId, String oldName, String newName) {
        Project.findAllByOrganisationIdAndOrganisationNameAndStatusNotEqual(orgId, oldName, DELETED).each { project ->
            project.organisationName = newName
            project.save(flush:true)
        }

        List projects = Project.where {
            status != DELETED
            associatedOrgs {
                organisationId == orgId
                name == oldName
            }
        }.list()


        projects?.each { Project project ->
            project.associatedOrgs.each { org ->
                if (org.organisationId == orgId && org.name == oldName) {
                    org.name = newName
                }
            }
            project.save(flush:true)
        }
    }

    Map getSciStarterProjectsPage(JsonSlurper slurper, int page = 1) {
        String baseUrl = grailsApplication.config.getProperty("scistarter.baseUrl")
        String finderUrl = grailsApplication.config.getProperty("scistarter.finderUrl")
        String apiKey = grailsApplication.config.getProperty("scistarter.apiKey")
        String url = "${baseUrl}${finderUrl}?format=json&key=${apiKey}&page=${page}"

        String data = webService.get(url, false)

        return slurper.parseText(data)
    }

    /**
     * Import SciStarter projects to Biocollect. Import script does the following.
     * 1. gets the list of projects and contacts SciStarter for more details on a project
     * 2. checks if the project is already imported, if yes, update fields. TODO
     * 3. if project does not exist, create a new project, organisation, project extent and project logo document.
     *      And link artifacts to the project. TODO: creating project extent.
     * @return
     */
    Map importProjectsFromSciStarter() {
        int ignoredProjects = 0, createdProjects = 0, updatedProjects = 0, page = 1
        JsonSlurper slurper = new JsonSlurper()

        log.info("Starting SciStarter import")
        try {

            while(true) {
                // list SciStarter projects for the current page
                Map data = getSciStarterProjectsPage(slurper, page)
                log.info("-- PAGE ${page}/${Math.round(data.total / 10)} SCISTARTER --")

                // Break the loop if there are no more projects left to import
                if (data.entities.size() == 0) break

                data.entities.eachWithIndex { project, index ->
                    try {
                        Project existingProject = Project.findByExternalIdAndIsSciStarter(project.legacy_id.toString(), true)

                        if (project.origin == 'atlasoflivingaustralia') {
                            // ignore projects SciStarter imported from BioCollect
                            log.info("Ignoring ALA project ${project.name} - ${project.id}")
                            ignoredProjects++
                        } else {
                            // map properties from SciStarter to Biocollect
                            Map transformedProject = SciStarterConverter.convert(project)
                            if (!existingProject) {
                                // create project & document & site & organisation
                                createSciStarterProject(transformedProject, project)
                                log.info("Creating ${project.name} in ecodata")

                                createdProjects++
                            } else {
                                // update a project just in case something has changed.
                                updateSciStarterProject(transformedProject, existingProject)

                                log.info("Updating ${existingProject.name} ${existingProject.projectId}.")
                                updatedProjects++
                            }
                        }
                    }  catch (Exception e) {
                        log.error("Error processing project - ${project.name}. Ignoring it. ${e.message}", e);
                    }
                }
                page++
            }

            log.info("Number of created projects ${createdProjects}. Number of ignored projects ${ignoredProjects}. Number of projects updated ${updatedProjects}.")
        } catch (SocketTimeoutException ste) {
            log.error(ste.message, ste)
        } catch (Exception e) {
            log.error(e.message, e)
        }

        log.info("Completed SciStarter import")
        [created: createdProjects, updated: updatedProjects, ignored: ignoredProjects]
    }

    /**
     * Creates a project in the database. It also creates all associated artifacts like organisation, document, site etc
     * @param transformedProp - mapped SciStarter project properties
     * @return
     */
    Map createSciStarterProject(Map transformedProp, Map rawProp) {
        Map organisation

        // create project extent
        Map sites = createSciStarterSites(rawProp)
        String projectSiteId
        if (sites?.siteIds?.size()) {
            projectSiteId = sites.siteIds[0]
        }

        transformedProp.projectSiteId = projectSiteId

        // create organisation
        if (transformedProp.organisationName) {
            organisation = createSciStarterOrganisation(transformedProp.organisationName)
            if (organisation.organisationId) {
                transformedProp.organisationId = organisation.organisationId
            } else {
                // throw exception?
            }
        }

        // remove unnecessary properties
        String imageUrl = transformedProp.remove('image')
        String attribution = transformedProp.remove('attribution')
        transformedProp.remove('projectId')
        // create project. do not call collectory to create data provider and data resource id
        Map project = create(transformedProp, false, true)
        String projectId = project.projectId

        // use the projectId to associate site with  project
        if (projectId) {
            sites?.siteIds?.each { siteId ->
                siteService.addProject(siteId, projectId)
            }
            // create project logo.
            createSciStarterLogo(imageUrl, attribution, projectId)

            return project
        } else {
            // todo: reverse transactions
        }
    }

    /**
     * Update a project. It updates only project properties and image. It does not change the
     * organisation and site.
     * @param transformedProp - mapped SciStarter project properties
     * @param project - project instance
     * @return
     */
    Map updateSciStarterProject(Map transformedProp, Project project) {
        // remove properties
        transformedProp.remove('projectId')
        transformedProp.remove('organisationId')
        transformedProp.remove('projectSiteId')
        transformedProp.remove('manager')

        String imageUrl = transformedProp.remove('image')
        String attribution = transformedProp.remove('attribution')
        String projectId = project.projectId
        commonService.updateProperties(project, transformedProp, true)
        updateSciStarterLogo(imageUrl, attribution, projectId)
    }

    /**
     * Create sites for a project. if a project has regions then create sites using it.
     * if project does not have region then set extent to the whole world map.
     * @return
     */
    Map createSciStarterSites(Map project) {
        Map result = [siteIds: null]
        List sites = []
        if (project.regions) {
            // convert region to site
            Map site = SciStarterConverter.siteMapping(project)
            // only add valid geojson objects
            if (site?.extent?.geometry && siteService.isGeoJsonValid((site?.extent?.geometry as JSON).toString())) {
                Map createdSite = siteService.create(site)
                if (createdSite.siteId) {
                    sites.push(createdSite.siteId)
                }
            }

            result.siteIds = sites
        } else {
            // if no region, then create world extent.
            String siteId = getWorldExtent()
            if (siteId) {
                result.siteIds = [siteId]
            }
        }

        result
    }

    /**
     * Create project logo. Logo are stored in document collection in Biocollect.
     * @param imageUrl
     * @param attribution
     * @param projectId
     * @return
     */
    Map createSciStarterLogo(String imageUrl, String attribution, String projectId) {
        Map props = [
                "externalUrl"                         : imageUrl,
                "isPrimaryProjectImage"               : true,
                "projectId"                           : projectId,
                "attribution"                         : attribution,
                "role"                                : "logo",
                "status"                              : "active",
                "type"                                : "image",
                "hasPreview"                          : false,
                "readOnly"                            : false,
                "thirdPartyConsentDeclarationRequired": false,
                "public"                              : true,
                "stages"                              : [],
                "embeddedVideoVisible"                : false,
                "isSciStarter"                        : true
        ]
        // create logo document
        documentService.create(props, null)
    }

    /**
     * Update project logo.
     * @param imageUrl
     * @param attribution
     * @param projectId
     * @return
     */
    Map updateSciStarterLogo(String imageUrl, String attribution, String projectId) {
        Document doc = Document.findByProjectIdAndIsPrimaryProjectImageAndRole(projectId, true, "logo")
        if (doc) {
            commonService.updateProperties(doc, [
                    "externalUrl": imageUrl,
                    "attribution": attribution
            ])
        } else if (imageUrl) {
            // create if image not present
            createSciStarterLogo(imageUrl, attribution, projectId)
        }
    }

    /**
     * Check organisation.
     * 1. if exist, use it.
     * 2. otherwise, create new
     * @param name
     * @return
     */
    Map createSciStarterOrganisation(String name) {
        Organisation org = Organisation.findByName(name)
        if (org) {
            return [organisationId: org.organisationId]
        } else {
            // create organisation
            Map orgProp = [
                    "collectoryInstitutionId": "null",
                    "name"                   : name,
                    "orgType"                : "conservation",
                    "description"            : "This organisation is imported from SciStarter"
            ]
            return organisationService.create(orgProp, false)
        }
    }

    /**
     * World extent is used as project area of all SciStarter Projects without project area. This function
     * creates a new world extent every time it is called.
     * @return - siteId - 'abcd-sds'
     */
    String getWorldExtent() {
        // use JSON.parse since JSONSlurper converts numbers to BigDecimal which throws error on serialization.
        Object world = JSON.parse(getClass().getResourceAsStream("/data/worldExtent.json")?.getText())
        Map site = siteService.create(world)
        return site.siteId
    }

    /**
     * Get a String that describes a project. This used for indexing purposes currently.
     * The motivaion for this method was because there was no single field to distinguish between projects but had multiple
     * fields like isWorks, isCitizenScience.
     * There is a ticket to have a single field - https://github.com/AtlasOfLivingAustralia/biocollect/issues/655
     * This method will become redundant when the above is implemented.
     */
    String getTypeOfProject(Map projectMap) {
        if (projectMap.isWorks) {
            return "works"
        } else if (projectMap.isMERIT) {
            return "merit"
        } else if (projectMap.isCitizenScience) {
            return "citizenScience"
        } else if (projectMap.isEcoScience) {
            return "ecoScience"
        }
    }

    /**
     * Returns a list of times the project MERI plan has been approved.
     * @param projectId the project to get the approval history for.
     * @return a List of Maps with keys approvalDate, approvedBy.
     */
    List getMeriPlanApprovalHistory(String projectId){
        Map results = documentService.search([projectId:projectId, role:'approval', labels:'MERI'])
        List<Map> histories = []
        results?.documents.collect{
            def data = documentService.readJsonDocument(it)

            if (!data.error){
                String displayName = userService.lookupUserDetails(data.approvedBy)?.displayName ?: 'Unknown'
                def doc = [
                        approvalDate:data.dateApproved,
                        approvedBy:displayName,
                        comment:data.reason,
                        changeOrderNumber:data.referenceDocument
                ]
                histories.push(doc)
            }
        }
        histories
    }

    /**
     * Returns the date and user of the most recent approval of the project MERI plan
     * @param projectId the project.
     * @return Map with keys approvalDate and approvedBy.  Null if the plan has not been approved.
     */
    Map getMostRecentMeriPlanApproval(String projectId) {
        List<Map> meriApprovalHistory = getMeriPlanApprovalHistory(projectId)
        meriApprovalHistory.max{it.approvalDate}
    }

    /**
     * Checks if a user have a role on an existing MERIT project.
     * @param userId
     * @param hubId
     * @return true if user have a role on an existing merit project
     */
    Boolean doesUserHaveHubProjects(String userId, String hubId) {
        List<UserPermission> ups = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqualAndStatusNotEqual(userId, Project.class.name, AccessLevel.starred, DELETED)
        int count = 0
        ups.each {
            count += Project.countByProjectIdAndHubId(it?.entityId, hubId)
        }
        count > 0
    }

    List fetchDataSetRecords (String projectId, String dataSetId) {
        int batchSize = 10, count = 10, offset = 0
        List records = []
        while (batchSize == count) {
            def response = Record.findAllByProjectIdAndDataSetId(projectId, dataSetId, [max: batchSize, offset: offset])
            count = records.size()
            response = response.collect { recordService.toMap(it) }
            records.addAll(response)
            offset += count
        }

        records
    }

    /**
     * Updates a single data set associated with a project.  Because the datasets are stored as an embedded
     * array in the Project collection, this method is synchronized on the project to avoid concurrent updates to
     * different data sets overwriting each other.
     * Due to the way it's been modelled as an embedded array, the client is allowed to supply a dataSetId
     * when creating a new data set (e.g. a data set created by a submission from the Monitor app uses the
     * submissionId as the dataSetId).
     * @param projectId The project to update
     * @param dataSet the data set to update.
     * @return
     */
    Map updateDataSet(String projectId, Map dataSet) {
       updateDataSets(projectId, [dataSet])
    }

    /**
     * Updates multiple data sets associated with a project at the same time.  This method exists to support
     * the use case of associating multiple data sets with a report and updating their publicationStatus when
     * the report is submitted/approved.
     *
     * Because the datasets are stored as an embedded
     * array in the Project collection, this method is synchronized on the project to avoid concurrent updates to
     * different data sets overwriting each other.
     * Due to the way it's been modelled as an embedded array, the client is allowed to supply a dataSetId
     * when creating a new data set (e.g. a data set created by a submission from the Monitor app uses the
     * submissionId as the dataSetId).
     * @param projectId The project to update
     * @param dataSet the data sets to update.
     * @return
     */
    Map updateDataSets(String projectId, List dataSets) {
        synchronized (PROJECT_UPDATE_LOCKS.get(projectId)) {
            Project.withNewSession { // Ensure that the queried Project is not cached in the current session which can cause stale data
                Project project = Project.findByProjectId(projectId)
                if (!project) {
                    return [status: 'error', error: "No project exists with projectId=${projectId}"]
                }
                for (Map dataSet in dataSets) {
                    if (!dataSet.dataSetId) {
                        dataSet.dataSetId = Identifiers.getNew(true, '')
                    }
                    Map matchingDataSet = project.custom?.dataSets?.find { it.dataSetId == dataSet.dataSetId }
                    if (matchingDataSet) {
                        matchingDataSet.putAll(dataSet)
                    } else {
                        if (!project.custom) {
                            project.custom = [:]
                        }
                        if (!project.custom?.dataSets) {
                            project.custom.dataSets = []
                        }
                        project.custom.dataSets.add(dataSet)
                    }
                }
                update([custom: project.custom], project.projectId, false)
            }
        }
    }

    /** Data sets that have been used in a Report cannot be deleted */
    private static boolean canModifyDataSet(Map dataSet) {
        return (!dataSet.publicationStatus || dataSet.publicationStatus == PublicationStatus.DRAFT) && !dataSet.reportId
    }

    /**
     * Returns true if every data set associated with the site can be modified.
     * @param site the site to check
     * @param project optionally supplied to prevent re-querying a project already available in the calling context
     */
    boolean canModifyDataSetSite(Map site, Project project = null) {
        if (!site) {
            return false
        }
        boolean canModifySite = true
        if (site.projects.size() != 1) {
            canModifySite = false
        }
        else {
            if (site.projects[0] != project?.projectId) {
                project = Project.findByProjectId(site.projects[0])
            }
        }

        if (project) {
            project.custom?.dataSets?.each { Map dataSet ->
                if (dataSet.siteId == site.siteId) {
                    canModifySite = canModifySite && canModifyDataSet(dataSet)
                }
            }
        }
        canModifySite
    }

    /**
     * Returns true if the Site associated with a data set can be deleted.  Generally it can only be
     * deleted if it was created by the Monitor app and is only associated with the supplied data set and no others.
     * @param dataSetId The id of the data set the Site is related to.
     * @param site The Site to check
     * @param project The Project related to the Site to check (used to prevent an unnecessary query if available.
     * @return true if the Site can be deleted.
     */
    private boolean canDeleteDataSetSite(String dataSetId, Map site, Project project = null) {
        if (!site) {
            return false
        }
        boolean canDelete = true
        /// Don't delete the site if it's used by another Project
        if (site.projects.size() != 1) {
            canDelete = false
        }
        else {
            if (site.projects[0] != project?.projectId) {
                project = Project.findByProjectId(site.projects[0])
            }
        }

        // Don't delete the site if it's used by another data set in this project.
        if (project) {
            project.custom?.dataSets?.each { Map dataSet ->
                if (dataSet.siteId == site.siteId && dataSet.dataSetId != dataSetId) {
                    canDelete = false
                }
            }
        }
        canDelete
    }

    Map deleteDataSet(String projectId, String dataSetId) {
        synchronized (PROJECT_UPDATE_LOCKS.get(projectId)) {
            Map result
            Project project = Project.findByProjectId(projectId)

            Map matchingDataSet = project?.custom?.dataSets?.find { it.dataSetId == dataSetId }

            if (!matchingDataSet || !canModifyDataSet(matchingDataSet)) {
                return [status: 'error', error: 'Data set with id: '+dataSetId + ' cannot be deleted']
            }
            else {
                project.custom.dataSets.remove(matchingDataSet)
                result = update([custom: project.custom], project.projectId, false)

                if (result.status != 'error') {
                    String activityId = matchingDataSet.activityId
                    if (activityId) {
                        Map activityResult = activityService.delete(activityId)
                        result.activityStatus = activityResult.status
                    }
                    String siteId = matchingDataSet.siteId
                    if (siteId) {

                        Map site = siteService.get(siteId)
                        if (canDeleteDataSetSite(dataSetId, site, project)) {
                            siteService.delete(siteId)
                        }

                    }

                }

            }
            result
        }
    }

    /**
     * Returns a list of all projects that have been updated since the specified date.
     * @param date the date to compare against
     * @return a list of projects
     */
    Map orderLayerIntersectionsByAreaOfProjectSites (Map project) {
        Map<String,Map<String,Double>> sumOfIntersectionsByLayer = [:].withDefault { [:].withDefault { 0 } }
        Map orderedIntersectionsByArea = [:]
        Map config = metadataService.getGeographicConfig(project.hubId)
        List layers = config?.checkForBoundaryIntersectionInLayers
        List projectSites = getRepresentativeSitesOfProject(project)
        projectSites?.each { Map site ->
            layers?.each { String layer ->
                Map facet = metadataService.getGeographicFacetConfig(layer, project.hubId)
                site.extent?.geometry?.get(SpatialService.INTERSECTION_AREA)?.get(facet.name)?.get(SiteService.INTERSECTION_CURRENT)?.each { String layerValue, value ->
                    sumOfIntersectionsByLayer[layer][layerValue] += value
                }
            }
        }

        sumOfIntersectionsByLayer.each { String layerId, Map value ->
            orderedIntersectionsByArea[layerId] = value.sort { entry ->
                -entry.value
            }.keySet().toList()
        }

        orderedIntersectionsByArea
    }

    /**
     * Get representative sites of a project.
     * 1. All sites except project area. This includes Reporting, EMSA and Planning sites.
     * 2. Get project area(s)
     * 3. If there are no sites associated with project, return Management Unit boundaries.
     * 4. Where none exist, return none
     * @param project
     * @return
     */
    List getRepresentativeSitesOfProject(Map project) {
        if (project) {
            List sites = project.sites
            List projectSites = siteService.findAllSitesExceptProjectArea(sites) ?: []
            if (projectSites.isEmpty()) {
                projectSites =  siteService.findAllSitesByTypeProjectArea(sites) ?: []
                if (projectSites.isEmpty() && project.managementUnitId) {
                    ManagementUnit mu = ManagementUnit.findByManagementUnitId(project.managementUnitId)
                    String managementUnitSiteId = mu?.managementUnitSiteId
                    if (managementUnitSiteId) {
                        Site muSite = Site.findBySiteId(managementUnitSiteId)
                        if (muSite) {
                            projectSites = [siteService.toMap(muSite, [SiteService.FLAT])]
                        }
                    }
                }
            }

            return projectSites
        }

        []
    }

    Map findAndFormatStatesAndElectoratesForProject(Map project) {
        Map result = findStateAndElectorateForProject (project)
        if(!result) {
            return [:]
        }

        List electorates = result.remove(OTHER_ELECTORATES_LIST) as List
        List states = result.remove(OTHER_STATES_LIST) as List
        def separator = "; "
        if (states) {
            result[OTHER_STATE] = states.join(separator)
        }

        if (electorates) {
            result[OTHER_ELECT] = electorates.join(separator)
        }

        result
    }

    /**
     * Find primary/other state(s)/electorate(s) for a project.
     * 1. Get eligible sites and do automatic ordering of states and electorates
     * 2. If overridePrimaryState and/or overridePrimaryElectorate is true, then override calculated value with manual value.
     * 3. Add all other states and electorates to end of list
     * 4. Remove any value in exclude list
     * @params project - map of a project with all sites associated in sites property
     * @return [
     *  "projectStateFacet": [] - all states
     *  "projectElectFacet": [] - all electorates
     *  "primarystate": "",
     *  "primaryelect": "",
     *  "otherStates": []
     *  "otherElectorates": []
     * ]
     */
    Map findStateAndElectorateForProject (Map project) {
        Map result = [:]
        if(project == null) {
            return result
        }

        Map geographicInfo = project?.geographicInfo
        if (!geographicInfo?.statewide && !geographicInfo?.nationwide) {
            Map intersections = orderLayerIntersectionsByAreaOfProjectSites(project)
            Map config = metadataService.getGeographicConfig()
            List intersectionLayers = config?.checkForBoundaryIntersectionInLayers
            intersectionLayers?.each { layer ->
                Map facetName = metadataService.getGeographicFacetConfig(layer)
                if (facetName.name) {
                    List intersectionValues = intersections[layer]
                    if (intersectionValues) {
                        result["project${facetName.name.capitalize()}Facet"] = intersectionValues
                    }
                }
                else
                    log.error ("No facet config found for layer $layer.")
            }
        }

        // take a copy of calculated states & electorates for comparison
        List statesInferredFromSites = result[PROJECT_STATE_FACET] ? new ArrayList<String>(result[PROJECT_STATE_FACET]) : []
        List electoratesInferredFromSites = result[PROJECT_ELECT_FACET] ? new ArrayList<String>(result[PROJECT_ELECT_FACET]) : []

        // override primary state with manually entered value
        if (geographicInfo?.overridePrimaryState) {
            if (geographicInfo.primaryState) {
                List states = result[PROJECT_STATE_FACET] = result[PROJECT_STATE_FACET] ?: []
                states.add(0, geographicInfo.primaryState)
                states.unique()
            }
        }

        // override primary electorate with manually entered value
        if (geographicInfo?.overridePrimaryElectorate) {
            if (geographicInfo.primaryElectorate) {
                List elects = result[PROJECT_ELECT_FACET] = result[PROJECT_ELECT_FACET] ?: []
                elects.add(0, geographicInfo.primaryElectorate)
                elects.unique()
            }
        }

        List otherStates = new ArrayList(result[PROJECT_STATE_FACET] ?: [] as Collection)
        List otherElectorates = new ArrayList(result[PROJECT_ELECT_FACET] ?: [] as Collection)
        // choose primary states and electorates here so that values in other fields do not
        // influence the result
        result[PRIMARY_STATE] = otherStates ? otherStates.pop() : null
        result[PRIMARY_ELECT] = otherElectorates ? otherElectorates.pop() : null

        // adds missing states
        if (geographicInfo?.otherStates) {
            otherStates.addAll(geographicInfo.otherStates)
            otherStates = otherStates.toUnique()
        }

        // removes excluded states
        if (geographicInfo?.otherExcludedStates) {
            otherStates.removeAll(geographicInfo.otherExcludedStates)
        }

        // otherElectorates are by default added to end of list
        if (geographicInfo?.otherElectorates) {
            otherElectorates.addAll(geographicInfo.otherElectorates)
            otherElectorates = otherElectorates.toUnique()
        }

        // removes excluded electorates
        if (geographicInfo?.otherExcludedElectorates) {
            otherElectorates.removeAll(geographicInfo.otherExcludedElectorates)
        }

        // update fields containing all states and electorates
        result[PROJECT_STATE_FACET] = result[PRIMARY_STATE] ? [result[PRIMARY_STATE]] + otherStates : otherStates
        result[PROJECT_ELECT_FACET] = result[PRIMARY_ELECT] ? [result[PRIMARY_ELECT]] + otherElectorates : otherElectorates

        // add other fields to result
        result[OTHER_STATES_LIST] = otherStates
        result[OTHER_ELECTORATES_LIST] = otherElectorates

        // compare inferred values and final values to find if they are the same
        if ((statesInferredFromSites != result[PROJECT_STATE_FACET]) || (electoratesInferredFromSites != result[PROJECT_ELECT_FACET] ))
            result[GEOGRAPHIC_RANGE_OVERRIDDEN] = true
        else
            result[GEOGRAPHIC_RANGE_OVERRIDDEN] = false

        result
    }

    /**
     * Returns a distinct list of hubIds for the supplied projects.
     * @param projects
     * @return
     */
    List findHubIdOfProjects(List projects) {
        Project.createCriteria().listDistinct {
            inList('projectId', projects)
            projections {
                property('hubId')
            }
        }
    }

    /**
     * Find hubs from project or use hubId query parameter
     * @param projects
     * @return
     */
    def findHubIdFromProjectsOrCurrentHub (List projects) {
        if (projects) {
            return findHubIdOfProjects(projects)
        }
        else {
            def currentHub = hubService.getCurrentHub()
            return currentHub ? [currentHub.hubId] : []
        }
    }

}