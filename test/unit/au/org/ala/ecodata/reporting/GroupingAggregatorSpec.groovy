package au.org.ala.ecodata.reporting

import spock.lang.Specification

/**
 * Specification for the GroupingAggregator.
 */
class GroupingAggregatorSpec extends Specification {

    def "multiple aggregrations can be performed on grouped data"() {

        given:
        GroupingConfig groupingConfig = new GroupingConfig(property:"group", type:"discrete")
        Aggregation score = new Aggregation(property:"property", type:"SUM")
        Aggregation score2 = new Aggregation(property:"property", type:"AVERAGE")
        AggregationConfig aggregationConfig = new GroupingAggregationConfig(label:"test", childAggregations: [score, score2], groups:groupingConfig)

        GroupingAggregator groupingAggregator = new GroupingAggregator(aggregationConfig)


        List<Map> data = [
                [property:1, group:"group1"], [property:2, group:"group1"], [property:3, group:"group1"], [property:4, group:"group1"],
                [property:5, group:"group2"], [property:6, group:"group2"], [property:7, group:"group2"], [property:8, group:"group3"]
        ]

        when:
        data.each {
            groupingAggregator.aggregate(it)
        }
        AggregationResult result = groupingAggregator.result()

        then:
        result.label == aggregationConfig.label
        result.count == data.size()

        List<AggregationResult> groups = ((GroupedAggregationResult)result).groups
        groups.size() == 3

        groups[0].group == "group1"
        groups[0].count == 4
        groups[0].results[0].result == 10
        groups[0].results[1].result == 10/4

        groups[1].group == "group2"
        groups[1].count == 3
        groups[1].results[0].result == 18
        groups[1].results[1].result == 18/3

        groups[2].group == "group3"
        groups[2].count == 1
        groups[2].results[0].result == 8
        groups[2].results[1].result == 8


    }

    def "child aggregations are not required if only a count is needed"() {
        given:
        GroupingConfig groupingConfig = new GroupingConfig(property:"group", type:"discrete")
        AggregationConfig aggregationConfig = new GroupingAggregationConfig(label:"test", groups:groupingConfig)

        GroupingAggregator groupingAggregator = new GroupingAggregator(aggregationConfig)


        List<Map> data = [
                [property:1, group:"group1"], [property:2, group:"group1"], [property:3, group:"group1"], [property:4, group:"group1"],
                [property:5, group:"group2"], [property:6, group:"group2"], [property:7, group:"group2"], [property:8, group:"group3"]
        ]

        when:
        data.each {
            groupingAggregator.aggregate(it)
        }
        AggregationResult result = groupingAggregator.result()

        then:
        result.label == aggregationConfig.label
        result.count == data.size()

        List<AggregationResult> groups = ((GroupedAggregationResult)result).groups
        groups.size() == 3

        groups[0].group == "group1"
        groups[0].count == 4

        groups[1].group == "group2"
        groups[1].count == 3

        groups[2].group == "group3"
        groups[2].count == 1
    }

    def "the grouping value can be a list"() {
        given:
        GroupingConfig groupingConfig = new GroupingConfig(property:"group", type:"discrete")
        AggregationConfig aggregationConfig = new GroupingAggregationConfig(label:"test", groups:groupingConfig)

        GroupingAggregator groupingAggregator = new GroupingAggregator(aggregationConfig)


        List<Map> data = [
                [property:1, group:["group1", "group2"]], [property:2, group:"group1"], [property:3, group:"group1"], [property:4, group:"group1"],
                [property:5, group:"group2"], [property:6, group:["group2", "group3"]], [property:7, group:"group2"], [property:8, group:"group3"]
        ]

        when:
        data.each {
            groupingAggregator.aggregate(it)
        }
        AggregationResult result = groupingAggregator.result()

        then:
        result.label == aggregationConfig.label
        result.count == data.size()

        List<AggregationResult> groups = ((GroupedAggregationResult)result).groups
        groups.size() == 3

        groups[0].group == "group1"
        groups[0].count == 4

        groups[1].group == "group2"
        groups[1].count == 4

        groups[2].group == "group3"
        groups[2].count == 2
    }

}
