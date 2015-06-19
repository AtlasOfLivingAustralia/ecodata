package au.org.ala.ecodata

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Specification / tests for the SiteService
 */
@TestFor(SiteService)
class SiteServiceSpec extends Specification {

    def webServiceMock = Mock(WebService)
    void setup() {
        service.webService = webServiceMock
        service.grailsApplication = grailsApplication
    }

    // We should be storing the extent geometry as geojson already to enable geographic searching using
    // mongo / elastic search.  But we aren't (at least not for all types), so the conversion is currently necessary.
    def "The site extent can be converted to geojson"() {

        when: "The site is a drawn rectangle"
        def coordinates = [ [ 148.260498046875, -37.26530995561874 ], [ 148.260498046875, -35.1288943410105 ], [ 149.710693359375, -35.1288943410105 ], [ 149.710693359375, -37.26530995561874 ], [ 149.710693359375, -37.26530995561874 ], [ 148.260498046875, -37.26530995561874 ] ]
        def extent = buildExtent('drawn', 'Polygon', coordinates)
        def geojson = service.geometryAsGeoJson([extent:extent])

        then: "The site is already valid geojson"
        geojson.type == 'Polygon'
        geojson.coordinates == coordinates

        when: "The site is a drawn circle"
        extent = [source:'drawn', geometry: [type:'Circle', centre: [134.82421875, -33.41310193384], radius:12700, pid:'1234']]
        geojson = service.geometryAsGeoJson([extent:extent])

        then: "Circles aren't valid geojson so we should ask the spatial portal for help"
        1 * webServiceMock.getJson("${grailsApplication.config.spatial.baseUrl}/ws/shape/geojson/1234") >> [type:'Polygon', coordinates: []]
        geojson.type == 'Polygon'
        geojson.coordinates == []
    }


    private Map buildExtent(source, type, coordinates, pid = '') {
        return [source:source, geometry:[type:type, coordinates: coordinates, pid:pid]]
    }
}
