package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

class SpatialServiceSpec extends Specification implements ServiceUnitTest<SpatialService> {
    WebService webService = Mock(WebService)

    def setup () {
        grailsApplication.config.spatial.intersectionThreshold = 0.05
        grailsApplication.config.spatial.baseUrl = ""
        service.grailsApplication = grailsApplication
        service.webService = webService

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    def "fillMissingDetailsOfObjects should add details of missing spatial objects"() {
        setup:
        def response = ["123": [[name: "ACT"], [name: "NSW"]]]
        when:
        service.fillMissingDetailsOfObjects(response)

        then:
        1 * webService.getJson("/ws/search?q=ACT&include=123") >> [[name: "ACT", fid: "123", pid: "345"], [name: "ACT and QLD", fid: "123", pid: "789"]]
        1 * webService.getJson("/ws/search?q=NSW&include=123") >> [[name: "NSW", fid: "123", pid: "890"]]
        response["123"].find {it.name == "ACT" }.pid == "345"
        response["123"].find {it.name == "NSW" }.pid == "890"
    }

    def "isValidGeometryIntersection should correctly identify overlapping objects" () {
        setup:
        def shape1 = GeometryUtils.geoJsonMapToGeometry([type: "Polygon", coordinates: [[[10, 0], [10, 10], [0, 10], [0, 0], [10, 0]]]])
        def shape2 = GeometryUtils.geoJsonMapToGeometry([type: "Polygon", coordinates: [[[5, 0], [5, 5], [0, 5], [0, 0], [5, 0]]]])
        def boundaryShape = GeometryUtils.geoJsonMapToGeometry([type: "Polygon", coordinates: [[[11.5, 9.5], [11.5, 11.5], [9.5, 11.5], [9.5, 9.5], [11.5, 9.5]]]])

        when:
        def genuineIntersection = service.isValidGeometryIntersection(shape1, shape2)
        def boundaryIntersection = service.isValidGeometryIntersection(shape1, boundaryShape)

        then:
        genuineIntersection
        !boundaryIntersection
    }

    def "filterOutObjectsInBoundary should remove intesection below threshold" () {
        setup:
        def response = ["cl22": [[name: "ACT", pid: "123", fid:"cl22"], [name: "NSW", pid: "456", fid:"cl22"]]]
        def mainObject = [type: "Polygon", coordinates: [[[10, 0], [10, 10], [0, 10], [0, 0], [10, 0]]]]

        when:
        service.filterOutObjectsInBoundary(response, mainObject)

        then:
        response["cl22"].size() == 1
        response["cl22"][0].name == "ACT"
        1 * webService.getJson("/ws/shapes/geojson/123") >> [type: "Polygon", coordinates: [[[5, 0], [5, 5], [0, 5], [0, 0], [5, 0]]]]
        1 * webService.getJson("/ws/shapes/geojson/456") >> [type: "Polygon", coordinates: [[[11.5, 9.5], [11.5, 11.5], [9.5, 11.5], [9.5, 9.5], [11.5, 9.5]]]]
    }
}
