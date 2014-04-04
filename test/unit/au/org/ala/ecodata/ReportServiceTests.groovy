package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.Score
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import junit.framework.TestCase
import org.junit.Before

@TestMixin(ControllerUnitTestMixin) // Used to register JSON converters.
class ReportServiceTests extends TestCase {

    def service

    @Before
    public void setUp() {
        service = new ReportService()

    }
    def setupInputs(outputs, activities, outputData) {

        def model = [outputs:outputs]

        Output.metaClass.static.withNewSession = {Closure c -> c.call() }

        def metadataService = mockFor(MetadataService)
        metadataService.demand.activitiesModel {->model}
        service.metadataService = metadataService.createMock()

        def activityDocs = activities.collect{[source:it]}
        def elasticSearchServiceMock = mockFor(ElasticSearchService, true)
        elasticSearchServiceMock.demand.searchActivities(2) {filters, pagination ->
            [hits:[totalHits:activityDocs.size(), hits:activityDocs]]
        }
        service.elasticSearchService = elasticSearchServiceMock.createMock()

        def outputServiceMock = mockFor(OutputService, true)
        outputServiceMock.demand.findAllForActivityId(activityDocs.size()) {activityId, levelOfDetail -> outputData[activityId]}
        service.outputService = outputServiceMock.createMock()

    }


    void testSimpleSumAggregation() {

        def output = "output"
        def property = "prop"
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:property]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = values.collect{[activityId:it.key]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([])

        assertEquals(1, results.outputData.size())
        assertEquals values.values().sum(), results.outputData[0].results[0].result

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects


    }

    void testGroupedSumAggregation() {

        def output = "output"
        def property = "prop"
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:property, groupBy:"activity:mainTheme"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([])

        assertEquals(1, results.outputData.size())
        assertEquals 1, results.outputData.results.size()

        assertEquals 1+2+5, results.outputData[0].results[0].result
        assertEquals "theme1", results.outputData[0].results[0].group
        assertEquals 3, results.outputData[0].results[0].count

        assertEquals 3+4, results.outputData[0].results[1].result
        assertEquals "theme2", results.outputData[0].results[1].group
        assertEquals 2, results.outputData[0].results[1].count

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects


    }

    void testNestedGroupedSumAggregation() {

        def output = "output"
        def property = "prop"
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, listName:'nested', outputName:output, label:property, groupBy:"output:group"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:"group1"], [prop:2, group:"group2"]],
                      2:[[prop:10, group:"group1"], [prop:12, group:"group2"]],
                      3:[[prop:3, group:"group3"]]
                     ]

        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, "nested", it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([])

        assertEquals(1, results.outputData.size())
        assertEquals 3, results.outputData[0].results.size()

        assertEquals 11, results.outputData[0].results[0].result
        assertEquals "group1", results.outputData[0].results[0].group
        assertEquals 2, results.outputData[0].results[0].count

        assertEquals 14, results.outputData[0].results[1].result
        assertEquals "group2", results.outputData[0].results[1].group
        assertEquals 2, results.outputData[0].results[1].count

        assertEquals 3, results.outputData[0].results[2].result
        assertEquals "group3", results.outputData[0].results[2].group
        assertEquals 1, results.outputData[0].results[2].count

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects


    }


    void testGroupedSumAggregationNoResults() {

    }


    def createOutput(activityId, name, property, value) {
        return [activityId:activityId, name:name, data:[(property):value]]
    }

}
