package au.org.ala.ecodata

import grails.transaction.Transactional


class ProjectActivityService {

    static final ACTIVE = "active"
    static final DOCS = 'docs'
    static final ALL = 'all' // docs and sites
    static final BRIEF = 'brief'

    def commonService, documentService, siteService


    /**
     * Creates an project activity.
     *
     * @param props the activity properties
     * @return json status
     */
    def create(props) {
        def o = new ProjectActivity(projectId: props.projectId, projectActivityId: Identifiers.getNew(true,''))
        try {
            props.remove("projectId");
            props.remove("projectActivityId");

            commonService.updateProperties(o, props)
            return [status:'ok',projectActivityId:o.projectActivityId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            ProjectActivity.withSession { session -> session.clear() }
            def error = "Error creating project activity for project ${props.projectId} - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    /**
     * Update project activity.
     *
     * @param props the activity properties
     * @id project activity id.
     * @return json status
     */
    def update(props, id) {
        def o = ProjectActivity.findByProjectActivityId(id)
        if (o) {
            try {
                props.remove("projectId");
                props.remove("projectActivityId");

                commonService.updateProperties(o, props)
                return [status:'ok',projectActivityId:o.projectActivityId]

            } catch (Exception e) {
                ProjectActivity.withSession { session -> session.clear() }
                def error = "Error updating project activity ${id} - ${e.message}"
                log.error error
                return [status: 'error', error: error]
            }
        } else {
            def error = "Error updating project activity - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def get(String id, levelOfDetail = []) {
        def p = ProjectActivity.findByProjectActivityId(id)
        return p?toMap(p, levelOfDetail):null
    }

    def getAllByProject(id, levelOfDetail = []){
        ProjectActivity.findAllByProjectId(id).findAll({it.status == ACTIVE}).collect { toMap(it, levelOfDetail) };
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param projectActivity a ProjectActivity instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    def toMap(site, levelOfDetail = []) {
        def dbo = site.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        if (levelOfDetail == DOCS) {
            mapOfProperties["documents"] = documentService.findAllForProjectActivityId(mapOfProperties.projectActivityId)
        }else if (levelOfDetail == ALL){
            mapOfProperties["documents"] = documentService.findAllForProjectActivityId(mapOfProperties.projectActivityId)
            def siteIds = mapOfProperties.sites
            def sites = []
            siteIds?.each{
                sites << siteService.get(it, 'brief')
            }
            mapOfProperties["sites"] = sites
        }
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.findAll {k,v -> v != null}
    }


}
