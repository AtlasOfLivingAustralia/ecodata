package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.*

import au.org.ala.ecodata.reporting.Score

class ProjectService {

    static transactional = false

    static final BRIEF = 'brief'
    static final FLAT = 'flat'
    static final ALL = 'all'
	static final PROMO = 'promo'
    static final OUTPUT_SUMMARY = 'outputs'
    static final ENHANCED = 'enhanced'

    def grailsApplication
    SiteService siteService
    DocumentService documentService
    MetadataService metadataService
    ReportService reportService
    ActivityService activityService
    ProjectActivityService projectActivityService
    PermissionService permissionService
    CollectoryService collectoryService
    WebService webService
    OrganisationService organisationService

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def getBrief(listOfIds) {
        Project.findAllByProjectIdInListAndStatusNotEqual(listOfIds, DELETED).collect {
            [projectId: it.projectId, name: it.name]
        }
    }

    def get(String id, levelOfDetail = []) {
        def p = Project.findByProjectId(id)

        return p?toMap(p, levelOfDetail):null
    }

    def list(levelOfDetail = [], includeDeleted = false, citizenScienceOnly = false) {
        def list
        if (!citizenScienceOnly)
            list = includeDeleted ? Project.list(): Project.findAllByStatus(ACTIVE)
        else if (includeDeleted)
            list = Project.findAllByIsCitizenScience(true)
        else
            list = Project.findAllByIsCitizenScienceAndStatus(true, ACTIVE)
        list?.collect { toMap(it, levelOfDetail) }
    }

    def listMeritProjects (levelOfDetail = [], includeDeleted = false){
        def list = []

        if (includeDeleted) {
            list = Project.findAllByIsMERIT(true)
        } else {
            list = Project.findAllByIsMERITAndStatus(true, ACTIVE)
        }
        list.collect { toMap(it, levelOfDetail) }
    }
	
	def promoted(){
		def list = Project.findAllByPromoteOnHomepage("yes")
		list.collect { toMap(it, PROMO) }
	}
	
    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param prj a Project instance
     * @return map of properties
     */
    Map toMap(Project project, levelOfDetail = [], includeDeletedActivities = false) {
        Map result

        Map mapOfProperties = project.getProperty("dbo").toMap()

        if (levelOfDetail == BRIEF) {
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
                mapOfProperties.sites = siteService.findAllForProjectId(project.projectId, [SiteService.FLAT])
                mapOfProperties.documents = documentService.findAllForProjectId(project.projectId, levelOfDetail)
                mapOfProperties.links = documentService.findAllLinksForProjectId(project.projectId, levelOfDetail)

                if (levelOfDetail == ALL) {
                    mapOfProperties.activities = activityService.findAllForProjectId(project.projectId, levelOfDetail, includeDeletedActivities)
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

            // look up current associated organisation details
            result.associatedOrgs?.each {
                if (it.organisationId) {
                    Organisation org = Organisation.findByOrganisationId(it.organisationId)
                    it.name = org.name
                    it.url = org.url
                    it.logo = Document.findByOrganisationIdAndRoleAndStatus(it.organisationId, "logo", ACTIVE)?.thumbnailUrl
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
        def dbo = prj.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
		mapOfProperties["status"] = mapOfProperties["status"]?.capitalize();
        mapOfProperties.remove("_id")
        mapOfProperties.remove("sites")
        mapOfProperties.sites = siteService.findAllForProjectId(prj.projectId, true)
        // remove nulls
        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props) {
        assert getCommonService()
        try {
            if (props.projectId && Project.findByProjectId(props.projectId)) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Project.withSession { session -> session.clear() }
                return [status:'error',error:'Duplicate project id for create ' + props.projectId]
            }
            // name is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            def project = new Project(projectId: props.projectId?: Identifiers.getNew(true,''), name:props.name)
            project.save(failOnError: true)

            props.remove('sites')
            props.remove('id')
            props << collectoryService.createDataProviderAndResource(project.projectId, props)
            getCommonService().updateProperties(project, props)
            return [status: 'ok', projectId: project.projectId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Project.withSession { session -> session.clear() }
            def error = "Error creating project - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def a = Project.findByProjectId(id)
        if (a) {
            try {
                getCommonService().updateProperties(a, props)
                if (a.dataProviderId)
                    collectoryService.updateDataProviderAndResource(get(id, FLAT))
                return [status: 'ok']
            } catch (Exception e) {
                Project.withSession { session -> session.clear() }
                def error = "Error updating project ${id} - ${e.message}"
                log.error error
                return [status: 'error', error: error]
            }
        } else {
            def error = "Error updating project - no such id ${id}"
            log.error error
            return [status:'error',error:error]
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
     * @return a Map containing the aggregated results.  TODO document me better, but it is likely this structure will change.
     *
     */
    def projectMetrics(String id, targetsOnly = false, approvedOnly = false) {
        def p = Project.findByProjectId(id)
        if (p) {
            def project = toMap(p, ProjectService.FLAT)

            def toAggregate = []

            metadataService.activitiesModel().outputs?.each{
                Score.outputScores(it).each { score ->
                    if (!targetsOnly || score.isOutputTarget) {
                        toAggregate << [score: score]
                    }
                }
            }
			
            def outputSummary = reportService.projectSummary(id, toAggregate, approvedOnly)
			

            // Add project output target information where it exists.

            project.outputTargets?.each { target ->
                // Outcome targets are text only and not mapped to a score.
                if (target.outcomeTarget) {
                    return
                }
                def score = outputSummary.find{it.score.isOutputTarget && it.score.outputName == target.outputLabel && it.score.label == target.scoreLabel}
                if (score) {
                    score['target'] = target.target
                } else {
               		   // If there are no Outputs recorded containing the score, the results won't be returned, so add
               			// one in containing the target.
                    score = toAggregate.find{it.score?.outputName == target.outputLabel && it.score?.label == target.scoreLabel}
                    if (score) {
                        outputSummary << [score:score.score, target:target.target]
                    } else {
                        // This can happen if the meta-model is changed after targets have already been defined for a project.
                        // Once the project output targets are re-edited and saved, the old targets will be deleted.
                        log.warn "Can't find a score for existing output target: $target.outputLabel $target.scoreLabel, projectId: $project.projectId"
                    }
                }
            }
            return outputSummary
        } else {
            def error = "Error retrieving metrics for project - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
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
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a list of the projects that match the supplied criteria
     */
    List<Map> search(Map searchCriteria, levelOfDetail = []) {

        def criteria = Project.createCriteria()
        def projects = criteria.list {
            ne("status", DELETED)
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }

        }
        projects.collect{toMap(it, levelOfDetail)}
    }

    def updateOrgName(orgId, orgName) {
        Project.collection.update(
            [organisationId: orgId],
            ['$set': [organisationName: orgName]], false, true)
    }

}
