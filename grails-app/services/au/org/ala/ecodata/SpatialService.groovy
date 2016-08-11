package au.org.ala.ecodata

import com.vividsolutions.jts.geom.Geometry
import grails.transaction.Transactional
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.commons.GrailsApplication
/**
 * The SpatialService is responsible for:
 * 1. The interface to the spatial portal.
 * 2. Working with the facet configuration to convert the data returned from the spatial portal into
 *    the format used by ecodata.
 */
@Transactional
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
        int threshold = grailsApplication.config.spatial.geoJsonEnvelopeConversionThreshold
        if(length > threshold){
            Geometry geo = GeometryUtils.geoJsonMapToGeometry (geoJson)
            geoJson = GeometryUtils.geometryToGeoJsonMap (geo.getEnvelope())
        }

        String url = grailsApplication.config.spatial.baseUrl+WKT_INTERSECT_URL_PREFIX
        if (!fieldIds) {
            fieldIds = metadataService.getSpatialLayerIdsToIntersect()
        }

        // We are using a WKT string instead of geojson as the spatial portal validates geojson - using
        // WKT allows us to get away with self intersecting polygons that users occasionally draw.
        String wkt = GeometryUtils.geoJsonMapToGeometry(geoJson).toText()

        Map result = [:]
        fieldIds.each { fid ->
            Map response = webService.doPost(url+fid, wkt)
            if (response.resp && !response.error) {
                result[fid] = response.resp
            }
        }
        convertResponsesToGeographicFacets(result)

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

        String url = grailsApplication.config.spatial.baseUrl+PID_INTERSECT_URL_PREFIX
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
        convertResponsesToGeographicFacets(result)
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
