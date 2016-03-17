package au.org.ala.ecodata

import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * The SpatialService is responsible for:
 * 1. The interface to the spatial portal.
 * 2. Working with the facet configuration to convert the data returned from the spatial portal into
 *    the format used by ecodata.
 */
@Transactional
class SpatialService {

    final String GEOJSON_INTERSECT_URL_PREFIX = "/intersect/geojson/"
    final String PID_INTERSECT_URL_PREFIX = "/intersect/object/"

    WebService webService
    MetadataService metadataService
    GrailsApplication grailsApplication

    /**
     * Invokes the spatial portal layer service to intersect the supplied geojson against a list of fields (layers)
     * and returns the names of the objects in each field that intersect with the supplied geometry.
     * @param geoJson a map containing geojson describing the geometry
     * @return Map with key = fieldId, value = List<String>, the names of the field objects that intersect with the
     * supplied geometry
     */
    Map<String,List<String>> intersectGeometry(Map geoJson) {

        String url = 'http://spatial.ala.org.au/ws'+GEOJSON_INTERSECT_URL_PREFIX //grailsApplication.config.spatial.baseUrl+GEOJSON_INTERSECT_URL_PREFIX
        List<String> fieldIds = metadataService.getSpatialLayerIdsToIntersect()
        Map result = [:]
        fieldIds.each { fid ->
            Map response = webService.doPost(url+fid, geoJson)
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
     * @return Map with key = fieldId, value = List<String>, the names of the field objects that intersect with the
     * supplied geometry
     */
    Map<String,List<String>> intersectPid(String pid) {

        String url = 'http://spatial.ala.org.au/ws'+PID_INTERSECT_URL_PREFIX //grailsApplication.config.spatial.baseUrl+PID_INTERSECT_URL_PREFIX
        List<String> fieldIds = metadataService.getSpatialLayerIdsToIntersect()

        Map result = [:]
        fieldIds.each { fid ->
            Object response = webService.getJson(url+fid+"/"+pid)
            if (response instanceof List) {
                result[fid] = response
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
