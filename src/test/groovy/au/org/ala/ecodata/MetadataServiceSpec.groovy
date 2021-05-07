package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class MetadataServiceSpec extends Specification implements ServiceUnitTest<MetadataService>, DataTest {

    WebService webService = Mock(WebService)
    SettingService settingService = Mock(SettingService)

    def setup() {
        Map grailsApplication = [config:[app:[facets:[geographic:[:]]], google: [geocode: [url: 'url'], api: [key:'abc']], spatial: [intersectUrl: 'url']]]
        grailsApplication.config.app.facets.geographic.contextual = ['state':'cl927', 'cmz':'cl2112']
        grailsApplication.config.app.facets.geographic.grouped = [other:['australian_coral_ecoregions':'cl917'], gerSubRegion:['gerBorderRanges':'cl1062']]
        grailsApplication.config.app.facets.geographic.special = [:]
        service.grailsApplication = grailsApplication
        service.settingService = settingService
        service.webService = webService
        mockDomain Score
    }

    void "getGeographicFacetConfig should correctly identify the facet name for a field id and whether it is grouped"(String fid, boolean grouped, String groupName) {

        expect:
        service.getGeographicFacetConfig(fid).grouped == grouped
        service.getGeographicFacetConfig(fid).name == groupName

        where:
        fid      || grouped | groupName
        'cl927'  || false   | 'state'
        'cl2112' || false   | 'cmz'
        'cl917'  || true    | 'other'
        'cl1062' || true    | 'gerSubRegion'

    }

    void "getLocationMetadataForPoint needs Google api key to add locality information" () {
        when:
        def result = service.getLocationMetadataForPoint(0,0)

        then:
        result.locality == "location A"
        2 * webService.getJson(_)  >> [results: [["formatted_address": "location A"]]]

        when:
        service.grailsApplication.config.google.api.key = ''
        result = service.getLocationMetadataForPoint(0,0)

        then:
        result.locality == ''
    }

    def "The service list can be retrieved from the database or backup json file"() {

        when: "We retrieve the services and there are none in the database"
        List services = service.getProjectServices()

        then: "then we fall back on the services.json in the classpath"
        1 * settingService.getSetting("services.config") >> null
        services.size() == 41

        when:
        services = service.getProjectServices()

        then:
        1 * settingService.getSetting("services.config") >> [[id:1, name:"Service"]]
        services.size() == 1
        services[0].id == 1
        services[0].name == "Service"
    }

    def "Convert double value to int for Services"(){
        setup:

        String projectId = "project_10"
        Map project = [projectId: projectId,
                       outputTargets:[
                               [scoreId: "1", target: "10", scoreLabel: "Test Score Label 1", unit: "Ha", scoreName: "areaTreatedHa", outputLabel: "Test Output Label 1"]],
                       custom: [details: [serviceIds:[1.0, 2.0,3.0,4.0]]]]
        Score score = new Score(scoreId:"1", label:"Test Score Label 1", entity:"Activity", isOutputTarget: true,  outputType: "RLP - Baseline data")
        score.save()

        when:
        List results = service.getProjectServicesWithTargets(project)

        then:
        results.size() == 4
        results*.name == ["Collecting, or synthesising baseline data", "Communication materials", "Community/stakeholder engagement", "Controlling access"]
        results[0].scores[0].label == "Test Score Label 1"
        results[0].scores[0].isOutputTarget == true
        results[0].scores[0].target == "10"
        results[0].scores[0].preiodTargets == null

    }
}
