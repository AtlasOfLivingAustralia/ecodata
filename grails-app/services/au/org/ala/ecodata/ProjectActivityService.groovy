package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.*

class ProjectActivityService {
    static transactional = false
    static final DOCS = 'docs'
    static final ALL = 'all' // docs and sites

    CommonService commonService
    DocumentService documentService
    SiteService siteService
    ActivityService activityService
    CommentService commentService
    PermissionService permissionService

    /**
     * Creates an project activity.
     *
     * @param props the activity properties
     * @return json status
     */
    Map create(Map props) {
        Map result

        ProjectActivity projectActivity = new ProjectActivity(projectId: props.projectId, projectActivityId: Identifiers.getNew(true, ''))
        try {
            props.remove("projectId");
            props.remove("projectActivityId");

            commonService.updateProperties(projectActivity, props)

            result = [status: 'ok', projectActivityId: projectActivity.projectActivityId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            ProjectActivity.withSession { session -> session.clear() }
            String error = "Error creating project activity for project ${props.projectId} - ${e.message}"
            log.error error
            result = [status: 'error', error: error]
        }

        result
    }

    /**
     * Update project activity.
     *
     * @param props the activity properties
     * @id project activity id.
     * @return json status
     */
    Map update(Map props, String id) {
        Map result

        ProjectActivity projectActivity = ProjectActivity.findByProjectActivityId(id)
        if (projectActivity) {
            try {
                props.remove("projectId");
                props.remove("projectActivityId");

                updateEmbargoDetails(projectActivity, props)

                commonService.updateProperties(projectActivity, props)

                result = [status: 'ok', projectActivityId: projectActivity.projectActivityId]
            } catch (Exception e) {
                ProjectActivity.withSession { session -> session.clear() }
                def error = "Error updating project activity ${id} - ${e.message}"
                log.error error, e
                result = [status: 'error', error: error]
            }
        } else {
            def error = "Error updating project activity - no such id ${id}"
            log.error error
            result = [status: 'error', error: error]
        }

        result
    }

    private static updateEmbargoDetails(ProjectActivity projectActivity, Map incomingProperties) {
        EmbargoOption option = incomingProperties.visibility?.embargoOption as EmbargoOption

        VisibilityConstraint visibility = new VisibilityConstraint()
        switch (option) {
            case EmbargoOption.NONE:
                visibility.embargoOption = EmbargoOption.NONE
                visibility.embargoUntil = null
                visibility.embargoForDays = null
                break
            case EmbargoOption.DAYS:
                visibility.embargoOption = EmbargoOption.DAYS
                visibility.embargoForDays = incomingProperties.visibility?.embargoForDays
                visibility.embargoUntil = EmbargoUtil.calculateEmbargoUntilDate(incomingProperties)
                break
            case EmbargoOption.DATE:
                visibility.embargoOption = EmbargoOption.DATE
                visibility.embargoForDays = null
                visibility.embargoUntil = EmbargoUtil.calculateEmbargoUntilDate(incomingProperties)
                break
        }

        incomingProperties.remove("visibility")
        projectActivity.visibility = visibility

    }

    Map delete(String projectActivityId, boolean destroy = false) {
        Map result
        ProjectActivity projectActivity = ProjectActivity.findByProjectActivityId(projectActivityId)

        if (projectActivity) {
            Activity.findAllByProjectActivityId(projectActivityId).each {
                activityService.delete(it.activityId, destroy)
            }

            documentService.findAllForProjectActivityId(projectActivityId).each {
                documentService.deleteDocument(it.documentId, destroy)
            }

            commentService.deleteAllForEntity(ProjectActivity.class.name, projectActivityId, destroy)

            if (destroy) {
                projectActivity.delete(flush:true)
            } else {
                projectActivity.status = DELETED
                projectActivity.save(flush: true)
            }

            if (projectActivity.hasErrors()) {
                result = [status: 'error', error: projectActivity.getErrors()]
            } else {
                result = [status: 'ok']
            }
        } else {
            result = [status: 'error', error: 'No such id']
        }

        result
    }

    Map get(String id, levelOfDetail = []) {
        ProjectActivity projectActivity = ProjectActivity.findByProjectActivityId(id)
        projectActivity ? toMap(projectActivity, levelOfDetail) : [:]
    }

    List getAllByProject(id, levelOfDetail = []) {
        ProjectActivity.findAllByProjectIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param projectActivity a ProjectActivity instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    Map toMap(projectActivity, levelOfDetail = []) {
        Map mapOfProperties = projectActivity.getProperty("dbo").toMap()

        if (levelOfDetail == DOCS) {
            mapOfProperties["documents"] = documentService.findAllForProjectActivityId(mapOfProperties.projectActivityId)
        } else if (levelOfDetail == ALL) {
            mapOfProperties["documents"] = documentService.findAllForProjectActivityId(mapOfProperties.projectActivityId)

            mapOfProperties["sites"] = mapOfProperties.sites.collect {
                siteService.get(it, "brief")
            }
        }
        String id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")

        mapOfProperties.findAll { k, v -> v != null }
    }

    List<String> listRestrictedProjectActivityIds(String userId = null, String projectId = null) {

        // We need to find the ProjectActivities that ARE restricted (i.e. they are embargoed).
        // We may or may not have a userId.
        // We may or may not have a projectId.
        // If we have a projectId, we only want to find the restricted ProjectActivities that belong to that project.
        // If we do NOT have a projectId, then we want to find ALL restricted ProjectActivities.
        // A ProjectActivity is restricted if:
        // 1. There is no user AND the ProjectActivity is Embargoed
        // 2. There is a user, AND the user is NOT an Admin or Editor for the Project, AND the ProjectActivity is Embargoed

        List<String> restrictedProjectActivityIds = []

        // ALA Admins can do everything, so there are no restricted ProjectActivities for them
        if (!permissionService.isUserAlaAdmin(userId)) {

            // If we know both the user and the project, then check if the user is an admin or editor for the project:
            //  -> if they are, then there are no restricted ProjectActivities for them
            //  -> if they are not an admin or an editor, then return all ProjectActivities for the project where the embargoUntil date is in the future
            if (userId && projectId) {
                boolean userIsProjectMember = permissionService.isUserAdminForProject(userId, projectId) || permissionService.isUserEditorForProject(userId, projectId)

                if (!userIsProjectMember) {
                    restrictedProjectActivityIds = ProjectActivity.withCriteria {
                        eq "projectId", projectId
                        isNotNull "visibility"
                        isNotNull "visibility.embargoUntil"
                        gt "visibility.embargoUntil", new Date()

                        projections {
                            property("projectActivityId")
                        }
                    }
                }
            } else {
                List<String> projectsTheUserIsAMemberOf = userId ? permissionService.getProjectsForUser(userId, AccessLevel.admin, AccessLevel.editor) : null

                restrictedProjectActivityIds = ProjectActivity.withCriteria {
                    if (projectId) {
                        // if we know the project id, then only look at ProjectActivities for that project
                        eq "projectId", projectId
                    } else if (projectsTheUserIsAMemberOf) {
                        // if we do not know the project id, then we need to return ProjectActivities for all projects where the user is NOT a member
                        not { 'in' "projectId", projectsTheUserIsAMemberOf }
                    }

                    // and we only want restricted ProjectActivities, so select only those with a future embargoUntil date
                    isNotNull "visibility"
                    isNotNull "visibility.embargoUntil"
                    ge "visibility.embargoUntil", new Date()

                    projections {
                        property("projectActivityId")
                    }
                }
            }
        }

        restrictedProjectActivityIds
    }
}
