package au.org.ala.ecodata

import com.vividsolutions.jts.geom.Geometry
import grails.converters.JSON
import org.geotools.geojson.GeoJSON
import org.geotools.geojson.geom.GeometryJSON

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
        def site = new Site(siteId: Identifiers.getNew(true,''))
        try {
            site.save(failOnError: true)
            updateSite(site, props, true)

            return [status:'ok',siteId:site.siteId]
        } catch (Exception e) {
            e.printStackTrace()
            // clear session to avoid exception when GORM tries to autoflush the changes
            Site.withSession { session -> session.clear() }
            def error = "Error creating site ${props.name} - ${e.message}"
            log.error(error, e)
            return [status:'error',error:error]
        }
    }

    def update(props, id, boolean enableCentroidRefresh = true) {
        def site = Site.findBySiteId(id)

        if (site) {
            try {
                updateSite(site, props, enableCentroidRefresh)
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

    private updateSite(site, props, enableCentroidRefresh) {
        props.remove('id')
        props.remove('siteId')

        assignPOIIds(props)

        // If the site location is being updated, refresh the location metadata.
        def centroid = props.extent?.geometry?.centre
        if (centroid && centroid.size() == 2 && enableCentroidRefresh) {
            props.extent.geometry += metadataService.getLocationMetadataForPoint(centroid[1], centroid[0])
        }
        else if (props.extent?.geometry && !centroid) { // Sites created from known shapes need a centroid to be calculated.
            populateLocationMetadataForSite(props)
        }
        getCommonService().updateProperties(site, props)
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
            log.error("Invalid site: ${site.siteId} missing geometry")
            return
        }
        def result = null
        switch (geometry.type) {
            case 'Circle':
                // We support circles, but they are not valid geojson. The spatial portal does a conversion for us.
                if (geometry.pid) {
                    result = geometryForPid(geometry.pid)
                }
                else {
                    result = [type: 'Point', coordinates: geometry.centre]
                }
                break
            case 'Point':
            case 'point':
                def coords = geometry.coordinates
                // There is a process that is recording the coordinates as strings.
                coords = [coords[0] as Double, coords[1] as Double]
                result = [type:'Point', coordinates:coords]
                break
            case 'Polygon':
                if (!geometry.coordinates) {
                    log.error("Invalid site: ${site.siteId} missing coordinates")
                    return
                }
                // The map drawing tools allow you to draw lines using the "polygon" tool.
                def type = geometry.coordinates.size() < 4 ? 'LineString' : 'Polygon'
                result = [type:type, coordinates: geometry.coordinates]
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

    def populateLocationMetadataForSite(site) {
        def centroid = site?.extent?.geometry?.centre
        if (!centroid || centroid.size() != 2) {
            def siteGeom = geometryAsGeoJson(site)
            if (siteGeom) {
                GeometryJSON gjson = new GeometryJSON()
                Geometry geom = gjson.read((siteGeom as JSON).toString())
                if (!site.extent) {
                    site.extent = [geometry:[:]]
                }
                if (!site.extent.geometry) {
                    site.extent.geometry = [:]
                }
                if (geom) {
                    centroid = [Double.toString(geom.centroid.x), Double.toString(geom.centroid.y)]
                    site.extent.geometry.centre = centroid
                }
                else {
                    log.error("No geometry for site: ${site.siteId}")
                }
            }

        }
        if (centroid) {
            site.extent.geometry += metadataService.getLocationMetadataForPoint(centroid[1], centroid[0])
        }
        site
    }

}
