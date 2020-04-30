package au.org.ala.ecodata


import spock.lang.Specification


class MetadataServiceSpec extends Specification {


    MetadataService metadataService = new MetadataService()
    WebService webService

    def setup() {
        Map grailsApplication = [config:[app:[facets:[geographic:[:]]], google: [geocode: [url: 'url'], api: [key:'abc']], spatial: [intersectUrl: 'url']]]
        grailsApplication.config.app.facets.geographic.contextual = ['state':'cl927', 'cmz':'cl2112']
        grailsApplication.config.app.facets.geographic.grouped = [other:['australian_coral_ecoregions':'cl917'], gerSubRegion:['gerBorderRanges':'cl1062']]
        grailsApplication.config.app.facets.geographic.special = [:]
        metadataService.grailsApplication = grailsApplication
        webService = Mock(WebService)
        metadataService.webService = webService
    }

    void "getGeographicFacetConfig should correctly identify the facet name for a field id and whether it is grouped"(String fid, boolean grouped, String groupName) {

        expect:
            metadataService.getGeographicFacetConfig(fid).grouped == grouped
            metadataService.getGeographicFacetConfig(fid).name == groupName

        where:
        fid      || grouped | groupName
        'cl927'  || false   | 'state'
        'cl2112' || false   | 'cmz'
        'cl917'  || true    | 'other'
        'cl1062' || true    | 'gerSubRegion'

    }

    void "getLocationMetadataForPoint needs Google api key to add locality information" () {
        when:
        def result = metadataService.getLocationMetadataForPoint(0,0)

        then:
        result.locality == "location A"
        2 * webService.getJson(_)  >> [results: [["formatted_address": "location A"]]]

        when:
        metadataService.grailsApplication.config.google.api.key = ''
        result = metadataService.getLocationMetadataForPoint(0,0)

        then:
        result.locality == ''
    }
}
