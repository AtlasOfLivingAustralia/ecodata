package au.org.ala.ecodata


import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class SpeciesReMatchServiceSpec extends Specification implements ServiceUnitTest<SpeciesReMatchService> {

    // write test case for SpeciesReMatchService.searchBie
    void setup() {
        service.cacheService = new CacheService()
        service.webService = Mock(WebService)
        grailsApplication.config.bie.url = "http://localhost:8080/"
    }

    void "test searchBie"() {
        setup:
        def resp  = [
                autoCompleteList: [
                        [name: "name", guid: "guid", commonName: "commonName"]
                ]
        ]
        service.webService.getJson(_) >> resp
        when:
        def result = service.searchBie("name")

        then:
        result == resp
    }

    // test same name returns same result
    void "test searchBie cache correctly"() {
        setup:
        int callCount = 0
        def resp  = [
                autoCompleteList: [
                        [name: "name", guid: "guid", commonName: "commonName"]
                ]
        ]
        def resp2 = [
                autoCompleteList: [
                        [name: "name2", guid: "guid2", commonName: "commonName2"]
                ]
        ]

        when:
        def result = service.searchBie("name")
        def result2 = service.searchBie("name")
        service.webService.getJson(_) >> {
            callCount++
            if (callCount == 0) {
                resp
            } else {
                resp2
            }
        }

        then:
        result == resp
        result2 == resp
        result2 != resp2

        when:
        result2 = service.searchBie("name2")

        then:
        result2 == resp2
    }

    void "search name server by name" () {
        setup:
        grailsApplication.config.namesmatching.url = "http://localhost:8080/"
        grailsApplication.config.namesmatching.strategy = ["exactMatch", "vernacularMatch"]
        def resp  = [
                "success": true,
                "scientificName": "Red",
                "taxonConceptID": "ALA_DR22913_1168_0",
                "rank": "genus",
                "rankID": 6000,
                "lft": 24693,
                "rgt": 24693,
                "matchType": "higherMatch",
                "nameType": "SCIENTIFIC",
                "kingdom": "Bamfordvirae",
                "kingdomID": "https://www.catalogueoflife.org/data/taxon/8TRHY",
                "phylum": "Nucleocytoviricota",
                "phylumID": "https://www.catalogueoflife.org/data/taxon/5G",
                "classs": "Megaviricetes",
                "classID": "https://www.catalogueoflife.org/data/taxon/6224M",
                "order": "Pimascovirales",
                "orderID": "https://www.catalogueoflife.org/data/taxon/623FC",
                "family": "Iridoviridae",
                "familyID": "https://www.catalogueoflife.org/data/taxon/BFM",
                "genus": "Red",
                "genusID": "ALA_DR22913_1168_0",
                "issues": [
                        "noIssue"
                ]
        ]
        service.webService.getJson(_) >> resp
        when:
        def result = service.searchByName("name")

        then:
        result == null

        when:
        resp.matchType = "exactMatch"
        def result2 = service.searchByName("name")

        then:
        service.webService.getJson(_) >> resp
        result2 == [
                scientificName: "Red",
                commonName: null,
                guid: "ALA_DR22913_1168_0",
                taxonRank: "genus"
        ]

        when:
        resp.matchType = "vernacularMatch"
        def result3 = service.searchByName("name", false, true)

        then:
        service.webService.getJson(_) >> resp
        result3 == [
                scientificName: "Red",
                commonName: null,
                guid: "ALA_DR22913_1168_0",
                taxonRank: "genus"
        ]

    }

}
