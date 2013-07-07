package au.org.ala.ecodata

class SiteService {

    static transactional = false
    static final ACTIVE = "active"
    static final BRIEF = 'brief'
    static final RAW = 'raw'

    def grailsApplication, activityService, projectService, commonService

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def get(id, levelOfDetail = []) {
        def o = Site.findBySiteId(id)
        if (!o) { return null }
        def map = [:]
        if (levelOfDetail.contains(RAW)) {
            map = commonService.toBareMap(o)
        } else {
            map = toMap(o, levelOfDetail)
        }
        map
    }

    def findAllForProjectId(id, levelOfDetail = []) {
        Site.findAllByProjects(id).findAll({it.status == ACTIVE}).collect { toMap(it, levelOfDetail) }
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param site a Site instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    def toMap(site, levelOfDetail = []) {
        def dbo = site.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        if (!levelOfDetail.contains(BRIEF)) {
            def projects = projectService.getBrief(mapOfProperties.projects)
            mapOfProperties.projects = projects
            mapOfProperties.activities = activityService.findAllForSiteId(site.siteId, levelOfDetail)
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
        def o = new Site(siteId: Identifiers.getNew(true,''))
        try {
            props.remove('id')
            o.save(failOnError: true)
            //props.activities = props.activities.collect {it.activityId}
            //props.assessments = props.assessments.collect {it.activityId}
            getCommonService().updateProperties(o, props)
            return [status:'ok',siteId:o.siteId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Site.withSession { session -> session.clear() }
            def error = "Error creating site ${props.name} - ${e.message}"
            log.error(error, e)
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
