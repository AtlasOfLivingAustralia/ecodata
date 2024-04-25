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

}
