package au.org.ala.ecodata


import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification


/**
 * Specification for the ReportService.
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ReportService)
class ReportServiceSpec extends Specification {

    MetadataService metadataService = Stub(MetadataService)
    ElasticSearchService elasticSearchService = Stub(ElasticSearchService)
    OutputService outputService = Stub(OutputService)


    def setupSpec() {
        Output.metaClass.static.withNewSession = {Closure c -> c.call() }
    }

    def setup() {

        service.elasticSearchService = elasticSearchService
        service.metadataService = metadataService
        service.outputService = outputService
    }

    def setupInputs(outputs, activities, outputData) {
        Map model = [outputs:outputs]
        Map dataModel = [dataModel:[[type:'string', name:'test'], [type:'list', name:'nested', columns:[]]]]

        metadataService.activitiesModel() >> model
        metadataService.getOutputDataModel(_) >> dataModel

        def activityDocs = activities.collect{[source:it]}
        elasticSearchService.searchActivities(_, _, _) >> [hits:[totalHits:activityDocs.size(), hits:activityDocs]]

        outputService.findAllForActivityId(_, _) >> {activityId, levelOfDetail -> outputData[activityId]}
    }

    def "the sum of a single property can be reported"() {
        given:
        def output = "output"
        def property = "prop"
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:property]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = values.collect{[activityId:it.key]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:
        results.outputData[0].result == values.values().sum()
        results.metadata.activities == 5
        results.metadata.projects.size() == 1

    }

    def "the sum of a property grouped by another property can be reported"() {
        given:
        def output = "output"
        def property = "prop"
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:property, groupBy:"activity:mainTheme"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:

        results.outputData[0].groups[0].group == "theme1"
        results.outputData[0].groups[0].count == 3
        results.outputData[0].groups[0].results[0].result == 1+2+5

        results.outputData[0].groups[1].results[0].result == 3+4
        results.outputData[0].groups[1].group == "theme2"
        results.outputData[0].groups[1].count == 2

        results.metadata.activities == 5
        results.metadata.projects.size() == 1
    }

    def "the filtered sum of a property can be reported"() {

        given:
        def output = "output"
        def property = "prop"
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:property, groupBy:"activity:mainTheme", filterBy:'theme1']

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:
        results.outputData.size() == 1

        def expected = 1+2+5

        results.outputData[0].result == expected

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()
    }

    def "test nested group sum aggregation"() {

        setup:
        def output = "output"
        def property = "prop"
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, listName:'nested', outputName:output, label:property, groupBy:"output:group"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:"group1"], [prop:2, group:"group2"]],
                      2:[[prop:10, group:"group1"], [prop:12, group:"group2"]],
                      3:[[prop:3, group:"group3"]]
        ]

        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, "nested", it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:
        results.outputData.size() == 1
        results.outputData[0].groups.size() == 3

        results.outputData[0].groups[0].results[0].result == 11
        results.outputData[0].groups[0].group == "group1"
        results.outputData[0].groups[0].count == 2

        results.outputData[0].groups[1].results[0].result == 14
        results.outputData[0].groups[1].group == "group2"
        results.outputData[0].groups[1].count == 2

        results.outputData[0].groups[2].results[0].result == 3
        results.outputData[0].groups[2].group == "group3"
        results.outputData[0].groups[2].count == 1

        assertEquals 5, results.metadata.activities
        assertEquals 1, results.metadata.projects.size()

    }

    def "testFilteredSumAggregationOfListAttribute"() {

        setup:
        def output = "output"
        def property = "prop"
        def list = 'nested'
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, listName: list, label:property, groupBy:"output:group", filterBy:'group1']

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:
        results.outputData.size() == 1


        def expected = 1+3+2+3
        results.outputData[0].result == expected
        results.outputData[0].count == 4

        results.metadata.activities == 5
        results.metadata.projects.size() == 1
    }

    def "testGroupByMultiValueField"() {
        setup:
        def output = "output"
        def property = "prop"
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, listName:'nested', outputName:output, label:property, groupBy:"output:group"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:["group1", "group2"]], [prop:2, group:["group2", "group3"]]],
                      2:[[prop:10, group:["group1"]], [prop:12, group:["group2"]]],
                      3:[[prop:3, group:["group3", "group2"]]]
        ]

        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, "nested", it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:
        results.outputData.size() == 1
        results.outputData[0].groups.size() == 3

        results.outputData[0].groups[0].results[0].result == 11
        results.outputData[0].groups[0].group == "group1"
        results.outputData[0].groups[0].count == 2

        results.outputData[0].groups[1].results[0].result == 18
        results.outputData[0].groups[1].group == "group2"
        results.outputData[0].groups[1].count == 4

        results.outputData[0].groups[2].results[0].result == 5
        results.outputData[0].groups[2].group == "group3"
        results.outputData[0].groups[2].count == 2

        results.metadata.activities == 5
        results.metadata.projects.size() == 1

    }

    def "testTopLevelGrouping"() {
        setup:
        def output = "output"
        def property = "prop"
        def list = 'nested'
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, listName: list, label:property, groupBy:"output:group"]

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [[score:new au.org.ala.ecodata.reporting.Score(score)]], [type:'discrete', property:'activity.mainTheme'])

        then:
        results.outputData.groups.size() == 2
        results.outputData.groups[0].group == 'theme1'

        def theme1Results = results.outputData.groups[0].results[0].groups
        theme1Results.size() == 4

        [group1:6, group2:4, group3:2, group4:5].each { k, v ->
            def nestedResult = theme1Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

        results.outputData.groups[1].group == 'theme2'
        def theme2Results = results.outputData.groups[1].results[0].groups
        [group1:3, group2:6].each { k, v ->
            def nestedResult = theme2Results.find{it.group == k}
            nestedResult.results[0].result == v
        }
    }

    def "testTopLevelDateGrouping"() {
        setup:
        def output = "output"
        def property = "prop"
        def list = 'nested'
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, listName: list, label:property, groupBy:"output:group"]

        def activityDates = [1:"2013-01-02T00:00:00Z", 2:"2014-01-02T00:00:00Z", 3:"2014-07-02T00:00:00Z", 4:"2015-01-01T00:00:00Z", 5:"2015-01-02T00:00:00Z"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = activityDates.collect{[activityId:it.key, plannedEndDate:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [[score:new au.org.ala.ecodata.reporting.Score(score)]], [property:'activity.plannedEndDate', type:'date', buckets:['2014-01-01T00:00:00Z', '2015-01-01T00:00:00Z'], format:'MMM yyyy'])

        then:
        results.outputData.groups[0].group == 'Before Jan 2014'
        def group1Results = results.outputData.groups[0].results[0].groups

        [group1:1, group2:2].each { k, v ->
            def nestedResult = group1Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

        results.outputData.groups[1].group == 'Jan 2014 - Dec 2014'
        def group2Results = results.outputData.groups[1].results[0].groups

        [group1:3, group2:4, group3:2].each { k, v ->
            def nestedResult = group2Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

        results.outputData.groups[2].group == 'After Dec 2014'
        def group3Results = results.outputData.groups[2].results[0].groups
        [group1:5, group2:4, group4:5].each { k, v ->
            def nestedResult = group3Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

    }

    def "scores with the same label should produce a single result"() {
        given:
        def output = "output"
        def property = "prop"
        def property2 = "prop2"
        String label = "label"
        def score = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property, outputName:output, label:label]
        def score2 = [aggregationType:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM, name:property2, outputName:output, label:label]

        def values = [1:[(property):1, (property2):6], 2:[(property):2, (property2):7],3:[(property):3, (property2):8],
                      4:[(property):4, (property2):9],5:[(property):5, (property2):10]]

        def activities = values.collect{[activityId:it.key]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, it.value)]]}
        setupInputs([[name:output, scores:[score, score2]]], activities, outputs)

        when:
        def results = service.aggregate([])

        then:
        results.outputData[0].result == values.values().sum{it[property]+it[property2]}
        results.metadata.activities == 5
        results.metadata.projects.size() == 1

    }

    def createOutput(activityId, name, property, value) {
        return [activityId:activityId, name:name, data:[(property):value]]
    }

    def createOutput(activityId, name, Map data) {
        return [activityId:activityId, name:name, data:data]
    }
}
