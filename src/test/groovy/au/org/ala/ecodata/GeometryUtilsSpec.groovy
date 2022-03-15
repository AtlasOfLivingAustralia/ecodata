package au.org.ala.ecodata

import grails.converters.JSON
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

//************* Existing version of geo tools are not support with Java 11
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

//    def "The GeometryUtils class provides a convenience method to convert and simplify geometry stored as a geojson Map"() {
//
//        setup:
//        Map geojson = [
//                "type":"Polygon",
//                "coordinates":[[[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [0, 5], [0, 8], [0, 10], [1, 10], [5, 10], [10, 10], [9, 10], [5, 10], [0, 10], [0, 6], [0, 3], [0, 0]]]
//        ]
//        when:
//        Map simplifiedGeometry = GeometryUtils.simplify(geojson, 1)
//
//        then:
//        simplifiedGeometry.type == 'Polygon'
//        simplifiedGeometry.coordinates.size() <= geojson.coordinates.size()
//    }
//
//    def "The GeometryUtils class can find and identify neighbour polygons in a List of geojson Features"() {
//
//        setup:
//        List features = []
//        features << squareFeature("1", 0, 0)
//        features << squareFeature("2", 1, 0)
//        features << squareFeature("3", 1, 1)
//        features << squareFeature("4", 2, 0)
//
//        when:
//        GeometryUtils.assignNeighboursToFeatures(features, "neighbours")
//
//        then:
//        features[0].properties.neighbours == ["2", "3"]
//        features[1].properties.neighbours == ["1", "3", "4"]
//        features[2].properties.neighbours == ["1", "2", "4"]
//        features[3].properties.neighbours == ["2", "3"]
//
//    }
//
//
//    def "The GeometryUtils class can assign distinct values such that neighbouring features do not share the same value"() {
//
//        setup:
//        List features = []
//        features << squareFeature("1", 0, 0)
//        features << squareFeature("2", 1, 0)
//        features << squareFeature("3", 1, 1)
//        features << squareFeature("4", 2, 0)
//
//        when:
//        GeometryUtils.assignDistinctValuesToNeighbouringFeatures(features, "type")
//
//        then:
//        features[0].properties.type == 1
//        features[1].properties.type == 2
//        features[2].properties.type == 3
//        features[3].properties.type == 1
//
//    }
}
