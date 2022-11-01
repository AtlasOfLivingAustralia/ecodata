package au.org.ala.ecodata

import au.org.ala.ecodata.converter.SciStarterConverter
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.codehaus.jackson.map.ObjectMapper
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

    def grailsApplication
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

        [total: list?.totalCount, list: list?.collect { toMap(it, "basic") }]
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

                mapOfProperties.documents = documentService.findAllForProjectId(project.projectId, levelOfDetail, version)
                mapOfProperties.links = documentService.findAllLinksForProjectId(project.projectId, levelOfDetail, version)

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
                        it.name = org.name
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
        List embeddedPropertyNames = ['associatedOrgs', 'externalIds', 'geographicInfo']
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


        if (!project.isExternal && Boolean.valueOf(grailsApplication.config.collectory.collectoryIntegrationEnabled)) {

            Map projectProps = toMap(project, FLAT)
            task {
                collectoryService.updateDataResource(projectProps, props)
            }.onComplete {
                log.info("Completed task to link project with collectory - ${project.name} (id = ${project.projectId})")
            }.onError { Throwable error ->
                if (error instanceof UndeclaredThrowableException) {
                    error = error.undeclaredThrowable
                }
                String message = "Failed to update collectory link for project ${project.name} (id = ${project.projectId})"
                log.error(message, error)
                emailService.sendEmail(message, "Error: ${error.message}", [grailsApplication.config.ecodata.support.email.address])
            }
        }
    }

    def update(Map props, String id, Boolean shouldUpdateCollectory = true) {
        Project project = Project.findByProjectId(id)
        if (project) {
            // retrieve any project activities associated with the project
            List projectActivities = projectActivityService.getAllByProject(id)
            props = includeProjectFundings(props)
            props = includeProjectActivities(props, projectActivities)

            try {
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
                webService.doDelete(grailsApplication.config.collectory.baseURL + 'ws/dataProvider/' + id)
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

                if (value instanceof List) {
                    inList(prop, value)
                } else {
                    eq(prop, value)
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
    void updateOrganisationName(orgId, orgName) {
        Project.findAllByOrganisationIdAndStatusNotEqual(orgId, DELETED).each { project ->
            project.organisationName = orgName
            project.save()
        }
    }

    /**
     * Import SciStarter projects to Biocollect. Import script does the following.
     * 1. gets the list of projects and contacts SciStarter for more details on a project
     * 2. checks if the project is already imported, if yes, update fields. TODO
     * 3. if project does not exist, create a new project, organisation, project extent and project logo document.
     *      And link artifacts to the project. TODO: creating project extent.
     * @return
     */
    Integer importProjectsFromSciStarter() {
        int ignoredProjects = 0, createdProjects = 0, updatedProjects = 0
        log.info("Starting SciStarter import")
        try {
            JsonSlurper jsonSlurper = new JsonSlurper()
            String sciStarterProjectUrl
            // list all SciStarter projects
            List projects = getSciStarterProjectsFromFinder()
            projects?.eachWithIndex { pProperties, index ->
                Map transformedProject
                Map project = pProperties
                if (project && project.title && project.id) {
                    Project importedSciStarterProject = Project.findByExternalIdAndIsSciStarter(project.id?.toString(), true)
                    // get more details about the project
                    try {
                        sciStarterProjectUrl = "${grailsApplication.config.scistarter.baseUrl}${grailsApplication.config.scistarter.projectUrl}/${project.id}?key=${grailsApplication.config.scistarter.apiKey}"
                        String text = webService.get(sciStarterProjectUrl, false);
                        if (text instanceof String) {
                            Map projectDetails = jsonSlurper.parseText(text)
                            if (projectDetails.origin && projectDetails.origin == 'atlasoflivingaustralia') {
                                // ignore projects SciStarter imported from Biocollect
                                log.warn("Ignoring ${projectDetails.title} - ${projectDetails.id} - This is an ALA project.")
                                ignoredProjects++
                            } else {
                                projectDetails << project
                                // map properties from SciStarter to Biocollect
                                transformedProject = SciStarterConverter.convert(projectDetails)
                                if (!importedSciStarterProject) {
                                    // create project & document & site & organisation
                                    createSciStarterProject(transformedProject, projectDetails)
                                    createdProjects++
                                } else {
                                    // update a project just in case something has changed.
                                    updateSciStarterProject(transformedProject, importedSciStarterProject)
                                    log.info("Updating ${importedSciStarterProject.name} ${importedSciStarterProject.projectId}.")
                                    updatedProjects++
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing project - ${sciStarterProjectUrl}. Ignoring it. ${e.message}", e);
                        ignoredProjects++
                    }
                }
            }

            log.info("Number of created projects ${createdProjects}. Number of ignored projects ${ignoredProjects}. Number of projects updated ${updatedProjects}.")
        } catch (SocketTimeoutException ste) {
            log.error(ste.message, ste)
        } catch (Exception e) {
            log.error(e.message, e)
        }

        log.info("Completed SciStarter import")
        createdProjects
    }

    /**
     * Get the entire project list from SciStarter
     * @return
     * @throws SocketTimeoutException
     * @throws Exception
     */
    List getSciStarterProjectsFromFinder() throws SocketTimeoutException, Exception {
        String scistarterFinderUrl = "${grailsApplication.config.scistarter.baseUrl}${grailsApplication.config.scistarter.finderUrl}?format=json&q="
        String responseText = webService.get(scistarterFinderUrl, false)
        if (responseText instanceof String) {
            ObjectMapper mapper = new ObjectMapper()
            Map response = mapper.readValue(responseText, Map.class)
            return response.results
        }
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
        if (project.regions?.size()) {
            // convert region to site
            project.regions.each { region ->
                Map site = SciStarterConverter.siteMapping(region)
                // only add valid geojson objects
                if (site?.extent?.geometry && siteService.isGeoJsonValid((site?.extent?.geometry as JSON).toString())) {
                    Map createdSite = siteService.create(site)
                    if (createdSite.siteId) {
                        sites.push(createdSite.siteId)
                    }
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

}