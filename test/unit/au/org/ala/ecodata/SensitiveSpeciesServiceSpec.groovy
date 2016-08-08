package au.org.ala.ecodata

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(SensitiveSpeciesService)
class SensitiveSpeciesServiceSpec extends Specification {
    SensitiveSpeciesService sensitiveSpeciesService = new SensitiveSpeciesService()
    WebService webservice

    def setup() {
        String sensitiveData = """<sensitiveSpeciesList>
\t<sensitiveSpecies name="Amphidium tortuosum" family="Rhabdoweisiaceae" guid="28153cb8-efb7-4d23-84f9-4ffcf3da1cc4" rank="SPECIES" commonName="">
    \t<instances>
\t      <conservationInstance category="P1" authority="WA DEC" dataResourceId="dr467" generalisation="10km" zone="WA" />
    \t</instances>
\t</sensitiveSpecies>
\t<sensitiveSpecies name="Caladenia sp. aff. fragrantissima (Central Victoria)" family="" guid="ALA_Caladenia_sp._aff._fragrantissima_(Central_Victoria)" rank="INFRASPECIFICNAME" commonName="Bendigo Spider-orchid">
    \t<instances>
\t      <conservationInstance category="EN" authority="Vic DSE" dataResourceId="dr490" generalisation="10km" zone="VIC" />
    \t</instances>
\t</sensitiveSpecies>
</sensitiveSpeciesList>"""

        sensitiveSpeciesService.sensitiveSpeciesData = new XmlParser().parseText(sensitiveData)
        webservice = Mock(WebService)
        sensitiveSpeciesService.webService = webservice
        sensitiveSpeciesService.googleMapsUrl = "https://maps.googleapis.com/maps/api/geocode/json"
        sensitiveSpeciesService.mapsApiKey = ""
    }

    def cleanup() {
    }

    void "species is not in sensitive list"() {
        when:
        webservice.getJson(_) >>  [status:"OK", results:[[address_components:[[short_name:'VIC', types:['administrative_area_level_1']]]]]]
        Map result = sensitiveSpeciesService.findSpecies('ABC',-37.965605, 145.071759)

        then:
        result == [:]
    }

    void "species is in sensitive list"() {
        when:
        webservice.getJson(_) >>  [status:"OK", results:[[address_components:[[short_name:'VIC', types:['administrative_area_level_1']]]]]]
        Map result = sensitiveSpeciesService.findSpecies('Caladenia sp. aff. fragrantissima (Central Victoria)',-37.965605, 145.071759)

        then:
        result != [:]
    }

    void "species is in sensitive list but not in the specified zone"() {
        when:
        webservice.getJson(_) >>  [status:"OK", results:[[address_components:[[short_name:'WA', types:['administrative_area_level_1']]]]]]
        Map result = sensitiveSpeciesService.findSpecies('Caladenia sp. aff. fragrantissima (Central Victoria)',-37.965605, 145.071759)

        then:
        result == [:]
    }


    void "get new coordinate if species falls under WA zone"() {
        when:
        webservice.getJson(_) >>  [status:"OK", results:[[address_components:[[short_name:'WA', types:['administrative_area_level_1']]]]]]
        Map result = sensitiveSpeciesService.findSpecies('Amphidium tortuosum',-28.221294, 125.199572)

        then:
        result.lat == -28.1
        result.lng == 125.3
        result.zone == "WA"

    }


    void "get new coordinate if species falls under VIC zone"() {
        when:
        webservice.getJson(_) >>  [status:"OK", results:[[address_components:[[short_name:'VIC', types:['administrative_area_level_1']]]]]]
        Map result = sensitiveSpeciesService.findSpecies('Caladenia sp. aff. fragrantissima (Central Victoria)',-37.965605, 145.071759)

        then:
        result.lat == -37.9
        result.lng == 145.2
        result.zone == "VIC"
    }
}
