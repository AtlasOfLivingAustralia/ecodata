package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.model.Filters
import grails.converters.JSON
import org.bson.conversions.Bson
import org.geotools.geojson.geom.GeometryJSON
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.web.json.JSONObject
import org.locationtech.jts.geom.Geometry

import static au.org.ala.ecodata.Status.DELETED
import static grails.async.Promises.task

class SiteService {

    static transactional = false
    static final ACTIVE = "active"
    static final BRIEF = 'brief'
    static final RAW = 'raw'
    static final FLAT = 'flat'
    static final PRIVATE = 'private'
    static final INDEXING = 'indexing'
    static final PLANNING_SITE = 'Planning site'
    static final EMSA_SITE = 'EMSA site'
    static final REPORTING_SITE = 'Reporting site'
    static final INTERSECTION_CURRENT = 'CURRENT'
    static List idTypesByMonitor

    def grailsApplication, activityService, projectService, commonService, webService, documentService, metadataService, cacheService
    PermissionService permissionService
    ProjectActivityService projectActivityService
    SpatialService spatialService
    ElasticSearchService elasticSearchService

    /**
     * Get list of monitor external ids.
     * @return
     */
    static List getMonitorIdTypes() {
        if (!idTypesByMonitor) {
            List monitorIdTypes = []
            ExternalId.IdType.values().toList().each {
                if (it.name().startsWith('MONITOR_')) {
                    monitorIdTypes << it
                    monitorIdTypes << it.name()
                }
            }

            idTypesByMonitor = monitorIdTypes
        }

        idTypesByMonitor
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

    def get(id, levelOfDetail = [], version = null) {
        if (version) {
            def all = AuditMessage.findAllByEntityIdAndEntityTypeAndDateLessThanEquals(id, Site.class.name,
                    new Date(version as Long), [sort:'date', order:'desc', max: 1])
            def site = [:]
            all?.each {
                if (it.entity.status == ACTIVE &&
                        (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                    site = toMap(it.entity, levelOfDetail, version)
                }
            }

            site
        } else {
            def o = Site.findBySiteId(id)
            if (!o) {
                return null
            }
            def map = [:]
            if (levelOfDetail.contains(RAW)) {
                map = commonService.toBareMap(o)
                // Treat the default externalId as the externalId property for backwards
                // compatibility with the MERIT and BioCollect UI
                map['externalId'] = o.externalId
            } else {
                map = toMap(o, levelOfDetail)
            }
            map
        }
    }

    def findAllForProjectId(id, levelOfDetail = [], version = null) {
        if (version) {
            def all = AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(id, Site.class.name,
                    new Date(version as Long), [sort: 'date', order: 'desc'])
            def sites = []
            def found = []
            all?.each {
                if (!found.contains(it.entity.siteId)) {
                    found << it.entity.siteId
                    if (it.entity.status == ACTIVE &&
                            (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        sites << toMap(it.entity, [SiteService.FLAT], version)
                    }
                }
            }
        } else {
            Site.findAllByProjects(id).findAll({ it.status == ACTIVE }).collect { toMap(it, levelOfDetail) }
        }
    }

    /**
     * Returns a list of siteIds associated with a project.  This is used by the AuditService to avoid
     * querying and mapping a full site as they can be very large sometimes and only the id is needed.
     * @param projectId the project id of interest
     * @return a List<String> of sitesIds
     */
    List<String> findAllSiteIdsForProject(String projectId) {
        Site.createCriteria().list {
            eq ('projects', projectId)
            projections {
                property('siteId')
            }
        }
    }

    List<Site> sitesForProject(String projectId) {
        Site.findAllByProjectsAndStatusNotEqual(projectId, DELETED)
    }

    List<Site> sitesForProjectWithTypes(String project, List<String> types) {
        Site.findAllByProjectsAndTypeInListAndStatusNotEqual(project, types, DELETED)
    }

    boolean doesProjectHaveSite(id){
        Site.findAllByProjects(id)?.size() > 0
    }

    def findAllNonPrivateSitesForProjectId(id, levelOfDetail = []){
        Site.withCriteria {
            eq('status', ACTIVE)
            ne('visibility', PRIVATE)
            inList('projects', [id])
        }.collect { toMap(it, levelOfDetail) }
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param site a Site instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    def toMap(site, levelOfDetail = [], version = null) {
        def mapOfProperties = site instanceof Site ? GormMongoUtil.extractDboProperties(site.getProperty("dbo")) : site
        // Treat the default externalId as the externalId property for backwards
        // compatibility with the MERIT and BioCollect UI
        if (site instanceof Site) {
            mapOfProperties['externalId'] = site.externalId
        }
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")

        if (!levelOfDetail.contains(FLAT) && !levelOfDetail.contains(BRIEF)) {
            mapOfProperties.documents = documentService.findAllForSiteId(site.siteId, version)
            if (levelOfDetail.contains(LevelOfDetail.PROJECTS.name())) {
                def projects = projectService.getBrief(mapOfProperties.projects, version)
                mapOfProperties.projects = projects
            } else if (!levelOfDetail.contains(LevelOfDetail.NO_ACTIVITIES.name())) {
                def projects = projectService.getBrief(mapOfProperties.projects, version)
                mapOfProperties.projects = projects
                mapOfProperties.activities = activityService.findAllForSiteId(site.siteId, levelOfDetail, version)
            }
        }

        if (levelOfDetail.contains(INDEXING)) {
            mapOfProperties.geometryType = site.geometryType
            mapOfProperties.geoPoint = site.geoPoint
        }

        mapOfProperties.findAll {k,v -> v != null}
    }

    Map toGeoJson(Map site) {

        Map properties = [
                id:site.siteId,
                name:site.name,
                type:site.type,
                notes:site.notes,
        ]
        Map geojson

        if (site.features) {
            geojson = [
                    type:'FeatureCollection',
                    properties: properties,
                    features: site.features
            ]
        }
        else {
            geojson = [
                    type:"Feature",
                    properties:properties,
                    geometry: geometryAsGeoJson(site)
            ]
        }
        geojson
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    /**
     * Knows to to create an object suitable for the create/update methods from geojson.
     * FeatureCollections are not supported.
     * This is due to historical issues of how sites are represented in ecodata
     */
    static Map propertiesFromGeoJson(Map geoJson, String source) {
        Map properties = [:]
        Map geometry = geoJson

        if (geoJson.type == "Feature") {
            properties = geoJson.properties
            geometry = geoJson.geometry
        }
        Map site = [name:properties.name, description:properties.description, notes:properties.notes]
        site.extent = [geometry:geometry, source:source]

        if (geometry.type == 'Point') {
            site.extent.source = 'point'  // Can't display points unless source = 'point'
            site.extent.geometry.decimalLatitude = geometry.coordinates[1] // Some views still rely on these
            site.extent.geometry.decimalLongitude = geometry.coordinates[0]
        }

        site
    }

    def getSimpleProjectArea(projectSiteId) {
        def threshold = grailsApplication.config.getProperty('biocollect.projectArea.simplificationThreshold', Integer, 10000)
        def tolerance = grailsApplication.config.getProperty('biocollect.projectArea.simplificationTolerance', Double, 0.0001)

        def site = get(projectSiteId, [SiteService.FLAT, SiteService.INDEXING])

        try {
            if (site != null) {
                def projectArea = geometryAsGeoJson(site)

                if (projectArea?.coordinates != null) {
                    def coordsSize = projectArea.coordinates.flatten().size()
                    if (coordsSize > threshold) {
                        site.geoIndex = GeometryUtils.simplify(projectArea, tolerance)
                    } else {
                        site.geoIndex = projectArea
                    }
                }
            }
        } catch (Exception e) {
            log.info("Unable to get simplified project area geometry (site ${site.siteId})", e)
        }

        // remove extent to not avoid total fields limit in ES
        site?.remove('extent')
        site
    }

    def create(props) {
      //  assert getCommonService()
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

    def update(Map props, String id, boolean enableCentroidRefresh = true) {
        Site site = Site.findBySiteId(id)

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

    private updateSite(Site site, Map props, boolean forceRefresh = false) {
        List fids = metadataService.getSpatialLayerIdsToIntersectForProjects(site.projects)
        props.remove('id')
        props.remove('siteId')
        // Used by BioCollect to improve the performance of site creation
        def asyncUpdate = props['asyncUpdate']?props['asyncUpdate']:false
        props.remove('asyncUpdate')

        assignPOIIds(props)
        // If the site location is being updated, refresh the location metadata.
        if (forceRefresh || hasGeometryChanged(toMap(site), props)) {
            if (asyncUpdate){
                // Sharing props object between thread causes ConcurrentModificationException.
                // Cloned object is used by spawned thread.
                // https://github.com/AtlasOfLivingAustralia/ecodata/issues/594
                Map clonedProps = new JSONObject(props)
                String userId = props.remove('userId')
                String siteId = site.siteId
                task {
                    Site.withNewSession { Session session ->
                        Site createdSite = Site.findBySiteId(siteId)
                        addSpatialPortalPID(clonedProps, userId)
                        populateLocationMetadataForSite(clonedProps, fids)
                        commonService.updateProperties(createdSite, clonedProps)
                    }
                }
            }
            else {
                populateLocationMetadataForSite(props, fids)
            }
        }

        // The commonService won't update externalId because it's a transient property
        if (props.externalId) {
            site.externalId = props.externalId
        }
        commonService.updateProperties(site, props)

    }


    /** Recomputing geographic facets, centroid and area can be expensive so we only want to do it if we have to */
    private boolean hasGeometryChanged(Map site, Map newProps) {
        if (!newProps.extent?.geometry) {
            return false
        }

        return (site.extent?.source != newProps.extent.source) ||
                (site.extent?.geometry?.coordinates != newProps.extent.geometry.coordinates) ||
                (site.extent?.geometry?.pid != newProps.extent.geometry.pid)
    }

    void delete(String siteId, boolean destroy = false) {
        if (!destroy) {
            update([status:DELETED], siteId, false)
        }
        else {
            Site site = Site.findBySiteId(siteId)
            site.delete()

            // TODO
            // delete from spatial portal.  Right now this is never used.
        }

        documentService.deleteAllForSite(siteId, destroy)
    }

    /**
     * Breaks the association between a site and a project, optionally deleting the site if it is not longer
     * used.  Note that a site will not be removed from the project if it has been associated with any project
     * activities.
     *
     * @param projectId the project to remove the site from.
     * @param siteIds a List of site ids to remove from the project.
     * @param deleteOrphans set to true to (soft) delete the site if it is no longer associated with any projects
     * or activities.
     * @return a Map containing
     */
    Map deleteSitesFromProject(String projectId, List siteIds = null, boolean deleteOrphans = false){
        log.debug("Deleting the sites for projectID : " + projectId)
        List siteList
        List warnings = []
        if (siteIds) {
            siteList = Site.findAllBySiteIdInListAndProjectsAndStatusNotEqual(siteIds, projectId, DELETED)
        }
        else {
            siteList = Site.findAllByProjectsAndStatusNotEqual(projectId, DELETED)
        }

        siteList.each { site ->
            if (canRemoveProject(site, projectId)) {
                site.projects.remove(projectId)
                site.save()

                IndexDocMsg message = new IndexDocMsg(docType: Project.class.name, docId: projectId, indexType: EventType.PostUpdate, docIds: [])
                elasticSearchService.queueIndexingEvent(message)

                if (deleteOrphans && canDelete(site)) {
                    if (deleteOrphans) {
                        delete(site.siteId)
                    }
                }
            }
            else {
                warnings << ["Cannot remove ${site.siteId} it is used by project activities"]
            }
        }
        [status:'ok', warnings:warnings]
    }

    boolean canRemoveProject(Site site, String projectId) {
        if (Activity.findAllBySiteIdAndProjectIdAndStatusNotEqual(site.siteId, projectId, DELETED)?.size()) {
            return false
        }
        return !ProjectActivity.findAllBySitesAndProjectIdAndStatusNotEqual(site.siteId, projectId, DELETED)?.size()
    }

    /**
     * Returns true if the site is not associated with any activities, projectActivities or projects.
     * We allow the site to be associated with documents, any documents will be deleted with the site.
     * @param site the Site to check.
     * @return true if this site can be deleted.
     */
    boolean canDelete(Site site) {
        if (site.projects?.size()) {
            return false
        }
        if (activityService.findAllForSiteId(site.siteId)?.size()) {
            return false
        }
        return !ProjectActivity.findAllBySitesAndStatusNotEqual(site.siteId, DELETED)?.size()
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

    def deletePoi(siteId, poiId) {
        def site = get(siteId, [FLAT])

        if (!site) {
            return [status:'error', error:"No site with ID ${siteId}"]
        }
        boolean removed = site.poi?.removeAll{it.poiId == poiId}
        if (!removed) {
            return [status:'error', error:"No POI exists with id: ${poiId}"]
        }

        update([poi:site.poi], siteId)
        return [status:'deleted', poiId:poiId]
    }

    def updatePoi(siteId, props) {
        if (!props.poiId) {
            return createPoi(siteId, props)
        }
        def site = get(siteId, [FLAT])

        if (!site) {
            return [status:'error', error:"No site with ID ${siteId}"]
        }
        Map poi = site.poi?.find{it.poiId == props.poiId}
        if (!poi) {
            return [status:'error', error:"No POI exists with poiId=${props.poiId}"]
        }
        poi.putAll(props)

        update([poi:site.poi], siteId)
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
        if(site){
            site.projects << projectId
            site.projects.unique()
            site.save()
            [status:'ok']
        } else {
            log.error("Could not find site - ${siteId}")
            [status:'site not found']
        }
    }

    Map geometryAsGeoJson(site) {
        def geometry = site?.extent?.geometry

        if (!geometry) {
            log.error("Invalid site: ${site.siteId} missing geometry")
            return null
        }
        def result = null
        switch (geometry.type) {
            case 'Circle':
                // We support circles, but they are not valid geojson.
                Geometry geom = GeometryUtils.geometryForCircle(geometry.coordinates[1], geometry.coordinates[0], geometry.radius)
                result = [type:'Polygon', coordinates: [Arrays.asList(geom.coordinates).collect{[it.x, it.y]}]]

                break
            case 'Point':
            case 'point':
                def coords = geometry.coordinates
                // There is a process that is recording the coordinates as strings.
                coords = [coords[0] as Double, coords[1] as Double]
                result = [type:'Point', coordinates:coords]
                break
            case 'MultiPoint':
                if (!geometry.coordinates) {
                    log.error("Invalid site: ${site.siteId} missing coordinates")
                    return
                }
                result = [type:'MultiPoint', coordinates: geometry.coordinates]
                break
            case 'Polygon':
                if (!geometry.coordinates) {
                    log.error("Invalid site: ${site.siteId} missing coordinates")
                    return
                }

                geometry.coordinates = removeDuplicatesFromCoordinates(geometry.coordinates)
                if(!isValidPolygon(geometry.coordinates)){
                    // The map drawing tools allow you to draw lines using the "polygon" tool.
                    def coordinateLength = geometry.coordinates.size()
                    if (coordinateLength == 1 && geometry.coordinates[0] instanceof List) {
                        def type = geometry.coordinates[0].size() < 4 ? 'MultiLineString' : 'MultiPolygon'
                        result = [type:type, coordinates: [geometry.coordinates]]
                    }
                    else {
                        def type = coordinateLength < 4 ? 'LineString' : 'Polygon'
                        result = [type: type, coordinates: [geometry.coordinates]]
                    }
                } else {
                    result =  [type:geometry.type, coordinates: geometry.coordinates]
                }
                break
            case 'LineString':
            case 'MultiPolygon':
            case 'MultiLineString':
                if (!geometry.coordinates) {
                    log.error("Invalid site: ${site.siteId} missing coordinates")
                    return
                }

                geometry.coordinates = removeDuplicatesFromCoordinates(geometry.coordinates)
                result =  [type:geometry.type, coordinates: geometry.coordinates]
                break
            case 'pid':
                result = geometryForPid(geometry.pid)
                // Spatial portal now returns results as Features.
                if (result?.type == 'Feature') {
                    result = result.geometry
                }
                break
        }
        result
    }

    Boolean isValidPolygon (List coordinates){
        Boolean valid = false
        Integer depth = 0
        def coord = coordinates

        while( coord instanceof List){
            depth ++;
            coord = coord[0]
        }

        if(depth == 3){
            valid = true
        }

        valid
    }

    /**
     * Removes consecutive duplicate coordinates. Elasticsearch throws exception.
     * @param coordinates
     * @return
     */
    List removeDuplicatesFromCoordinates(List coordinates){
        if(!(coordinates instanceof List && coordinates[0] instanceof List && (coordinates[0][0] instanceof List || coordinates[0][0]?.toString()?.isNumber()))){
            return coordinates
        }

        if((coordinates instanceof List) && ( coordinates[0] instanceof List)  && !(coordinates[0][0] instanceof List)){
            return removeDuplicatePoint(coordinates)
        } else {
            for (int i = 0; i < coordinates.size(); i++) {
                coordinates[i] = removeDuplicatesFromCoordinates(coordinates[i])
            }
        }

        coordinates
    }

    List removeDuplicatePoint(List points){
        List vettedCoordinates = []
        List previousPoint

        points?.each { List point ->
            if(!point.equals(previousPoint)){
                vettedCoordinates.add(point)
            } else if(previousPoint == null){
                vettedCoordinates.add(point)
            } else {
                log.debug("Duplicate points identified - ${point}")
            }

            previousPoint = point
        }

        vettedCoordinates
    }

    def geometryForPid(pid) {
        // getting wkt since spatial portal geojson api is not returning all precision points of lat and lng.
        def url = "${grailsApplication.config.getProperty('spatial.baseUrl')}${spatialService.WKT_SHAPE_URL_PREFIX}${pid}"
        def wkt = webService.get(url, false)
        if (wkt instanceof String) {
            return GeometryUtils.wktToGeoJson(wkt)
        }
        else {
            log.error("No geometry for pid: ${pid}")
            return null
        }
    }

    def populateLocationMetadataForSite(Map site, List<String> fids = null) {

        Map siteGeom
        if (site.type == Site.TYPE_COMPOUND) {
            siteGeom = [
                    type:'GeometryCollection',
                    geometries: [
                            site.features.collect{it.geometry}
                    ]
            ]
        }
        else {
            siteGeom = geometryAsGeoJson(site)
        }

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
                def centroid = [Double.toString(geom.centroid.x), Double.toString(geom.centroid.y)]
                site.extent.geometry.centre = centroid

                site.extent.geometry.aream2 = GeometryUtils.area(geom)
            }
            else {
                log.error("No geometry for site: ${site.siteId}")
            }

            def geoFacets = lookupGeographicFacetsForSite(site, fids)
            def intersectionsAreaByFacets = geoFacets.remove(SpatialService.INTERSECTION_AREA)
            site.extent.geometry += geoFacets
            mergeIntersectionsArea(site, intersectionsAreaByFacets)
        }
    }

    /**
     * The data is stored in the format -
     * [ intersectionAreaByFacets: [ CURRENT: [act: 0.1, nsw: 0.6], cl22: [act: 0.1, nsw: 0.6]] ]
     * @param site
     * @param intersectionsAreaByFacets
     * @return
     */
    def mergeIntersectionsArea(Map site, Map intersectionsAreaByFacets) {
        Map geometry = site.extent.geometry
        List hubs = projectService.findHubIdFromProjectsOrCurrentHub(site.projects)
        String hubId = hubs?.size() == 1 ? hubs[0] : null
        Map existingIntersectionsArea = geometry[SpatialService.INTERSECTION_AREA] = geometry[SpatialService.INTERSECTION_AREA] ?: [:]
        intersectionsAreaByFacets?.each { String layer, Map nameAndValue ->
            if (nameAndValue) {
                Map facet = metadataService.getGeographicFacetConfig(layer, hubId)
                existingIntersectionsArea[facet.name] = existingIntersectionsArea[facet.name]?: [:]
                existingIntersectionsArea[facet.name][INTERSECTION_CURRENT] = nameAndValue
                existingIntersectionsArea[facet.name][layer] = nameAndValue
            }
        }

        site
    }

    /**
     * get images for a list of sites. The images are associated with the point of interest of each site.
     * @param ids
     * @param mongoParams
     * @param sort
     * @param order
     * @param max
     * @param offset
     * @return
     */
    List getImages (Set<String> ids, Map mongoParams, Long userId, String sort = 'lastUpdated', String order = 'desc', Integer max = 5, Integer offset = 0) throws Exception {
        Map documents, rPoi, rSite
        List result = []
        List sites = Site.findAllBySiteIdInListAndStatus(ids.toList(), Status.ACTIVE)
        sites.each { site ->
            if (site) {
                rSite = [siteId: site.siteId, name: site.name, type: site.type, description: site.description]
                rSite.poi = []
                site.poi?.each { poi ->
                    mongoParams.poiId = poi.poiId
                    documents = getPoiImages( mongoParams, userId, max, offset, sort, order)
                    rPoi = [name: poi.name, poiId: poi.poiId, type: poi.type, docs: documents]
                    rSite.poi.push(rPoi)
                }

                result.push(rSite)
            } else {
                log.debug('Could not find site of siteId: ' + siteId)
            }
        }

        result
    }

    /**
     * get images for a POI
     * @param mongoParams
     * @param userId
     * @param max
     * @param offset
     * @param sort
     * @param order
     * @return
     */
    public Map getPoiImages(Map mongoParams, Long userId, Integer max, Integer offset, String sort, String order) {
        Map documents
        documents = documentService.search(mongoParams, max, offset, sort, order)
        documents.documents = embargoDocuments(documents.documents, userId);
        return documents;
    }


    /**
     * Embargo a list of document.
     * 1. if user ala admin, then documents are not embargoed
     * 2. if user admin or editor of a project, then do not embargo document
     * 3. if user logged in or anonymous, embargo documents
     * @param documents
     * @param userId
     * @return
     */
    public List embargoDocuments( List documents, Long userId){
        Set<String> activityIds = []
        Map activities = [:]
        List<String> projectsTheUserIsAMemberOf
        documents?.each {
            activityIds.add(it.activityId);
        }

        Activity.findAllByActivityIdInList(activityIds, ACTIVE).collect {
            activities[it.activityId] = activityService.toMap(it, [FLAT])
            activities[it.activityId].projectActivity = projectActivityService.get(activities[it.activityId].projectActivityId,[FLAT])
        }

        if ( userId && ( permissionService.isUserAlaAdmin(userId?.toString()))) {
            projectsTheUserIsAMemberOf = null;
        } else if (userId) {
            projectsTheUserIsAMemberOf = permissionService.getProjectsForUser(userId?.toString(), AccessLevel.admin, AccessLevel.editor)
        } else {
            projectsTheUserIsAMemberOf = []
        }

        if(projectsTheUserIsAMemberOf != null){
            documents.each { Map doc ->
                Map activity = activities[doc.activityId]
                if( activity && ! (doc.projectId in projectsTheUserIsAMemberOf) ){
                    if(activity.projectActivity.visibility && activity.projectActivity.visibility?.embargoUntil.after(new Date())){
                        documentService.embargoDocument(doc);
                    }
                }
            }
        }

        documents
    }

    /**
     * Accepts a closure that will be called once for each (not deleted) Site in the system,
     * passing the site (as a Map) as the single parameter.
     * Implementation note, this uses the Mongo API directly as using GORM incurs a
     * significant memory and performance overhead when dealing with so many entities
     * at once.
     * @param action the action to be performed on each Activity.
     */
    void doWithAllSites(Closure action, List<Bson> filters = [], int batchSize = 100) {

        MongoCollection collection = Site.getCollection()
        Bson query = Filters.ne("status", DELETED)
        filters.add(query)
        query = Filters.and(filters)
        def results = collection.find(query).batchSize(batchSize)

        results.each { dbObject ->
            action.call(dbObject)
        }

    }


    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a map with two keys: "count": the total number of results, "sites": a list of the sites that match the supplied criteria
     */
    public Map search(Map searchCriteria, Integer max = 100, Integer offset = 0, String sort = null, String orderBy = null) {

        BuildableCriteria criteria = Site.createCriteria()
        List sites = criteria.list(max:max, offset:offset) {
            ne("status", DELETED)
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }
            if (sort) {
                order(sort, orderBy?:'asc')
            }

        }
        [sites:sites.collect{toMap(it)}, count:sites.totalCount]
    }

    void reloadSiteMetadata(List<String> fids = null, Date modifiedBefore = null, Integer max = 1000) {
        def collection = Site.getCollection()

       /* Bson query = Filters.and(
                                Filters.ne("status", "DELETED"),
                                (Filters.and(Filters.exists("projects", true), Filters.ne("projects", []))),
                                Filters.ne("refreshed", "Y")
                            )
        if (modifiedBefore) {
            query.and(Filters.lt("lastUpdated", modifiedBefore))
        }*/

        BasicDBObject query = new BasicDBObject()
        query.put('status', new BasicDBObject('$ne', DELETED))
        query.put('refreshed', new BasicDBObject('$ne', 'Y'))
        query.put('projects', new BasicDBObject(['$exists':true, '$ne':[]]))
        if (modifiedBefore) {
            query.put('lastUpdated', new BasicDBObject('$lt', modifiedBefore))
        }

        println collection.count(query)
        MongoCursor results = collection.find(query).batchSize(10).limit(max).iterator()
        int count = 0
        while (results.hasNext()) {
            DBObject site = results.next()
            try {
                if (site.extent?.geometry) {
                        Map<String, List<String>> geoFacets = lookupGeographicFacetsForSite(site, fids)
                        site.extent.geometry.putAll(geoFacets)
                }
                else {
                    log.warn( "No geometry for site "+site)
                    site.refreshFailed = 'no geometry'
                }

            }
            catch (Exception e) {
                log.error("Error updating site: "+site,e)
                site.refreshFailed = e.getMessage()
            }
            site.refreshed = "Y"
            collection.save(site)
            count ++
            if (count % 20 == 0) {
                log.info("Updated "+count+" of "+max+ " sites")
            }
        }
    }
    Map<String, List<String>> lookupGeographicFacetsForSite(Map site, List<String> fidsToLookup = null) {
        log.debug("Looking up geographic facets for site: "+site.siteId)
        Map<String, List<String>> geographicFacets = null
        switch (site.extent.source) {
            case 'pid':
                String fid = site.extent.geometry.fid
                geographicFacets = spatialService.intersectPid(site.extent.geometry.pid as String, fid, fidsToLookup)
                break
            default:
                Map geom
                if (site.type == Site.TYPE_COMPOUND) {
                    geom = [
                            'type':'GeometryCollection',
                            'geometries': site.features.collect{it.geometry} ?: []

                    ]
                }
                else {
                    geom = geometryAsGeoJson(site)
                }
                geographicFacets = spatialService.intersectGeometry(geom, fidsToLookup)
                break
        }
        geographicFacets ?: [:]

    }

    /**
     * check if GeoJson is valid. This is using Elastic Search's interpretation of GeoJson.
     * @param geoJson
     * @return
     */
    Boolean isGeoJsonValid(String geoJson){
        try {
            new GeometryJSON().read(geoJson)
        } catch (Exception e){
            log.error('Invalid GeoJson. ' + e.message)
            return false
        }

        return true
    }

    def sitesContainsName(String id, String entityType, String name) {

        def sites
        if(entityType == 'projectActivity') {
            def projectActivity = ProjectActivity.findByProjectActivityId(id)
            sites = projectActivity.sites
        } else if (entityType == 'project') {
            sites = Site.findAllByProjects(id).findAll({ it.status == ACTIVE }).collect { it.siteId }
        } else {
            throw new IllegalArgumentException("No entity type provided")
        }

        return Site.countBySiteIdInListAndName(sites, name) > 0
    }

    def addSpatialPortalPID(Map props, String userId){
        //if its a drawn shape, save and get a PID
        if (props?.extent?.source?.toLowerCase() == 'drawn') {
            def shapePid = persistSiteExtent(props.name, props.extent.geometry, userId)
            props.extent.geometry.pid = shapePid?.resp?.id ?: ""

            if (!props.extent.geometry.pid) {
                log.error("Failed persisting site on spatial portal. Site Id ${props.siteId}")
            }
        }
    }

    def persistSiteExtent(name, geometry, userId = "") {

        def resp = null
        if (geometry?.type == 'Circle') {
            def body = [name: name, description: "my description", user_id: userId, api_key: grailsApplication.config.getProperty('api_key')]
            def url = grailsApplication.config.getProperty('spatial.baseUrl') + "/ws/shape/upload/pointradius/" +
                    geometry?.coordinates[1] + '/' + geometry?.coordinates[0] + '/' + (geometry?.radius / 1000)
            resp = webService.doPost(url, body)
        } else if (geometry?.type in ['Polygon', 'LineString']) {
            def body = [geojson: [type: geometry.type, coordinates: geometry.coordinates], name: name, description: 'my description', user_id: userId, api_key: grailsApplication.config.getProperty('api_key')]
            resp = webService.doPost(grailsApplication.config.getProperty('spatial.baseUrl') + "/ws/shape/upload/geojson", body)
        }

        resp
    }

    def getSiteCentroid(Map site) {
        if ( site?.extent?.geometry?.centre ) {
            List coords = site.extent.geometry.centre
            [coords[0] as Double, coords[1] as Double]
        }
    }

    int calculateGeohashPrecision(Map boundingBox) {
        Geometry geom = GeometryUtils.geoJsonMapToGeometry(boundingBox)
        double area = GeometryUtils.area(geom)
        List lookupTable = grailsApplication.config.getProperty('geohash.lookupTable', List)
        int maxNumberOfGrids = grailsApplication.config.getProperty('geohash.maxNumberOfGrids', Integer)
        int maxLengthIndex = grailsApplication.config.getProperty('geohash.maxLength', Integer)
        Map step

        for(int i = 0; i < maxLengthIndex;  i++) {
            step = lookupTable[i]
            if ( (area / step.area) > maxNumberOfGrids ) {
                break
            }
        }

        step.length
    }

    String getSitePurposeLabel (String purpose) {
        switch (purpose) {
            case Site.EMSA_SITE_CODE:
                return EMSA_SITE
            case Site.REPORTING_SITE_CODE:
                return REPORTING_SITE
            case Site.PLANNING_SITE_CODE:
            default:
                return PLANNING_SITE
        }
    }

    String getPurpose (Map site) {
        String code = Site.PLANNING_SITE_CODE

        if (site.externalIds) {
            List idTypes = site.externalIds.idType
            if (!getMonitorIdTypes().intersect(idTypes).isEmpty())
                code = Site.EMSA_SITE_CODE
        } else if (site.type == Site.TYPE_COMPOUND) {
            code = Site.REPORTING_SITE_CODE
        }

        code
    }

    List findAllSitesExceptProjectArea(List<Map> sites) {
        sites?.findAll {
            it.type != Site.TYPE_PROJECT_AREA
        }
    }

    List findAllSitesByTypeProjectArea(List<Map> sites) {
        sites?.findAll { it.type == Site.TYPE_PROJECT_AREA }
    }

    /**
     * Check if the intersection area is calculated for all provided layers
     * @param site
     * @param fids
     * @return
     */
    Boolean areIntersectionCalculatedForAllLayers(def site){
        List fids = metadataService.getGeographicConfig().checkForBoundaryIntersectionInLayers
        fids?.every { fid ->
            String group = metadataService.getGeographicFacetConfig(fid, null)?.name
            site.extent?.geometry?[SpatialService.INTERSECTION_AREA]?[group]?[fid] != null
        }
    }
}
