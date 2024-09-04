package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.services.ServiceUnitTest
import org.codehaus.jackson.map.ObjectMapper
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.locationtech.jts.geom.Geometry
import spock.lang.Specification

class SpatialServiceSpec extends Specification implements ServiceUnitTest<SpatialService> {
    WebService webService = Mock(WebService)
    ObjectMapper mapper = new ObjectMapper()

    def setup () {
        grailsApplication.config.spatial.intersectionThreshold = 0.05
        grailsApplication.config.spatial.baseUrl = ""
        service.grailsApplication = grailsApplication
        service.webService = webService
        service.metadataService = new MetadataService()
        service.metadataService.hubService = new HubService()
        service.metadataService.grailsApplication = grailsApplication

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
        def shape3 = GeometryUtils.geoJsonMapToGeometry(mapper.readValue('{' +
                '        "type": "Polygon",' +
                '        "coordinates": [[' +
                '          [0.000, 0.000],' +
                '          [0.000, 0.02],' +
                '          [0.02, 0.02],' +
                '          [0.02, 0.000],' +
                '          [0.000, 0.000]' +
                '        ]]' +
                '      }', Map.class ))
        def shape4 = GeometryUtils.geoJsonMapToGeometry(mapper.readValue('{' +
                '        "type": "Polygon",' +
                '        "coordinates": [[' +
                '          [-0.01, -0.01],' +
                '          [-0.01, 0.01],' +
                '          [0.01, 0.01],' +
                '          [0.01, -0.01],' +
                '          [-0.01, -0.01]' +
                '        ]]' +
                '      }', Map.class))
        def shape1 = GeometryUtils.geoJsonMapToGeometry(getShape1())
        def shape2 = getShape2()

        def boundaryShape = getBoundaryShape()

        when:
        def genuineIntersection = service.isValidGeometryIntersection(shape4, shape3)
        def genuineIntersection2 = service.isValidGeometryIntersection(shape1, shape2)
        def boundaryIntersection = service.isValidGeometryIntersection(shape1, boundaryShape)

        then:
        genuineIntersection
        genuineIntersection2
        !boundaryIntersection
    }

    def "filterOutObjectsInBoundary should remove intersection below threshold" () {
        setup:
        grailsApplication.config.app.facets.geographic.contextual.state = "cl22"
        grailsApplication.config.app.facets.geographic.checkForBoundaryIntersectionInLayers = ["cl22"]
        def response = ["cl22": [[name: "ACT", pid: "123", fid:"cl22", fieldname: "ACT"], [name: "NSW", pid: "456", fid:"cl22", fieldname: "NSW"]]]
        def mainObject = getShape1()

        when:
        def (filteredResponse, intersectionProportion) = service.filterOutObjectsInBoundary(response, mainObject)

        then:
        filteredResponse["cl22"].size() == 1
        filteredResponse["cl22"][0].name == "ACT"
        intersectionProportion.size() == 1
        intersectionProportion["cl22"]["ACT"] != null
        intersectionProportion["cl22"]["NSW"] == null
        1 * webService.get("/ws/shapes/wkt/123") >> getShape2()
        1 * webService.get("/ws/shapes/wkt/456") >> getBoundaryShape()
    }
    
    private Geometry getBoundaryShape() {
        return GeometryUtils.geoJsonMapToGeometry(mapper.readValue('{' +
                '        "coordinates": [' +
                '          [' +
                '            [' +
                '              -0.018754367050215708,' +
                '              -0.018926888600058193' +
                '            ],' +
                '            [' +
                '              -0.018754367050215708,' +
                '              0.0010731113999418087' +
                '            ],' +
                '            [' +
                '              0.0012456329497842946,' +
                '              0.0010731113999418087' +
                '            ],' +
                '            [' +
                '              0.0012456329497842946,' +
                '              -0.018926888600058193' +
                '            ],' +
                '            [' +
                '              -0.018754367050215708,' +
                '              -0.018926888600058193' +
                '            ]' +
                '          ]' +
                '        ],' +
                '        "type": "Polygon"' +
                '      }', Map.class))
    }

    private Map getShape1() {
        return mapper.readValue('{' +
                '        "type": "Polygon",' +
                '        "coordinates": [' +
                '          [' +
                '            [' +
                '              0,' +
                '              0' +
                '            ],' +
                '            [' +
                '              0.5,' +
                '              0' +
                '            ],' +
                '            [' +
                '              0.5,' +
                '              0.5' +
                '            ],' +
                '            [' +
                '              0,' +
                '              0.5' +
                '            ],' +
                '            [' +
                '              0,' +
                '              0' +
                '            ]' +
                '          ]' +
                '        ]' +
                '      }', Map.class)
    }

    private Geometry getShape2() {
        GeometryUtils.geoJsonMapToGeometry(mapper.readValue('{' +
                '        "coordinates": [' +
                '          [' +
                '            [' +
                '              -0.13755907377421295,' +
                '              0.14911497361144654' +
                '            ],' +
                '            [' +
                '              -0.13755907377421295,' +
                '              -0.0466462909049028' +
                '            ],' +
                '            [' +
                '              0.1484061923408433,' +
                '              -0.0466462909049028' +
                '            ],' +
                '            [' +
                '              0.1484061923408433,' +
                '              0.14911497361144654' +
                '            ],' +
                '            [' +
                '              -0.13755907377421295,' +
                '              0.14911497361144654' +
                '            ]' +
                '          ]' +
                '        ],' +
                '        "type": "Polygon"' +
                '      }', Map.class))
    }
}
