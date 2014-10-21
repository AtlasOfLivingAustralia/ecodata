package au.org.ala.ecodata

class SiteService {

    static transactional = false
    static final ACTIVE = "active"
    static final BRIEF = 'brief'
    static final RAW = 'raw'
    static final FLAT = 'flat'

    def grailsApplication, activityService, projectService, commonService, webService, documentService, metadataService

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    /**
     * Returns all sites in the system in a list.
     * @param includeDeleted true if deleted sites should be returned.
     * @return
     */
    def list(boolean includeDeleted = false) {
        def list = []
        def sites = includeDeleted ? Site.list() : Site.findAllByStatus(ACTIVE)
        sites.each { site ->
            list << toMap(site, [FLAT])
        }
        list.sort {it.name}

        list
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

        if (!levelOfDetail.contains(FLAT) && !levelOfDetail.contains(BRIEF)) {
            mapOfProperties.documents = documentService.findAllForSiteId(site.siteId)
            if (levelOfDetail != LevelOfDetail.NO_ACTIVITIES.name()) {
                def projects = projectService.getBrief(mapOfProperties.projects)
                mapOfProperties.projects = projects
                mapOfProperties.activities = activityService.findAllForSiteId(site.siteId, levelOfDetail)
            }
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
            o.save(failOnError: true)
            assignPOIIds(props)
            props.remove('id')
            o.save(failOnError: true)
            //props.activities = props.activities.collect {it.activityId}
            //props.assessments = props.assessments.collect {it.activityId}
            // If the site location is being updated, refresh the location metadata.
            def centroid = props?.extent?.geometry?.centre
            if (centroid && centroid.size() == 2) {
                props.extent.geometry += metadataService.getLocationMetadataForPoint(centroid[1], centroid[0])
            }
            getCommonService().updateProperties(o, props)
            return [status:'ok',siteId:o.siteId]
        } catch (Exception e) {
            e.printStackTrace()
            // clear session to avoid exception when GORM tries to autoflush the changes
            Site.withSession { session -> session.clear() }
            def error = "Error creating site ${props.name} - ${e.message}"
            log.error(error, e)
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def site = Site.findBySiteId(id)
        if (site) {
            try {
                assignPOIIds(props)

                // If the site location is being updated, refresh the location metadata.
                def centroid = props.extent?.geometry?.centre
                if (centroid && centroid.size() == 2) {
                    props.extent.geometry += metadataService.getLocationMetadataForPoint(centroid[1], centroid[0])
                }
                getCommonService().updateProperties(site, props)
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

    def deleteSitesFromProject(String projectId){
        log.debug("Deleting the sites for projectID : " + projectId)
        //def siteList = siteService.findAllForProjectId(id, siteService.BRIEF)
        def siteList = Site.findAllByProjects(projectId)
        siteList.each { site ->
            site.projects.remove(projectId)
            site.save()
        }
        [status:'ok']
    }

    /**
     * Goes through the POIs assigned to a a site and assigns GUIDs to any new POIs.
     * @param site the site to check the POIs of.
     */
    def assignPOIIds(site) {
        site.poi?.each { poi ->
            if (!poi.poiId) {
                poi.poiId = Identifiers.getNew(true, '')
            }
        }
    }

    /**
     * Creates a point of interest for a site.
     * @param siteId the ID of the site
     * @param props the properties of the POI.
     * @return the ID of the new POI.
     */
    def createPoi(siteId, props) {
        props.poiId = Identifiers.getNew(true, '')
        def site = get(siteId, [FLAT])

        if (!site) {
            return [status:'error', error:"No site with ID ${siteId}"]
        }
        def pois = site.poi ?:[]
        pois.push(props)

        update([poi:pois], siteId)

        return [status:'ok', poiId:props.poiId]
    }

    def removeProject(siteId, projectId){
        log.debug("Removing project $projectId from site $siteId" +
                "")
        def site = Site.findBySiteId(siteId)
        site.projects.remove(projectId)
        site.save()
        [status:'ok']
    }

    def addProject(siteId, projectId){
        def site = Site.findBySiteId(siteId)
        site.projects << projectId
        site.projects.unique()
        site.save()
        [status:'ok']
    }

    def geometryAsGeoJson(site) {
        def geometry = site?.extent?.geometry

        if (!geometry) {
            return
        }
        def result
        switch (geometry.type) {
            case 'Circle':
                // We support circles, but they are not officially supported.
                result = [type:'Point', coordinates:geometry.centre]
                break
            case 'Point':
            case 'point':
                def coords = geometry.coordinates
                // There is a process that is recording the coordinates as strings.
                coords = [coords[0] as Double, coords[1] as Double]
                result = [type:'Point', coordinates:coords]
                break
            case 'Polygon':
                result = [type:'Polygon', coordinates: geometry.coordinates]
                break
            case 'pid':
                result = geometryForPid(geometry.pid)
                break
        }
        result
    }

    def geometryForPid(pid) {
        def url = "${grailsApplication.config.spatial.baseUrl}/ws/shape/geojson/${pid}"
        webService.getJson(url)
    }

}
