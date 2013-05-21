package au.org.ala.ecodata

class ProjectService {

    static transactional = false

    def grailsApplication

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param prj a Project instance
     * @return map of properties
     */
    def toMap(prj) {
        def dbo = prj.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.remove("sites")
        mapOfProperties.sites = prj.sites.collect {
            def s = [siteId: it.siteId, name: it.name, location: it.location]
            s
        }
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
        def o = new Project(projectId: Identifiers.getNew(true,''))
        try {
            getCommonService().updateProperties(o, props)
            return [status:'ok',projectId:o.projectId]
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
                return [status:'ok']
            } catch (Exception e) {
                Project.withSession { session -> session.clear() }
                def error = "Error updating project ${id} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error updating project - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }
}
