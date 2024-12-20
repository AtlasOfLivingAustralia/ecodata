package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller

/*import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin*/

import org.grails.web.converters.marshaller.json.MapMarshaller
/**
 * Specification / tests for the SiteService
 */

class SiteServiceSpec extends MongoSpec implements ServiceUnitTest<SiteService> {

    //def service = new SiteService()
    def webServiceMock = Mock(WebService)
    def metadataServiceMock = Mock(MetadataService)
    def spatialServiceMock = Mock(SpatialService)
    def projectService = Mock(ProjectService)
    CommonService commonService = new CommonService()
    void setup() {
        //defineBeans {
       //     commonService(CommonService)
       // }
        service.commonService = commonService
        service.commonService.grailsApplication = grailsApplication
       // grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
        //mongoDomain([Site])
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
        service.webService = webServiceMock
        service.grailsApplication = grailsApplication
        service.metadataService = metadataServiceMock
        service.spatialService = spatialServiceMock
        service.projectService = projectService
     //   grailsApplication.mainContext.registerSingleton('commonService', CommonService)
     //   grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
    }

    void cleanup() {
        Site.collection.remove(new BasicDBObject())
    }

    // We should be storing the extent geometry as geojson already to enable geographic searching using
    // mongo / elastic search.  But we aren't (at least not for all types), so the conversion is currently necessary.
    def "The site extent can be converted to geojson"() {

        when: "The site is a drawn rectangle"
        def coordinates = [ [ 148.260498046875, -37.26530995561874 ], [ 148.260498046875, -35.1288943410105 ], [ 149.710693359375, -35.1288943410105 ], [ 149.710693359375, -37.26530995561874 ], [ 148.260498046875, -37.26530995561874 ] ]
        def extent = buildExtent('drawn', 'Polygon', coordinates)
        def geojson = service.geometryAsGeoJson([extent:extent])

        then: "The site is already valid geojson"
        geojson.type == 'Polygon'
        geojson.coordinates == [coordinates]

        and: "It can be validated"
        service.isGeoJsonValid((geojson as JSON).toString())

        when: "The site is a drawn circle"
        extent = [source:'drawn', geometry: [type:'Circle', centre: [134.82421875, -33.41310193384], coordinates: [134.82421875, -33.41310193384], radius:12700, pid:'1234']]
        geojson = service.geometryAsGeoJson([extent:extent])

        then: "Circles aren't valid geojson so we need to convert them to a polygon"
        geojson.type == 'Polygon'
        geojson.coordinates[0].size() == 101
        service.isGeoJsonValid((geojson as JSON).toString())

        when: "The site is a line"
        coordinates = [[145.42448043823242,-37.72728027686003],[148.00626754760742,-37.16031654673676],[148.36881637573242,-37.77071473849609],[147.09440231323242,-38.59111377614743]]
        extent = ["source":"drawn","geometry":["type":"LineString","coordinates": coordinates]]
        geojson = service.geometryAsGeoJson([extent:extent])

        then: "Is site a valid GeoJSON"
        geojson.type == 'LineString'
        geojson.coordinates == coordinates
        service.isGeoJsonValid((geojson as JSON).toString())
    }

    def "Duplicate coordinates must be removed"() {
        when: "duplicate points are included"
        def coordinates = [ [ 1, 2 ], [ 1, 2 ], [ 3, 4 ], [ 3,4 ], [ 5, 6 ], [ 5, 6 ], [ 1, 2 ] ]
        def sanitisedCoordinates = service.removeDuplicatesFromCoordinates(coordinates)

        then: "duplicate points are removed"
        sanitisedCoordinates.size() == 4
        sanitisedCoordinates == [ [ 1, 2 ], [ 3, 4 ],  [ 5, 6 ], [ 1, 2 ] ]


        when: "duplicate points are not included"
        coordinates = [ [ 1, 2 ], [ 3, 4 ], [ 5, 6 ], [ 1, 2 ] ]
        sanitisedCoordinates = service.removeDuplicatesFromCoordinates(coordinates)

        then: "coordinates remain the same"
        sanitisedCoordinates.size() == 4
        sanitisedCoordinates == [ [ 1, 2 ], [ 3, 4 ],  [ 5, 6 ], [ 1, 2 ] ]


        when: "when a point is passed"
        coordinates = [ 1, 2 ]
        sanitisedCoordinates = service.removeDuplicatesFromCoordinates(coordinates)

        then: "coordinates remain the same"
        sanitisedCoordinates.size() == 2
        sanitisedCoordinates == [ 1, 2 ]

        when: "when null is passed"
        coordinates = null
        sanitisedCoordinates = service.removeDuplicatesFromCoordinates(coordinates)

        then: "coordinates remain the same"
        sanitisedCoordinates == null

        when: "duplicates present in multi-polygon"
        coordinates = [
                [
                        [       [ 1, 2 ], [ 1, 2 ], [ 3, 4 ], [ 3,4 ], [ 5, 6 ], [ 5, 6 ], [ 1, 2 ]     ]
                ],
                [
                        [       [ 1, 2 ], [ 1, 2 ], [ 3, 4 ], [ 3,4 ], [ 5, 6 ], [ 5, 6 ], [ 1, 2 ]     ],
                        [       [ 1, 2 ], [ 1, 2 ], [ 3, 4 ], [ 3,4 ], [ 5, 6 ], [ 5, 6 ], [ 1, 2 ]     ]
                ]
        ]
        sanitisedCoordinates = service.removeDuplicatesFromCoordinates(coordinates)

        then: "duplicate points are removed keeping multi-polygonal structure"
        sanitisedCoordinates.size() == 2
        sanitisedCoordinates[0][0] == [ [ 1, 2 ], [ 3, 4 ],  [ 5, 6 ], [ 1, 2 ] ]
        sanitisedCoordinates[1][0] == [ [ 1, 2 ], [ 3, 4 ],  [ 5, 6 ], [ 1, 2 ] ]
        sanitisedCoordinates[1][1] == [ [ 1, 2 ], [ 3, 4 ],  [ 5, 6 ], [ 1, 2 ] ]
        println(sanitisedCoordinates)

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

    def "An externalId can be supplied and the Site will convert it to the correct format"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name:'Site 1', externalId:'e1'])
            session.flush()
        }
        then:
        def site = Site.findBySiteId(result.siteId)
        site.name == 'Site 1'
        site.externalId == 'e1'
        site.externalIds.size() == 1
        site.externalIds[0].externalId == 'e1'
        site.externalIds[0].idType == ExternalId.IdType.UNSPECIFIED
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
        def site = Site.findBySiteId (result.siteId)

        // Reference to the DBO is because POI is currently a dynamic property (it shouldn't be)
        site.poi.size() == 1
        site.poi[0].poiId != null
    }

    def "New sites without a centroid should have one assigned"() {
        when:
        def result
        projectService.findHubIdFromProjectsOrCurrentHub(_) >> []
        Site.withSession { session ->
            result = service.create([name: 'Site 1', extent: [source: 'pid', geometry: [type: 'pid', pid: 'cl123']]])
            session.flush()
        }


        then:
        1 * webServiceMock.get(_, _) >>  "POLYGON ((137 -34, 137 -35, 136 -35, 136 -34, 137 -34))"
        1 * spatialServiceMock.intersectPid('cl123', null, null) >> [state:'state1', test:'test']

        def site = Site.findBySiteId (result.siteId)
        //def site = Site.collection.find([siteId:result.siteId]).next()

        // Reference to the DBO is because POI is currently a dynamic property (it shouldn't be)
        site.extent.geometry.centre == ['136.5', '-34.5']
        site.extent.geometry.test == 'test'
    }

    def "must return precision depending on area of bounding box" () {
        def result

        expect:
        service.calculateGeohashPrecision(bbox) == precision

        where:
        precision   | bbox
        4           | ["type":"Polygon","coordinates":[[[132.890625,-31.53076171875],[156.796875,-31.53076171875],[156.796875,-16.435546875],[132.890625,-16.435546875],[132.890625,-31.53076171875]]]]
        5           | ["type":"Polygon","coordinates":[[[148.59386444091797,-22.8820269764962],[148.79093170166016,-22.8820269764962],[148.79093170166016,-22.761302755997598],[148.59386444091797,-22.761302755997598],[148.59386444091797,-22.8820269764962]]]]
    }

    def "Data can be extracted from geojson"() {
        setup:
        def geojsonPolygon =
                [
                        type: 'Feature',
                        geometry: [
                                type: 'Polygon',
                                coordinates: [
                                        [
                                                [ 1, 2 ], [ 3, 4 ], [ 5, 6 ], [ 1, 2 ]
                                        ]
                                ]
                        ],
                        properties: [
                                name: 'Site 1'
                        ]
                ]
        def geoJsonPoint = [
                type: 'Feature',
                geometry: [
                        type: 'Point',
                        coordinates: [1, 2]
                ],
                properties: [
                        name: 'Site 1'
                ]
        ]

        when:
        Map result = service.propertiesFromGeoJson(geojsonPolygon, 'upload')

        then:
        result.extent.geometry.type == geojsonPolygon.geometry.type
        result.extent.geometry.coordinates == geojsonPolygon.geometry.coordinates
        result.extent.source == 'upload'
        result.name == "Site 1"

        when:
        result = service.propertiesFromGeoJson(geoJsonPoint, 'upload')

        then:
        result.extent.geometry.type == geoJsonPoint.geometry.type
        result.extent.geometry.coordinates == geoJsonPoint.geometry.coordinates
        result.extent.source == 'point'
        result.extent.geometry.decimalLatitude == geoJsonPoint.geometry.coordinates[1]
        result.extent.geometry.decimalLongitude == geoJsonPoint.geometry.coordinates[0]
        result.name == "Site 1"
    }

    def "Sites can be found by externalId (including type)"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name:'Site 1', siteId:"s1", externalIds:[new ExternalId(externalId:'e1', idType:ExternalId.IdType.MONITOR_PLOT_GUID)]])
            session.flush()
        }
        then:
        def site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, 'e1')
        site.name == 'Site 1'
        site.externalIds.size() == 1
        site.externalIds[0].externalId == 'e1'
        site.externalIds[0].idType == ExternalId.IdType.MONITOR_PLOT_GUID

    }

    def "Sites can be listed by externalId and sorted"() {
        when:
        def result
        Site.withSession { session ->
            result = service.create([name:'Site 1', siteId:"s1", externalIds:[new ExternalId(externalId:'e1', idType:ExternalId.IdType.MONITOR_PLOT_GUID)]])
            session.flush()
            result = service.create([name:'Site 2', siteId:"s2", externalIds:[new ExternalId(externalId:'e1', idType:ExternalId.IdType.MONITOR_PLOT_GUID)]])
            session.flush()
        }
        then:
        def sites = Site.findAllByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, 'e1', ['sort': "lastUpdated", 'order': "desc"])
        sites.size() == 2
        sites[0].name == 'Site 2'
        sites[0].externalIds.size() == 1
        sites.externalIds.externalId == [['e1'], ['e1']]
        sites.externalIds.idType == [[ExternalId.IdType.MONITOR_PLOT_GUID], [ExternalId.IdType.MONITOR_PLOT_GUID]]
    }

    def "The site area is calculated from the FeatureCollection for a compound site"() {
        setup:
        projectService.findHubIdFromProjectsOrCurrentHub(_) >> []
        def coordinates = [[148.260498046875, -37.26530995561874], [148.260498046875, -37.26531995561874], [148.310693359375, -37.26531995561874], [148.310693359375, -37.26531995561874], [148.260498046875, -37.26530995561874]]
        def extent = buildExtent('drawn', 'Polygon', coordinates)
        Map site = [type: Site.TYPE_COMPOUND, extent: extent, features: [
                [
                        type    : "Feature",
                        geometry: [
                                type       : "Polygon",
                                coordinates: coordinates
                        ]
                ],
                [
                        type    : "Feature",
                        geometry: [
                                type       : "Polygon",
                                coordinates: coordinates
                        ]
                ]
        ]]

        when:
        service.populateLocationMetadataForSite(site)

        then:
        1 * spatialServiceMock.intersectGeometry({it.type == 'GeometryCollection'}, _) >> ["electorate":["Bean"], "state":["ACT"]]
        site.extent.geometry.aream2 == 4938.9846950349165d
        site.extent.geometry.electorate == ["Bean"]
        site.extent.geometry.state == ["ACT"]

        when:
        site.type = Site.TYPE_WORKS_AREA
        service.populateLocationMetadataForSite(site)

        then: "Each feature is intersected individually and duplicates removed"
        1 * spatialServiceMock.intersectGeometry(_, _) >> ["state":["ACT"]]
        site.extent.geometry.aream2 == 2469.492347517461
        site.extent.geometry.state == ["ACT"]

    }

    def "Site service returns simplified geometry for siteId"() {
        when:
        def coordinates = [[148.260498046875, -37.26530995561874], [148.260498046875, -37.26531995561874], [148.310693359375, -37.26531995561874], [148.310693359375, -37.26531995561874], [148.260498046875, -37.26530995561874]]
        def extent = [
                geometry: [
                        type       : "Polygon",
                        coordinates: coordinates
                ]
        ]
        def newSite = service.create([name:'Site 1', extent: extent])

        then:
        service.getSimpleProjectArea(newSite.siteId) != null
    }

    private Map buildExtent(source, type, coordinates, pid = '') {
        return [source:source, geometry:[type:type, coordinates: coordinates, pid:pid]]
    }
}
