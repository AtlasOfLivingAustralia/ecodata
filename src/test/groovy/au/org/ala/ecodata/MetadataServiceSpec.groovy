package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.json.simple.JSONArray
import org.json.simple.JSONObject

class MetadataServiceSpec extends MongoSpec implements ServiceUnitTest<MetadataService> {

    WebService webService = Mock(WebService)
    SettingService settingService = Mock(SettingService)
    ActivityFormService activityFormService = Mock(ActivityFormService)

    def setup() {
        service.grailsApplication = grailsApplication
        grailsApplication.config.google = [geocode: [url: 'url'], api: [key:'abc']]
        grailsApplication.config.spatial= [intersectUrl: 'url']
        grailsApplication.config.app.facets.geographic.contextual = ['state':'cl927', 'cmz':'cl2112']
        grailsApplication.config.app.facets.geographic.grouped = [other:['australian_coral_ecoregions':'cl917'], gerSubRegion:['gerBorderRanges':'cl1062']]
        grailsApplication.config.app.facets.geographic.special = [:]
        service.settingService = settingService
        service.webService = webService
        service.activityFormService = activityFormService

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    def cleanup() {
        Service.findAll().each { it.delete() }
        Score.findAll().each{ it.delete() }
        ActivityForm.findAll().each{ it.delete() }
        Program.findAll().each { it.delete() }
    }

    private void setupServices() {
        for (int i in 1..4) {
            Service service = new Service(legacyId:i, serviceId:'s'+i, name:'Service '+i)
            service.setOutputs([new ServiceForm(formName:"form 1", sectionName: "section 1")])
            service.insert()

        }

    }

    private def createActivity(props) {
        Activity.withNewTransaction {
            Activity activity = new Activity(props)
            activity.save(failOnError: true, flush: true)
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

    def "Get the activities from program config"() {
        setup:
        JSONArray jsonArrayActivities = new JSONArray()
        JSONObject jsonObjectActivity = new JSONObject()
        jsonObjectActivity.put("name","Administration, management & reporting")
        jsonArrayActivities.push(jsonObjectActivity)

        String programId = '123'
        Program program = new Program(programId:programId, name: 'test 123', description: 'description 1',
            config:[excludes:["excludes",["DATA_SETS", "MERI_PLAN"]], projectReports:["reportType":"Activity"], activities:jsonArrayActivities])
        program.save(flush:true, failOnError: true)


        List activityForms = new ArrayList()
        activityForms.add(new ActivityForm(name: 'test', formVersion: 1, supportsSites: true, supportsPhotoPoints: true, type: 'Activity'))

        when:
        Map result = service.activitiesListByProgramId(programId)

        then:
        1 * activityFormService.search(_) >> activityForms
        result != [:]

    }

    def "Program config has no activities"() {
        setup:
        Program.findAll().each { it.delete() }
        String programId = '456'
        Program program = new Program(programId:programId, name: 'test 123', description: 'description 1',
            config:[excludes:["excludes",["DATA_SETS", "MERI_PLAN"]], projectReports:["reportType":"Activity"]])
        program.save(flush:true, failOnError: true)


        List activityForms = new ArrayList()
        activityForms.add(new ActivityForm(name: 'test', formVersion: 1, supportsSites: true, supportsPhotoPoints: true, type: 'Activity'))

        when:
        Map result = service.activitiesListByProgramId(programId)

        then:
        1 * activityFormService.search(_) >> []
        result == [:]

    }

    def "excelWorkbookToMap: get content from bulk_import_example.xlsx into a list"() {
        setup:
        service.activityFormService = new ActivityFormService()
        service.cacheService = new CacheService()
        service.excelImportService = new ExcelImportService()
        ActivityForm form1 = new ActivityForm(
                name: 'form1',
                formVersion: 1,
                status: Status.ACTIVE,
                publicationStatus: PublicationStatus.PUBLISHED,
                type: 'Activity',
                sections: [
                        new FormSection (
                                name: 'form1',
                                templateName: 'form1',
                                template: [
                                        name: 'form1',
                                        dataModel: [
                                        [
                                                name: 'a',
                                                dataType: 'string',
                                                label: 'A',
                                                required: true
                                        ],
                                        [
                                                name: 'b',
                                                dataType: 'list',
                                                required: true,
                                                columns: [
                                                        [
                                                                name: 'c',
                                                                dataType: 'stringList',
                                                                required: true
                                                        ]
                                                ]
                                        ],
                                        [
                                                name: 'd',
                                                dataType: 'list',
                                                required: true,
                                                columns: [
                                                        [
                                                                name: 'e',
                                                                dataType: 'string',
                                                                required: true
                                                        ],
                                                        [
                                                                name: 'f',
                                                                dataType: 'species',
                                                                required: true
                                                        ]
                                                ]
                                        ],
                                        [
                                                name: 'g',
                                                dataType: 'list',
                                                required: true,
                                                columns: [
                                                        [
                                                                name: 'h',
                                                                dataType: 'stringList',
                                                                required: true
                                                        ],
                                                        [
                                                                name: 'i',
                                                                dataType: 'species',
                                                                required: true
                                                        ]
                                                ]
                                        ]
                                ]
                            ]
                        )
                ]
        )
        form1.save(flush: true, failOnError: true)
        def file = new File("src/test/resources/bulk_import_example.xlsx").newInputStream()

        when:
        def content = service.excelWorkbookToMap(file, 'form1', true, null)

        then:
        content.size() == 2
        content[0][0].data.size() == 5
        content[0][0].data.get("serial") == 1.0
        content[0][0].data.get("a") == "test a 1"
        content[0][0].data.get("b") == [[c: ["test bc 1", "test bc 2", "test bc 3"]]]
        content[0][0].data.get("d").size() == 3
        content[0][0].data.get("d")[0].e == "test de 1"
        content[0][0].data.get("d")[0].f.name == "sci name 1 (com name 1)"
        content[0][0].data.get("d")[0].f.scientificName == "sci name 1"
        content[0][0].data.get("d")[0].f.commonName == "com name 1"
        content[0][0].data.get("d")[0].f.guid == "id1"
        content[0][0].data.get("d")[0].f.outputSpeciesId != null
        content[0][0].data.get("d")[1].e == null
        content[0][0].data.get("d")[1].f.name == "sci name 2 (com name 2)"
        content[0][0].data.get("g").size() == 2
        content[0][0].data.get("g")[0].h.size() == 2
        content[0][0].data.get("g")[0].h[0] == "test gh 1"
        content[0][0].data.get("g")[0].h[1] == "test gh 2"
        content[0][0].data.get("g")[0].i.name == "sci name 1 (com name 1)"
        content[0][0].data.get("g")[0].i.scientificName == "sci name 1"
        content[0][0].data.get("g")[0].i.commonName == "com name 1"
        content[0][0].data.get("g")[0].i.guid == "gi1"
        content[0][0].data.get("g")[0].i.outputSpeciesId != null
        content[0][0].data.get("g")[1].h[0] == "test gh 3"
        content[0][0].data.get("g")[1].i.name == "sci name 3 (com name 3)"
        content[0][0].data.get("g")[1].i.scientificName == "sci name 3"
        content[0][0].data.get("g")[1].i.commonName == "com name 3"
        content[0][0].data.get("g")[1].i.guid == "gi3"
        content[0][0].data.get("g")[1].i.outputSpeciesId != null


        content[1][0].data.get("serial") == 2.0
        content[1][0].data.get("a") == "test a 2"
        content[1][0].data.get("b") == [[c: ["test bc 4", "test bc 5"]]]
        content[1][0].data.get("d")[0].e == "test de 2"
        content[1][0].data.get("d")[0].f.name == "sci name 4 (com name 4)"
        content[1][0].data.get("d")[0].f.scientificName == "sci name 4"
        content[1][0].data.get("d")[0].f.commonName == "com name 4"
        content[1][0].data.get("d")[0].f.guid == "id4"
        content[1][0].data.get("d")[0].f.outputSpeciesId != null
        content[1][0].data.get("g").size() == 2
        content[1][0].data.get("g")[0].h.size() == 1
        content[1][0].data.get("g")[0].h[0] == "test gh 4"
        content[1][0].data.get("g")[0].i.name == "sci name 4 (com name 4)"
        content[1][0].data.get("g")[0].i.scientificName == "sci name 4"
        content[1][0].data.get("g")[0].i.commonName == "com name 4"
        content[1][0].data.get("g")[0].i.guid == "gi4"
        content[1][0].data.get("g")[0].i.outputSpeciesId != null
        content[1][0].data.get("g")[1].h[0] == "test gh 5"
        content[1][0].data.get("g")[1].i.name == "sci name 5 (com name 5)"
        content[1][0].data.get("g")[1].i.scientificName == "sci name 5"
        content[1][0].data.get("g")[1].i.commonName == "com name 5"
        content[1][0].data.get("g")[1].i.guid == "gi5"
        content[1][0].data.get("g")[1].i.outputSpeciesId != null
    }
}
