package au.org.ala.ecodata

import grails.core.GrailsApplication
import grails.plugin.cache.Cacheable
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTReader

import static ParatooService.deepCopy
/**
 * The SpatialService is responsible for:
 * 1. The interface to the spatial portal.
 * 2. Working with the facet configuration to convert the data returned from the spatial portal into
 *    the format used by ecodata.
 */
class SpatialService {

    static final String INTERSECTION_AREA = "intersectionAreaByFacets"
    final String GEOJSON_INTERSECT_URL_PREFIX = "/ws/intersect/geojson/"
    final String WKT_INTERSECT_URL_PREFIX = "/ws/intersect/wkt/"
    final String WKT_SHAPE_URL_PREFIX = "/ws/shapes/wkt/"

    final String PID_INTERSECT_URL_PREFIX = "/ws/intersect/object/"
    final String LOOKUP_TABLE_PATH = "/data/pidIntersectCache.json"

    WebService webService
    MetadataService metadataService
    GrailsApplication grailsApplication

    Map lookupTable

    public SpatialService() {
        JsonSlurper js = new JsonSlurper()
        js.setType(JsonParserType.CHARACTER_SOURCE)
        lookupTable = js.parse(SpatialService.getResourceAsStream(LOOKUP_TABLE_PATH))
    }

    /**
     * Invokes the spatial portal layer service to intersect the supplied geojson against a list of fields (layers)
     * and returns the names of the objects in each field that intersect with the supplied geometry.
     * @param geoJson a map containing geojson describing the geometry
     * @return Map with key = fieldId, value = List<String>, the names of the field objects that intersect with the
     * supplied geometry
     */
    Map<String,?> intersectGeometry(Map geoJson, List<String> fieldIds = null) {
        // We are using a WKT string instead of geojson as the spatial portal validates geojson - using
        // WKT allows us to get away with self intersecting polygons that users occasionally draw.
        String wkt
        int threshold = grailsApplication.config.getProperty('spatial.geoJsonEnvelopeConversionThreshold', Integer)
        Geometry geo
        // Ignore threshold check for GeometryCollection collection
        if (geoJson.type != 'GeometryCollection') {
            int length = geoJson?.toString().size()
            if (length > threshold) {
                geo = GeometryUtils.geoJsonMapToGeometry (geoJson)
                geoJson = GeometryUtils.geometryToGeoJsonMap(geo.getEnvelope())
            }

            geo = GeometryUtils.geoJsonMapToGeometry (geoJson)
            if (!geo.isValid()) {
                geo = geo.buffer(0)
            }
            wkt = geo.toText()
        } else {
            geo = GeometryUtils.geoJsonMapToGeometry (geoJson)
            GeometryCollection geometryCollection = (GeometryCollection)geo
            if(!geometryCollection.isValid()) {
                geometryCollection = geometryCollection.buffer(0)
            }

            Geometry convexHullGeometry = geometryCollection.union().convexHull()
            wkt = convexHullGeometry.toText()
        }

        String url = grailsApplication.config.getProperty('spatial.baseUrl')+WKT_INTERSECT_URL_PREFIX
        if (!fieldIds) {
            fieldIds = metadataService.getSpatialLayerIdsToIntersect()
        }

        long start = System.currentTimeMillis()
        long end = System.currentTimeMillis()
        log.info("Time taken to convert geojson to wkt: ${end-start}ms")


        Map result = [:]
        fieldIds.each { fid ->
            start = end
            Map response = webService.doPost(url+fid, wkt)
            if (response.resp && !response.error) {
                result[fid] = response.resp
            }
            end = System.currentTimeMillis()
            log.info("Time taken to intersect with layer $fid: ${end-start}ms")
        }

        start = end

        Map geographicFacets
        Map intersectionAreaForFacets = [:].withDefault{ [:].withDefault{0.0d} }
        if (geo.geometryType == 'GeometryCollection') {
            geographicFacets = [:].withDefault{[]}
            GeometryCollection geometryCollection = (GeometryCollection)geo
            for (int i=0; i<geometryCollection.numGeometries; i++) {
                def (filtered, intersectionArea) = filterOutObjectsInBoundary(result, geometryCollection.getGeometryN(i))
                start = end
                Map geographicFacetsForGeometry = convertResponsesToGeographicFacets(filtered)
                geographicFacetsForGeometry.each { k, v ->
                    geographicFacets[k] += v
                    geographicFacets[k] = geographicFacets[k].unique()
                }
                intersectionArea.each { k, v ->
                    v.each { fieldName, area ->
                        intersectionAreaForFacets[k][fieldName] =  intersectionAreaForFacets[k][fieldName] + area
                    }
                }
            }
            
            geographicFacets[INTERSECTION_AREA] = intersectionAreaForFacets
        }
        else {
            def (filtered, intersectionArea) = filterOutObjectsInBoundary(result, geo)
            geographicFacets = convertResponsesToGeographicFacets(filtered)
            geographicFacets[INTERSECTION_AREA] = intersectionArea
        }
        end = System.currentTimeMillis()
        log.info("Time taken to convert responses to geographic facets: ${end-start}ms")
        geographicFacets
    }

    /**
     * Invokes the spatial portal layer service to intersect the geometry of the supplied pid against a list of fields (layers)
     * and returns the names of the objects in each field that intersect with the supplied geometry.
     * @param pid the spatial portal object id
     * @param fid the field id that identifies the layer the object/pid is related to.
     * @return Map with key = fieldId, value = List<String>, the names of the field objects that intersect with the
     * supplied geometry
     */
    Map<String,?> intersectPid(String pid, String pidFid = null, List<String> fieldIds = null) {

        String url = grailsApplication.config.getProperty('spatial.baseUrl')+PID_INTERSECT_URL_PREFIX
        if (!fieldIds) {
            fieldIds = metadataService.getSpatialLayerIdsToIntersect()
        }

        Map result = [:]
        fieldIds.each { fid ->

            if (pidFid && lookupTable.get(pidFid)?.get(pid)?.get(fid)) {
                result[fid] = lookupTable[pidFid][pid][fid].collect{[name:it]}
            }
            else {
                def response = getPidFidIntersection(url, fid, pid)
                result[fid] = response
            }
        }

        fillMissingDetailsOfObjects(result)
        Map pidGeoJson = getGeoJsonForPidToMap(pid)
        def (geographicFacetsWithFID, intersectionProportion) = filterOutObjectsInBoundary(result, pidGeoJson)
        Map geographicFacets = convertResponsesToGeographicFacets(geographicFacetsWithFID)
        geographicFacets[INTERSECTION_AREA] = intersectionProportion
        geographicFacets
    }

    @Cacheable(value = "spatialPidFidIntersection")
    List getPidFidIntersection(String url, String fid, String pid) {
        def intersections = webService.getJson(url + fid + "/" + pid)
        if (intersections instanceof List) {
            List intersectionsList = intersections.collect { obj ->
                new HashMap(obj)
            }
            return intersectionsList
        }
        else {
            log.info("No intersection for pid $pid and fid $fid")
            return []
        }
    }

    private List filterOutObjectsInBoundary(Map response, Map mainObjectGeoJson) {
        Geometry mainGeometry = GeometryUtils.geoJsonMapToGeometry(mainObjectGeoJson)
        filterOutObjectsInBoundary(response, mainGeometry)
    }

    /**
     * Spatial portal intersection returns values at the boundary. This function filters the boundary intersection by
     * comparing if the area of intersection is less than a predefined amount 5% (this is configurable).
     * NOTE: GeoJSON objects must be valid for filtering out to work.
     * @param response - per layer/fid intersection values - [ "cl34" : [[pid: 123, name: "ACT", fid: "cl34", id: "ACT" ...], ...]
     * @param mainObjectGeoJson - GeoJSON object that is used to intersect with layers.
     */
    private List filterOutObjectsInBoundary(Map response, Geometry mainGeometry) {
        List checkForBoundaryIntersectionInLayers = metadataService.getGeographicConfig().checkForBoundaryIntersectionInLayers
        if (!mainGeometry.isValid()) {
            // fix invalid geometry
            mainGeometry = mainGeometry.buffer(0)
            if (!mainGeometry.isValid()) {
                log.info("Main geometry invalid. Cannot check intersection is near boundary.")
                return [response, [:]]
            }
        }
        Map filteredResponse = [:]
        Map intersectionAreaByFacets = [:].withDefault { [:] }
        response?.each { String fid, List<Map> matchingObjects ->
            filteredResponse[fid] = []
            // check for boundary intersection object for selected layers defined in config.
            if (checkForBoundaryIntersectionInLayers.contains(fid)) {
                matchingObjects.each { Map obj ->
                    String boundaryPid = obj.pid
                    if (boundaryPid) {
                        log.debug("Intersecting ${obj.fieldname}(${fid}) - ${obj.name} ")
                        // Get geoJSON of the object stored in spatial portal
                        long start = System.currentTimeMillis()

                        Geometry boundaryGeometry = getGeometryForPid(boundaryPid)
                        long end = System.currentTimeMillis()
                        log.debug("Time taken to convert geojson to geometry for pid $boundaryPid: ${end - start}ms")

                        if (!boundaryGeometry.isValid()) {
                            // fix invalid geometry
                            boundaryGeometry = boundaryGeometry.buffer(0)
                            if (!boundaryGeometry.isValid()) {
                                log.debug("Cannot check object $boundaryPid($fid) is near main geomerty")
                                return
                            }
                        }

                        // check if intersection should be ignored
                        start = end
                        if (isValidGeometryIntersection(mainGeometry, boundaryGeometry)) {
                            filteredResponse[fid].add(obj)
                            def (intersectionAreaOfMainGeometry, area) = getIntersectionProportionAndArea(mainGeometry, boundaryGeometry)
                            intersectionAreaByFacets[fid][obj.name] = area
                        } else {
                            log.debug("Filtered out ${obj.fieldname}(${fid}) - ${obj.name}")
                        }

                        end = System.currentTimeMillis()
                        log.debug("Time taken to check intersection for pid $boundaryPid: ${end - start}ms")
                    }
                }
            } else {
                filteredResponse[fid].addAll(matchingObjects)
            }
        }

        [filteredResponse, intersectionAreaByFacets]
    }

    /**
     * Calculates area of intersection and check the overlap to be more than 5% (configurable) of site area.
     * @param mainGeometry
     * @param boundaryGeometry
     * @return true - if intersection area is greater than or equal to intersection threshold or intersection area is greater than or equal to 10,000 hectare
     * false - if intersection area is less than intersection threshold and intersection area is less than 10,000 hectare
     */
    boolean isValidGeometryIntersection (Geometry mainGeometry, Geometry boundaryGeometry) {
        Double intersectionThreshold = grailsApplication.config.getProperty("spatial.intersectionThreshold", Double)
        Integer threshold = grailsApplication.config.getProperty('spatial.intersectionAreaThresholdInHectare', Integer)
        try {
            if (mainGeometry.contains(boundaryGeometry) || boundaryGeometry.contains(mainGeometry))
                return true
            else {
                def (intersectionProportion, area) = getIntersectionProportionAndArea (mainGeometry, boundaryGeometry)
                return ( intersectionProportion >= intersectionThreshold ) // intersection is greater than 5% of site area
                        || ( area / 10_000 >= threshold ) // or, intersection area is greater than threshold hectare defined in config. NOTE: area returned by GeometryUtils.area is in m2.
            }
        }
        catch (Exception ex) {
            log.error("Error checking intersection between geometries", ex)
            return true
        }
    }

    List getIntersectionProportionAndArea (Geometry mainGeometry, Geometry boundaryGeometry) {
        Geometry intersection = boundaryGeometry.intersection(mainGeometry)
        double intersectArea = intersection.getArea()
        double mainGeometryArea = mainGeometry.getArea()
        double proportion = 0.0
        double area = 0.0d
        if (mainGeometryArea != 0.0d) {
            proportion = intersectArea/mainGeometryArea
        }

        if (intersectArea != 0.0d) {
            area = GeometryUtils.area(intersection)
        }

        [proportion, area]
    }

    /**
     * Search spatial portal to get details of a intersecting object. Useful when intersection is done using lookup table.
     * @param response
     */
    void fillMissingDetailsOfObjects(Map response) {
        response?.each { String fid, List<Map> matchingObjects ->
            matchingObjects.each { Map obj ->
                String pid = obj.pid
                if (!pid) {
                    def spatialObj = searchObjectToMap(obj.name, fid) ?: [:]
                    obj << spatialObj
                }
            }
        }
    }

    @Cacheable(value = "spatialSearchObjectMap")
    Map searchObjectToMap(String query, String fids = "") {
        searchObject(query, fids)
    }

    @Cacheable(value = "spatialSearchObject", key = { query.toUpperCase() + fids.toUpperCase() })
    Map searchObject(String query, String fids = "") {
        String urlquery = URLEncoder.encode(query, 'UTF-8').replaceAll('\\+', '%20')
        String url = grailsApplication.config.getProperty('spatial.baseUrl')+"/ws/search?q=$urlquery&include=$fids"
        def resp = webService.getJson(url)
        if ((resp instanceof  Map) || !resp)
            return [:]

        def result = resp?.find { it.name?.toUpperCase() == query?.toUpperCase() } ?: [:]
        deepCopy(result)
    }

    @Cacheable(value = "spatialGeoJsonPidObject")
    Map getGeoJsonForPidToMap(String pid) {
        log.debug("Cache miss for getGeoJsonForPidToMap($pid)")
        getGeoJsonForPid(pid)
    }

    @Cacheable(value = "spatialPidObjectGeometry", key={pid})
    Geometry getGeometryForPid(String pid) {
        log.debug("Cache miss for getGeometryForPid($pid)")
        String url = grailsApplication.config.getProperty('spatial.baseUrl')+"/ws/shapes/wkt/$pid"
        String wkt = webService.get(url)

        Geometry geometry = null
        try {
            geometry = new WKTReader().read(wkt)
        }
        catch (Exception e) {
            log.error("Error reading geometry for pid $pid")
            // Ehcache throws an error if a null value is returned, so we create a dummy geometry
            // that won't intersect with anything.
            geometry = new GeometryFactory().createPoint(new Coordinate(0, 0))
        }
        geometry
    }

    /**
     * Get GeoJSON of a spatial object.
     * @param pid
     * @return
     */
    @Cacheable(value="spatialGeoJsonPid", key= {pid})
    Map getGeoJsonForPid (String pid) {
        log.debug("Cache miss for getGeoJsonForPid($pid)")
        // spatial returning invalid polygons when using geojson endpoint
        String url = grailsApplication.config.getProperty('spatial.baseUrl')+"/ws/shapes/wkt/$pid"
        def resp = webService.get(url)

        if (resp instanceof Map) {
            return null
        }

        GeometryUtils.wktToGeoJson(resp)
    }

    /**
     * Converts the response from the spatial portal into geographic facets, taking into account the facet
     * configuration (whether the facet is made up of a single layer or a group of layers).
     * @param intersectResponse the response from the spatial portal from one of the intersect calls.
     * @return a Map, key=facetName, value=list of object or layer names that were intersected.
     */
    private Map<String, List<String>> convertResponsesToGeographicFacets(Map<String, List<Map>> intersectResponse) {

        Map<String, List<String>> result = [:]
        intersectResponse.each { String fid, List<Map> matchingObjects ->
            Map facetConfig = metadataService.getGeographicFacetConfig(fid)
            if (facetConfig.grouped) {
                result[facetConfig.name] = result[facetConfig.name]?:[]
                // Grouped facets combine multiple layers into a single facet.  If the site intersects with
                // any object in the layer, then that layer is added as a matching value to the facet.
                if (matchingObjects) {
                    result[facetConfig.name].add(matchingObjects[0].fieldname)
                }
            }
            else {
                result[facetConfig.name] = matchingObjects.collect{it.name}
            }
        }
        result
    }

}
