package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.Score
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import junit.framework.TestCase
import org.junit.Before

@TestMixin(ControllerUnitTestMixin) // Used to register JSON converters.
class ReportServiceTests extends TestCase {

    ReportService service

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
        elasticSearchServiceMock.demand.searchActivities(2) {filters, searchTerm, pagination ->
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
        assertEquals values.values().sum() as Double, results.outputData[0].results[0].result, 0

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()


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

        def expected = 1+2+5
        assertEquals expected as Double, results.outputData[0].results[0].result, 0
        assertEquals "theme1", results.outputData[0].results[0].group
        assertEquals 3, results.outputData[0].results[0].count

        expected = 3+4
        assertEquals expected as Double, results.outputData[0].results[1].result, 0
        assertEquals "theme2", results.outputData[0].results[1].group
        assertEquals 2, results.outputData[0].results[1].count

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()


    }

    void testFilteredSumAggregation() {

        def output = "output"
        def property = "prop"
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:property, groupBy:"activity:mainTheme", filterBy:'theme1']

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([])

        assertEquals(1, results.outputData.size())
        assertEquals 1, results.outputData.results.size()

        def expected = 1+2+5
        assertEquals expected as Double, results.outputData[0].results[0].result, 0
        assertEquals "", results.outputData[0].results[0].group // Groups are removed from filtered scores.
        assertEquals 3, results.outputData[0].results[0].count

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()
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

        assertEquals new Double(11), results.outputData[0].results[0].result, 0
        assertEquals "group1", results.outputData[0].results[0].group
        assertEquals 2, results.outputData[0].results[0].count

        assertEquals new Double(14), results.outputData[0].results[1].result, 0
        assertEquals "group2", results.outputData[0].results[1].group
        assertEquals 2, results.outputData[0].results[1].count

        assertEquals new Double(3), results.outputData[0].results[2].result, 0
        assertEquals "group3", results.outputData[0].results[2].group
        assertEquals 1, results.outputData[0].results[2].count

        assertEquals 3, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()


    }


    void testFilteredSumAggregationOfListAttribute() {

        def output = "output"
        def property = "prop"
        def list = 'mylist'
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, listName: list, label:property, groupBy:"output:group", filterBy:'group1']

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([])

        assertEquals(1, results.outputData.size())
        assertEquals 1, results.outputData.results.size()

        def expected = 1+3+2+3
        assertEquals expected as Double, results.outputData[0].results[0].result, 0
        assertEquals "", results.outputData[0].results[0].group // Groups are removed from filtered scores.
        assertEquals 4, results.outputData[0].results[0].count

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()
    }

    void testGroupByMultiValueField() {
        def output = "output"
        def property = "prop"
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, listName:'nested', outputName:output, label:property, groupBy:"output:group"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:["group1", "group2"]], [prop:2, group:["group2", "group3"]]],
                      2:[[prop:10, group:["group1"]], [prop:12, group:["group2"]]],
                      3:[[prop:3, group:["group3", "group2"]]]
        ]

        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, "nested", it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([])

        assertEquals(1, results.outputData.size())
        assertEquals 3, results.outputData[0].results.size()

        assertEquals new Double(11), results.outputData[0].results[0].result, 0
        assertEquals "group1", results.outputData[0].results[0].group
        assertEquals 2, results.outputData[0].results[0].count

        assertEquals new Double(18), results.outputData[0].results[1].result, 0
        assertEquals "group2", results.outputData[0].results[1].group
        assertEquals 4, results.outputData[0].results[1].count

        assertEquals new Double(5), results.outputData[0].results[2].result, 0
        assertEquals "group3", results.outputData[0].results[2].group
        assertEquals 2, results.outputData[0].results[2].count

        assertEquals 3, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()

    }

    void testTopLevelGrouping() {
        def output = "output"
        def property = "prop"
        def list = 'mylist'
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, listName: list, label:property, groupBy:"output:group"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([], null, [[score:new Score(score)]], [entity:'activity', property:'mainTheme'])
        assertEquals 2, results.outputData.size()
        assertEquals 'theme1', results.outputData[0].group

        def theme1Results = results.outputData[0].results[0].results
        assertEquals 4, theme1Results.size()
        def groupResults = [group1:6, group2:4, group3:2, group4:5]
        groupResults.each { k, v ->
            def nestedResult = theme1Results.find{it.group == k}
            assertEquals new Double(v), nestedResult.result, 0
        }

        assertEquals 'theme2', results.outputData[1].group
        def theme2Results = results.outputData[1].results[0].results
        groupResults = [group1:3, group2:6]
        groupResults.each { k, v ->
            def nestedResult = theme2Results.find{it.group == k}
            assertEquals new Double(v), nestedResult.result, 0
        }
    }

    void testTopLevelDateGrouping() {
        def output = "output"
        def property = "prop"
        def list = 'mylist'
        def score = [aggregationType:Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, listName: list, label:property, groupBy:"output:group"]

        def activityDates = [1:"2013-01-02T00:00:00Z", 2:"2014-01-02T00:00:00Z", 3:"2014-07-02T00:00:00Z", 4:"2015-01-01T00:00:00Z", 5:"2015-01-02T00:00:00Z"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = activityDates.collect{[activityId:it.key, plannedEndDate:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        def results = service.aggregate([], null, [[score:new Score(score)]], [entity:'activity', property:'plannedEndDate', type:'date', buckets:['2014-01-01T00:00:00Z', '2015-01-01T00:00:00Z'], format:'MMM yyyy'])
        assertEquals 'Before Jan 2014', results.outputData[0].group
        def group1Results = results.outputData[0].results[0].results
        def groupResults = [group1:1, group2:2]
        groupResults.each { k, v ->
            def nestedResult = group1Results.find{it.group == k}
            assertEquals new Double(v), nestedResult.result, 0
        }

        assertEquals 'Jan 2014 - Dec 2014', results.outputData[1].group
        def group2Results = results.outputData[1].results[0].results
        groupResults = [group1:3, group2:4, group3:2]
        groupResults.each { k, v ->
            def nestedResult = group2Results.find{it.group == k}
            assertEquals new Double(v), nestedResult.result, 0
        }

        assertEquals 'After Dec 2014', results.outputData[2].group
        def group3Results = results.outputData[2].results[0].results
        groupResults = [group1:5, group2:4, group4:5]
        groupResults.each { k, v ->
            def nestedResult = group3Results.find{it.group == k}
            assertEquals new Double(v), nestedResult.result, 0
        }

    }


    def createOutput(activityId, name, property, value) {
        return [activityId:activityId, name:name, data:[(property):value]]
    }

}
