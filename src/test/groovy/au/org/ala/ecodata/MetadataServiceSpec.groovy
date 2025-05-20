package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.springframework.context.MessageSource

class MetadataServiceSpec extends MongoSpec implements ServiceUnitTest<MetadataService> {

    WebService webService = Mock(WebService)
    SettingService settingService = Mock(SettingService)
    ActivityFormService activityFormService = Mock(ActivityFormService)
    MessageSource messageSource = Mock(MessageSource)
    HubService hubService = Mock(HubService)

    def setupSpec() {
        setupTerms()
        setupInvestmentPriorities()
    }

    private void setupTerms() {
        // Mock test data
        new Term(termId: 't1', term: "testTerm1", category: "testCategory", hubId: "hub1", status: Status.ACTIVE).save(flush: true, failOnError:true)
        new Term(termId: 't2', term: "testTerm2", category: "testCategory", hubId: "hub1", status: Status.ACTIVE).save(flush: true, failOnError:true)
        new Term(termId: 't3', term: "testTerm3", category: "otherCategory", hubId: "hub1", status: Status.ACTIVE).save(flush: true, failOnError:true)
        new Term(termId: 't4', term: "testTerm4", category: "testCategory", hubId: "hub2", status: Status.ACTIVE).save(flush: true, failOnError:true)
        new Term(termId: 't5', term: "testTerm5", category: "testCategory", hubId: "hub1", status: Status.DELETED).save(flush: true, failOnError:true)
    }
    
    private void setupInvestmentPriorities() {
        new InvestmentPriority(investmentPriorityId: 'ip1', name: "testInvestmentPriority1", categories: ["testCategory"], hubId: "hub1", status: Status.ACTIVE).save(flush: true, failOnError: true)
        new InvestmentPriority(investmentPriorityId: 'ip2', name: "testInvestmentPriority2", categories: ["testCategory"], hubId: "hub1", status: Status.ACTIVE).save(flush: true, failOnError: true)
        new InvestmentPriority(investmentPriorityId: 'ip3', name: "testInvestmentPriority3", categories: ["otherCategory"], hubId: "hub1", status: Status.ACTIVE).save(flush: true, failOnError: true)
        new InvestmentPriority(investmentPriorityId: 'ip4', name: "testInvestmentPriority4", categories: ["testCategory2"], hubId: "hub2", status: Status.ACTIVE).save(flush: true, failOnError: true)
        new InvestmentPriority(investmentPriorityId: 'ip5', name: "testInvestmentPriority5", categories: ["testCategory2"], hubId: "hub1", status: Status.DELETED).save(flush: true, failOnError: true)
    }

    def setup() {
        service.grailsApplication = grailsApplication
        grailsApplication.config.google = [geocode: [url: 'url'], api: [key:'abc']]
        grailsApplication.config.spatial= [intersectUrl: 'url']
        grailsApplication.config.app = [facets: [ geographic: [contextual: ['state':'cl927', 'cmz':'cl2112'], grouped: [other:['australian_coral_ecoregions':'cl917'], gerSubRegion:['gerBorderRanges':'cl1062']], special: [:]]]]
        service.settingService = settingService
        service.webService = webService
        service.activityFormService = activityFormService
        service.messageSource = messageSource
        service.hubService = hubService

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    def cleanup() {
        Service.findAll().each { it.delete() }
        Score.findAll().each{ it.delete() }
        ActivityForm.findAll().each{ it.delete() }
        Program.findAll().each { it.delete() }
    }

    def cleanupSpec() {
        Term.findAll().each { it.delete(flush:true) }
        InvestmentPriority.findAll().each { it.delete(flush:true) }
    }

    private void setupServices() {
        for (int i in 1..4) {
            Service service = new Service(legacyId:i, serviceId:'s'+i, name:'Service '+i)
            service.setOutputs([new ServiceForm(formName:"form 1", sectionName: "section 1")])
            service.insert(flush:true)

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

    def "The MetadataService can return a categorized list of activities filtered by the Program configuration"() {
        setup:
        JSONArray jsonArrayActivities = new JSONArray()
        JSONObject jsonObjectActivity = new JSONObject()
        jsonObjectActivity.put("name","test1")
        jsonArrayActivities.push(jsonObjectActivity)

        String programId = '123'
        Program program = new Program(programId:programId, name: 'test 123', description: 'description 1',
            config:[excludes:["excludes",["DATA_SETS", "MERI_PLAN"]], projectReports:["reportType":"Activity"], activities:jsonArrayActivities])
        program.save(flush:true, failOnError: true)

        ActivityForm form1 = new ActivityForm(name: 'test1', formVersion: 1, status: Status.ACTIVE, type: 'Activity', publicationStatus: PublicationStatus.DRAFT, category:"C1")

        List activityForms = [form1]

        when:
        Map result = service.activitiesListByProgramId(programId)

        then:
        1 * activityFormService.search([publicationStatus:PublicationStatus.PUBLISHED, name:["test1"]]) >> activityForms
        1 * messageSource.getMessage("api.test1.description", null, "", Locale.default) >> "test1 description"
        result == ["C1":[[name:form1.name, type:form1.type, description:"test1 description"]]]

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
        def resp = service.excelWorkbookToMap(file, 'form1', true, null)
        def content = resp.success

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

    def "excelWorkbookToMap: getting content from bulk_import_example_error.xlsx should result in an error"() {
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
        def file = new File("src/test/resources/bulk_import_example_error.xlsx").newInputStream()

        when:
        def content = service.excelWorkbookToMap(file, 'form1', true, null)

        then:
        messageSource.getMessage("bulkimport.conversionToObjectError", _, "", Locale.default) >> { code, array, msg, locale-> "Error parsing data into an object in serial number ${array[0]}" }
        messageSource.getMessage("bulkimport.errorGroupBySerialNumber", _, "", Locale.default) >> { code, array, msg, locale-> "Error making nested object from multiple rows in serial number ${array[0]}" }
        messageSource.getMessage("bulkimport.conversionToFormSectionError", _, "", Locale.default) >> { code, array, msg, locale-> "Error parsing data for form section (output) ${array[0]}" }
        content.error != null
        content.error.contains("300")
        content.error.contains("55")
    }
  
    void "getGeographicConfig should get geographic configuration either from hub or system default"() {
        when:
        def result = service.getGeographicConfig()

        then:
        1 * hubService.getCurrentHub(_) >> new Hub(geographicConfig: [contextual: ['state':'cl900', 'cmz':'cl200'], grouped: [other:['australian_coral_ecoregions':'cl97'], gerSubRegion:['gerBorderRanges':'cl102']]])
        result.contextual == ['state':'cl900', 'cmz':'cl200']
        result.grouped == [other:['australian_coral_ecoregions':'cl97'], gerSubRegion:['gerBorderRanges':'cl102']]

        when:
        result = service.getGeographicConfig()

        then:
        1 * hubService.getCurrentHub(_) >> null
        result.size() == 3
        result.contextual == ['state':'cl927', 'cmz':'cl2112']
        result.grouped == [other: ['australian_coral_ecoregions':'cl917'], gerSubRegion: ['gerBorderRanges':'cl1062']]
        result.special == [:]
    }
    def "findTermsByCategory should return terms filtered by category and hubId"() {

        when: "findTermsByCategory is called with a category and hubId"
        List<Term> result = service.findTermsByCategory("testCategory", "hub1")

        then: "Only terms matching the category and hubId are returned, excluding deleted terms"
        result.size() == 2
        result*.term.containsAll(["testTerm1", "testTerm2"])
    }

    def "findTermsByCategory should return terms filtered by category only when hubId is null"() {

        when: "findTermsByCategory is called with a category and no hubId"
        List<Term> result = service.findTermsByCategory("testCategory", null)

        then: "Only terms matching the category are returned, excluding deleted terms"
        result.size() == 3
        result*.term.containsAll(["testTerm1", "testTerm2", "testTerm4"])
    }

    def "The metadataservice will update the status to deleted when asked to delete a term"() {
        setup:
        String termId = 't2'
        when:
        Term deletedTerm = service.deleteTerm(termId)

        then:
        deletedTerm.status == Status.DELETED
        Term.findByTermId(termId).status == Status.DELETED

    }

    def "A Term can be updated"() {
        setup:
        Term term = new Term(termId: 't1', term: "testTerm - updated", hubId: "hub1", category: "testCategory")

        when:
        Term updatedTerm = service.updateTerm(term.properties)
        Term.withSession{
            it.clear()
        }

        then:
        !updatedTerm.hasErrors()
        Term.findByTermId('t1').term == "testTerm - updated"

    }

    def "A Term hubId should not be able to be overwritten once set"() {
        setup:
        Term term = new Term(termId: 't1', term: "testTerm - updated", hubId: "hub2", category: "testCategory")

        when:
        Term updatedTerm = service.updateTerm(term.properties)

        then:
        updatedTerm.hasErrors()
        updatedTerm.errors.getFieldErrorCount('hubId') == 1

    }

    /**
     * This test is because the unique constraint on Terms prevents multiple deleted terms in the
     * same category/term/hubId
     */
    def "A Term can be deleted more than once"() {
        setup:
        Term term = new Term(termId: 't3', term: "testTerm3", category: "otherCategory", hubId: "hub1", status: Status.ACTIVE)

        when:
        Term updatedTerm = service.deleteTerm(term.termId)

        then:
        !updatedTerm.hasErrors()

        when: "We re-add then re-delete the Term"
        Term newTerm = service.updateTerm([term:term.term, description: term.description, category: term.category, hubId: term.hubId])
        Term deletedTerm = service.deleteTerm(newTerm.termId)

        then:
        !deletedTerm.hasErrors()

    }


    def "findInvestmentPrioritiesByCategory should investment priorities filtered by category"() {

        when: "findInvestmentPrioritiesByCategory is called with a category"
        List<Term> result = service.findInvestmentPrioritiesByCategory(["testCategory"])

        then: "Only investment priorities matching the category are returned, excluding deleted ones"
        result.size() == 2
        result*.name.containsAll(["testInvestmentPriority1", "testInvestmentPriority2"])

        when: "findInvestmentPrioritiesByCategory is called with a multiple categories"
        result = service.findInvestmentPrioritiesByCategory(["testCategory", "otherCategory"])

        then: "Investment priorities matching either category are returned, excluding deleted ones"
        result.size() == 3
        result*.name.containsAll(["testInvestmentPriority1", "testInvestmentPriority2", "testInvestmentPriority3"])

    }

    def "The metadataservice will update the status to deleted when asked to delete a investment priority"() {
        setup:
        String investmentPriorityId = 'ip2'
        when:
        InvestmentPriority deletedInvestmentPriority = service.deleteInvestmentPriority(investmentPriorityId)

        then:
        deletedInvestmentPriority.status == Status.DELETED
        InvestmentPriority.findByInvestmentPriorityId(investmentPriorityId).status == Status.DELETED

    }

    def "An investment priority can be updated"() {
        setup:
        InvestmentPriority investmentPriority = new InvestmentPriority(investmentPriorityId: 'ip1', name: "Investment priority - updated", hubId: "hub1", categories: ["testCategory"])

        when:
        InvestmentPriority updatedInvestmentPriority = service.updateInvestmentPriority(investmentPriority.properties)
        InvestmentPriority.withSession{
            it.clear()
        }

        then:
        !updatedInvestmentPriority.hasErrors()
        InvestmentPriority.findByInvestmentPriorityId(investmentPriority.investmentPriorityId).name == investmentPriority.name

    }

    def "An InvestmentPriority hubId should not be able to be overwritten once set"() {
        setup:
        InvestmentPriority investmentPriority = new InvestmentPriority(investmentPriorityId: 'ip1', name: "Investment priority - updated", hubId: "hub2", categories: ["testCategory"])

        when:
        InvestmentPriority updatedInvestmentPriority = service.updateInvestmentPriority(investmentPriority.properties)

        then:
        updatedInvestmentPriority.hasErrors()
        updatedInvestmentPriority.errors.getFieldErrorCount('hubId') == 1

    }

    /**
     * This test is because the unique constraint on InvestmentPriority name/status prevents multiple deleted investment priorities in the
     * same name
     */
    def "An InvestmentPriority can be deleted more than once"() {
        setup:
        InvestmentPriority investmentPriority = new InvestmentPriority(investmentPriorityId: 'ip3', name: "investmentPriority3", category: "otherCategory", hubId: "hub1", status: Status.ACTIVE)

        when:
        InvestmentPriority updatedInvestmentPriority = service.deleteInvestmentPriority(investmentPriority.investmentPriorityId)

        then:
        !updatedInvestmentPriority.hasErrors()

        when: "We re-add then re-delete the InvestmentPriority"
        InvestmentPriority newInvestmentPriority = service.updateInvestmentPriority([name:investmentPriority.name, description: investmentPriority.description, categories: investmentPriority.categories, hubId: investmentPriority.hubId])
        InvestmentPriority deletedInvestmentPriority = service.deleteInvestmentPriority(newInvestmentPriority.investmentPriorityId)

        then:
        !deletedInvestmentPriority.hasErrors()

    }

}
