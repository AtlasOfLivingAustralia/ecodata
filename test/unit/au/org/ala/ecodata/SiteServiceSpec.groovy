package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.codehaus.groovy.grails.web.converters.marshaller.json.CollectionMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

/**
 * Specification / tests for the SiteService
 */

@TestMixin(MongoDbTestMixin)
class SiteServiceSpec extends Specification {

    def service = new SiteService()
    def webServiceMock = Mock(WebService)
    def metadataServiceMock = Mock(MetadataService)
    def spatialServiceMock = Mock(SpatialService)
    void setup() {
        mongoDomain([Site])
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
        service.webService = webServiceMock
        service.grailsApplication = grailsApplication
        service.metadataService = metadataServiceMock
        service.spatialService = spatialServiceMock
        grailsApplication.mainContext.registerSingleton('commonService', CommonService)
        grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
    }

    void cleanup() {
        Site.collection.remove(new BasicDBObject())
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
        extent = [source:'drawn', geometry: [type:'Circle', centre: [134.82421875, -33.41310193384], coordinates: [134.82421875, -33.41310193384], radius:12700, pid:'1234']]
        geojson = service.geometryAsGeoJson([extent:extent])

        then: "Circles aren't valid geojson so we need to convert them to a polygon"
        geojson.type == 'Polygon'
        geojson.coordinates.size() == 101
    }

    def "A new site can be created"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name:'Site 1'])
            session.flush()
        }
        then:
        Site.findBySiteId(result.siteId).name == 'Site 1'
    }

    def "A new site should not allow the siteId to be supplied"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name:'Site 1', siteId:'1234'])
            session.flush()
        }
        then:
        result.siteId != '1234'
        Site.findBySiteId(result.siteId).name == 'Site 1'
    }

    def "Any POIs supplied with a new site should be assigned IDs"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name:'Site 1', poi:[[name:'poi 1', geometry: [decimalLatitude:-35, decimalLongitude:140]]]])
            session.flush()
        }
        then:
        // Using the mongo API as dynamic properties aren't mapped otherwise.
        def site = Site.collection.find([siteId:result.siteId]).next()

        // Reference to the DBO is because POI is currently a dynamic property (it shouldn't be)
        site.poi.size() == 1
        site.poi[0].poiId != null
    }

    def "New sites without a centroid should have one assigned"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name: 'Site 1', extent: [source: 'pid', geometry: [type: 'pid', pid: 'cl123']]])
            session.flush()
        }


        then:
        1 * webServiceMock.getJson(_) >>  [type:'Polygon', coordinates: [[137, -34], [137,-35], [136, -35], [136, -34], [137, -34]]]
        1 * spatialServiceMock.intersectPid('cl123', null, null) >> [state:'state1', test:'test']

        def site = Site.collection.find([siteId:result.siteId]).next()

        // Reference to the DBO is because POI is currently a dynamic property (it shouldn't be)
        site.extent.geometry.centre == ['136.5', '-34.5']
        site.extent.geometry.test == 'test'
    }


    private Map buildExtent(source, type, coordinates, pid = '') {
        return [source:source, geometry:[type:type, coordinates: coordinates, pid:pid]]
    }
}
