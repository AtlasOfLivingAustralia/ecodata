package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.*

class ProjectActivityService {

    static final DOCS = 'docs'
    static final ALL = 'all' // docs and sites

    CommonService commonService
    DocumentService documentService
    SiteService siteService
    ActivityService activityService
    CommentService commentService

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

                commonService.updateProperties(projectActivity, props)

                updateEmbargoedRecords(projectActivity)

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
                projectActivity.delete()
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
        projectActivity ? toMap(projectActivity, levelOfDetail) : null
    }

    List getAllByProject(id, levelOfDetail = []) {
        ProjectActivity.findAllByProjectId(id).findAll({ it.status == ACTIVE })
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

    void updateEmbargoedRecords(ProjectActivity projectActivity) {
        List<Record> records = Record.findAllByProjectActivityId(projectActivity.projectActivityId)

        Date embargoUntil = EmbargoUtil.calculateEmbargoUntilDate(projectActivity)

        records.each {
            it.embargoUntil = embargoUntil
            it.save(flush: true)
        }
    }

}
