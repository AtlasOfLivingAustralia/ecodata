package au.org.ala.ecodata

import grails.converters.JSON
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.locationtech.jts.geom.*
import org.opengis.referencing.crs.CoordinateReferenceSystem
import spock.lang.Specification

class GeometryUtilsSpec extends Specification {

    def setup() {
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }


    private Map squareFeature(String id, int x, int y) {
        Map feature = [
                type:'Feature',
                geometry:[
                        type:'Polygon',
                        coordinates: [[[x, y], [x, y+1], [x+1, y+1], [x+1, y], [x, y]]]
                ],
                properties:[id:id]
        ]
        feature
    }

    def "The GeometryUtils class provides a convenience method to convert and simplify geometry stored as a geojson Map"() {

        setup:
        Map geojson = [
                "type":"Polygon",
                "coordinates":[[[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [0, 5], [0, 8], [0, 10], [1, 10], [5, 10], [10, 10], [9, 10], [5, 10], [0, 10], [0, 6], [0, 3], [0, 0]]]
        ]
        when:
        Map simplifiedGeometry = GeometryUtils.simplify(geojson, 1)

        then:
        simplifiedGeometry.type == 'Polygon'
        simplifiedGeometry.coordinates.size() <= geojson.coordinates.size()
    }

    def "The GeometryUtils class can find and identify neighbour polygons in a List of geojson Features"() {

        setup:
        List features = []
        features << squareFeature("1", 0, 0)
        features << squareFeature("2", 1, 0)
        features << squareFeature("3", 1, 1)
        features << squareFeature("4", 2, 0)

        when:
        GeometryUtils.assignNeighboursToFeatures(features, "neighbours")

        then:
        features[0].properties.neighbours == ["2", "3"]
        features[1].properties.neighbours == ["1", "3", "4"]
        features[2].properties.neighbours == ["1", "2", "4"]
        features[3].properties.neighbours == ["2", "3"]

    }


    def "The GeometryUtils class can assign distinct values such that neighbouring features do not share the same value"() {

        setup:
        List features = []
        features << squareFeature("1", 0, 0)
        features << squareFeature("2", 1, 0)
        features << squareFeature("3", 1, 1)
        features << squareFeature("4", 2, 0)

        when:
        GeometryUtils.assignDistinctValuesToNeighbouringFeatures(features, "type")

        then:
        features[0].properties.type == 1
        features[1].properties.type == 2
        features[2].properties.type == 3
        features[3].properties.type == 1

    }

    def "wktToGeoJson should correctly convert WKT to GeoJSON"() {
        given: "A WKT string representing a point"
        String wkt = "POINT (30 10)"

        when:
        Map result = GeometryUtils.wktToGeoJson(wkt)

        then:
        result != null
        result.type == "Point"
        result.coordinates == [30.0, 10.0]
    }

    def "wktToGeoJson should handle precision with the specified number of decimals"() {
        given: "A WKT string representing a geometry with decimals"
        String wkt = "POINT (30.1234567890123456789 10.1234567890123456789)"

        when:
        Map result = GeometryUtils.wktToGeoJson(wkt, 5)

        then: "The result has the coordinates rounded to the specified number of decimals"
        result != null
        result.type == "Point"
        result.coordinates == [30.12346, 10.12346]
    }

    def "a point in UTM should be converted to WGS84" () {
        GeometryFactory factory = new GeometryFactory()
        // Create a point in UTM (approximate meters)
        Point utmPoint = factory.createPoint(new Coordinate(724061.775, 7973106.940))


        when:
        CoordinateReferenceSystem utmCrs = org.geotools.referencing.CRS.decode("AUTO2:42001,143.12,-18.32", true)
        Geometry wgs84Point = GeometryUtils.utmToWgs84(utmPoint, utmCrs)

        then:
        (143.1199..143.1201).containsWithinBounds(wgs84Point.getCoordinate().x)
        (-18.3201..-18.3199).containsWithinBounds(wgs84Point.getCoordinate().y)
    }

    def "should correctly convert a buffered UTM polygon to WGS84"() {
        given: "A line with two coordinates UTM"
        GeometryFactory factory = new GeometryFactory()
        LineString line = factory.createLineString([
                new Coordinate(143.12, -18.32),
                new Coordinate(143.13, -18.31)
        ] as Coordinate[])

        when: "It is converted to WGS84"
        Geometry wgs84Buffer = GeometryUtils.convertLineStringOrMultiLineStringToThinPolygon(line, 0.5)

        then: "The resulting geometry is a valid polygon"
        wgs84Buffer.isValid()
        wgs84Buffer.geometryType == "Polygon"
    }
}
