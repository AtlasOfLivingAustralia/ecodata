package au.org.ala.ecodata

import grails.core.GrailsApplication
import grails.plugin.cache.Cacheable
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import org.geotools.geojson.geom.GeometryJSON
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory

import static ParatooService.deepCopy
/**
 * The SpatialService is responsible for:
 * 1. The interface to the spatial portal.
 * 2. Working with the facet configuration to convert the data returned from the spatial portal into
 *    the format used by ecodata.
 */
class SpatialService {

    final String GEOJSON_INTERSECT_URL_PREFIX = "/ws/intersect/geojson/"
    final String WKT_INTERSECT_URL_PREFIX = "/ws/intersect/wkt/"

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
    Map<String,List<String>> intersectGeometry(Map geoJson, List<String> fieldIds = null) {
        int length = geoJson?.toString().size()
        int threshold = grailsApplication.config.getProperty('spatial.geoJsonEnvelopeConversionThreshold', Integer)
        if(length > threshold){
            Geometry geo = GeometryUtils.geoJsonMapToGeometry (geoJson)
            geoJson = GeometryUtils.geometryToGeoJsonMap (geo.getEnvelope())
        }

        String url = grailsApplication.config.getProperty('spatial.baseUrl')+WKT_INTERSECT_URL_PREFIX
        if (!fieldIds) {
            fieldIds = metadataService.getSpatialLayerIdsToIntersect()
        }

        long start = System.currentTimeMillis()
        // We are using a WKT string instead of geojson as the spatial portal validates geojson - using
        // WKT allows us to get away with self intersecting polygons that users occasionally draw.
        String wkt = GeometryUtils.geoJsonMapToGeometry(geoJson).toText()
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
        filterOutObjectsInBoundary(result, geoJson)
        end = System.currentTimeMillis()
        log.info("Time taken to filter out objects in boundary: ${end-start}ms")

        start = end
        convertResponsesToGeographicFacets(result)
        end = System.currentTimeMillis()
        log.info("Time taken to convert responses to geographic facets: ${end-start}ms")

    }

    /**
     * Invokes the spatial portal layer service to intersect the geometry of the supplied pid against a list of fields (layers)
     * and returns the names of the objects in each field that intersect with the supplied geometry.
     * @param pid the spatial portal object id
     * @param fid the field id that identifies the layer the object/pid is related to.
     * @return Map with key = fieldId, value = List<String>, the names of the field objects that intersect with the
     * supplied geometry
     */
    Map<String,List<String>> intersectPid(String pid, String pidFid = null, List<String> fieldIds = null) {

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
                Object response = webService.getJson(url+fid+"/"+pid)
                if (response instanceof List) {
                    result[fid] = response
                }
            }
        }

        fillMissingDetailsOfObjects(result)
        Map pidGeoJson = getGeoJsonForPidToMap(pid)
        filterOutObjectsInBoundary(result, pidGeoJson)
        convertResponsesToGeographicFacets(result)
    }

    /**
     * Spatial portal intersection returns values at the boundary. This function filters the boundary intersection by
     * comparing if the area of intersection is less than a predefined amount 5% (this is configurable).
     * NOTE: GeoJSON objects must be valid for filtering out to work.
     * @param response - per layer/fid intersection values - [ "cl34" : [[pid: 123, name: "ACT", fid: "cl34", id: "ACT" ...], ...]
     * @param mainObjectGeoJson - GeoJSON object that is used to intersect with layers.
     */
    void filterOutObjectsInBoundary(Map response, Map mainObjectGeoJson) {
        Geometry mainGeometry = GeometryUtils.geoJsonMapToGeometry(mainObjectGeoJson)
        if (!mainGeometry.isValid()) {
            log.info("Main geometry invalid. Cannot check intersection is near boundary.")
            return
        }

        response?.each { String fid, List<Map> matchingObjects ->
            List pidToFilter = []
            matchingObjects.each { Map obj ->
                String boundaryPid = obj.pid
                if (boundaryPid) {
                    // Get geoJSON of the object stored in spatial portal
                    long start = System.currentTimeMillis()
//                    def boundaryGeoJson = getGeoJsonForPidToMap(boundaryPid)
//                    long end = System.currentTimeMillis()
//                    log.debug("Time taken to get geojson for pid $boundaryPid: ${end-start}ms")
//
//                    start = end
//                    Geometry boundaryGeometry = GeometryUtils.geoJsonMapToGeometry(boundaryGeoJson)
                    Geometry boundaryGeometry = getGeoJsonForPidToGeometry(boundaryPid)
                    long end = System.currentTimeMillis()
                    log.debug("Time taken to convert geojson to geometry for pid $boundaryPid: ${end-start}ms")

                    if (boundaryGeometry.isValid()) {
                        // check if intersection should be ignored
                        start = end
                        if (!isValidGeometryIntersection(mainGeometry, boundaryGeometry))
                            pidToFilter.add(boundaryPid)
                        end = System.currentTimeMillis()
                        log.debug("Time taken to check intersection for pid $boundaryPid: ${end-start}ms")
                    }
                    else {
                        log.debug ("Cannot check object $boundaryPid($fid) is near main geomerty")
                    }
                }
            }

            matchingObjects.removeAll {
                def result = pidToFilter.contains(it.pid)
                result ? log.debug("Filtered out ${it.pid} object") : null
                result
            }
        }
    }

    /**
     * Calculates area of intersection and check the overlap to be more than 5% (configurable) of site area.
     * @param mainGeometry
     * @param boundaryGeometry
     * @return true - if intersection area is greater than intersection threshold
     * false - if intersection area is less than or equal to intersection threshold
     */
    boolean isValidGeometryIntersection (Geometry mainGeometry, Geometry boundaryGeometry) {
        Double intersectionThreshold = grailsApplication.config.getProperty("spatial.intersectionThreshold", Double)
        try {
            if (mainGeometry.contains(boundaryGeometry) || boundaryGeometry.contains(mainGeometry))
                return true
            else {
                Geometry intersection = boundaryGeometry.intersection(mainGeometry)
                double intersectArea = intersection.getArea()
                double mainGeometryArea = mainGeometry.getArea()
                return intersectArea/mainGeometryArea > intersectionThreshold
            }
        }
        catch (Exception ex) {
            log.error("Error checking intersection between geometries", ex)
            return true
        }
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
        query = URLEncoder.encode(query, 'UTF-8').replaceAll('\\+', '%20')
        String url = grailsApplication.config.getProperty('spatial.baseUrl')+"/ws/search?q=$query&include=$fids"
        def resp = webService.getJson(url)
        if ((resp instanceof  Map) || !resp)
            return

        def result = resp?.find { it.name?.toUpperCase() == query?.toUpperCase() }
        deepCopy(result)
    }

    @Cacheable(value = "spatialGeoJsonPidObject")
    Map getGeoJsonForPidToMap(String pid) {
        log.debug("Cache miss for getGeoJsonForPidToMap($pid)")
        getGeoJsonForPid(pid)
    }

    @Cacheable(value = "spatialGeoJsonPidObjectGeometry", key={pid})
    Geometry getGeoJsonForPidToGeometry(String pid) {
        log.debug("Cache miss for getGeoJsonForPidToMap($pid)")
        log.debug("Cache miss for getGeoJsonForPid($pid)")
        String url = grailsApplication.config.getProperty('spatial.baseUrl')+"/ws/shapes/geojson/$pid"
        String jsonText = webService.get(url)

        Geometry geometry = null
        try {
            geometry = new GeometryJSON().read(jsonText)
            log.info("*************************************Successfully created geometry for pid $pid")
        }
        catch (Exception e) {
            log.error("Error reading geometry for pid $pid")
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
        String url = grailsApplication.config.getProperty('spatial.baseUrl')+"/ws/shapes/geojson/$pid"
        Map resp = webService.getJson(url)

        if (resp.error) {
            return null
        }

        deepCopy(resp)
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
