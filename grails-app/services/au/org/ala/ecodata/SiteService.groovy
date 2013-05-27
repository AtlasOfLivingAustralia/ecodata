package au.org.ala.ecodata

class SiteService {

    static transactional = false

    def grailsApplication, activityService

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def get(id) {
        get(id, false)
    }

    def get(id, rich) {
        def o = Site.findBySiteId(id)
        return o ? (rich ? toRichMap(o): toMap(o)) : null
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param site a Site instance
     * @return map of properties
     */
    def toMap(site) {
        def dbo = site.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.remove("activites")
        mapOfProperties.activities = site.activities.collect {
            def a = [activityId: it.activityId,
                    siteId: it.siteId,
                    type: it.type,
                    startDate: it.startDate,
                    endDate: it.endDate,
                    collector: it.collector,
                    assessment: it.assessment]
            a
        }

        mapOfProperties.findAll {k,v -> v != null}
    }

    /**
     * Converts the domain object into a highly detailed map of properties, including
     * dynamic properties, and linked components.
     * @param prj a Project instance
     * @return map of properties
     */
    def toRichMap(site) {
        def dbo = site.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.remove("activites")
        mapOfProperties.activities = site.activities.collect {
            activityService.get it.activityId
        }

        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props) {
        assert getCommonService()
        def project = Project.findByProjectId(props.projectId)
        if (project) {
            def o = new Site(projectId: project.projectId, siteId: Identifiers.getNew(true,''))
            try {
                getCommonService().updateProperties(o, props)
                project.addToSites(o)
                project.save()
                return [status:'ok',siteId:o.siteId]
            } catch (Exception e) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Site.withSession { session -> session.clear() }
                def error = "Error creating site for project ${props.projectId} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error creating site - no project with id = ${props.projectId}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def a = Site.findBySiteId(id)
        if (a) {
            try {
                getCommonService().updateProperties(a, props)
                return [status:'ok']
            } catch (Exception e) {
                Site.withSession { session -> session.clear() }
                def error = "Error updating site ${id} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error updating site - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

}
