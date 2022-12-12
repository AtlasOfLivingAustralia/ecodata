package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest

class MetadataServiceSpec extends MongoSpec implements ServiceUnitTest<MetadataService> {

    WebService webService = Mock(WebService)
    SettingService settingService = Mock(SettingService)

    def setup() {
        service.grailsApplication = grailsApplication
        grailsApplication.config.google = [geocode: [url: 'url'], api: [key:'abc']]
        grailsApplication.config.spatial= [intersectUrl: 'url']
        grailsApplication.config.app.facets.geographic.contextual = ['state':'cl927', 'cmz':'cl2112']
        grailsApplication.config.app.facets.geographic.grouped = [other:['australian_coral_ecoregions':'cl917'], gerSubRegion:['gerBorderRanges':'cl1062']]
        grailsApplication.config.app.facets.geographic.special = [:]
        service.settingService = settingService
        service.webService = webService
    }

    def cleanup() {
        Service.findAll().each { it.delete() }
        Score.findAll().each{ it.delete() }
        ActivityForm.findAll().each{ it.delete() }
    }

    private void setupServices() {
        for (int i in 1..4) {
            Service service = new Service(legacyId:i, serviceId:'s'+i, name:'Service '+i)
            service.setOutputs([new ServiceForm(formName:"form 1", sectionName: "section 1")])
            service.insert()

        }

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

        setup:
        setupServices()

        when: "We retrieve the services and there are none in the database"
        List services = service.getServiceList()

        then:
        services.size() == 4
        for (int i in 1..4) {
            services[i-1].legacyId == i
            services[i-1].name == "Service "+i
        }
    }

    def "Convert double value to int for Services"(){
        setup:
        setupServices()
        String projectId = "project_10"
        Map project = [projectId: projectId,
                       outputTargets:[
                               [scoreId: "1", target: "10", scoreLabel: "Test Score Label 1", unit: "Ha", scoreName: "areaTreatedHa", outputLabel: "Test Output Label 1"]],
                       custom: [details: [serviceIds:[1.0, 2.0,3.0,4.0]]]]
        Score score = new Score(scoreId:"1", label:"Test Score Label 1", entity:"Activity", isOutputTarget: true,  outputType: "RLP - Baseline data")
        score.configuration = [filter:[filterValue:'section 1']]
        score.insert(flush:true)

        when:
        List results = service.getProjectServicesWithTargets(project)

        then:
        results.size() == 4
        results*.name == ["Service 1", "Service 2", "Service 3", "Service 4"]
        results[0].scores[0].label == "Test Score Label 1"
        results[0].scores[0].isOutputTarget == true
        results[0].scores[0].target == "10"
        results[0].scores[0].preiodTargets == null

    }

    def "The withAllActivityForms method allows us to perform an action against all saved forms"() {

        setup:
        ActivityForm form1 = new ActivityForm(name:'test', formVersion:1, status: Status.ACTIVE, type:'Activity')
        form1.save(flush:true, failOnError: true)
        ActivityForm form2 = new ActivityForm(name:'test', formVersion:2, status: Status.DELETED, type:'Activity')
        form2.save(flush:true, failOnError: true)
        ActivityForm form3 = new ActivityForm(name:'abc', formVersion:1, status: Status.ACTIVE, type:'Activity')
        form3.save(flush:true, failOnError: true)
        ActivityForm form4 = new ActivityForm(name:'abc', formVersion:2, status: Status.ACTIVE, type:'Activity')
        form4.save(flush:true, failOnError: true)

        when:
        List forms = []
        service.withAllActivityForms { ActivityForm form ->
            forms << form
        }

        then:
        forms.collect{it.name } == ['test', 'abc', 'abc']

    }
}
