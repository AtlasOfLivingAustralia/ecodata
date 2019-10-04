package au.org.ala.ecodata.reporting

import spock.lang.Specification

/**
 * Specification for the CompositeAggregator
 */
class FilteredAggregatorSpec extends Specification {

    def "a single result can be produced from the sum of filtered data"() {

        given:
        Aggregation score = new Aggregation(property:"property", type:"SUM")
        GroupingConfig filter = new GroupingConfig(property:"group", filterValue: "group1", type:"discrete")
        FilteredAggregationConfig aggregationConfig = new FilteredAggregationConfig(label:"test", childAggregations: [score], filter:filter)

        FilteredAggregator filteredAggregator = new FilteredAggregator(aggregationConfig)

        List<Map> data = [
                [property:1, group:"group1"], [property:2, group:"group1"], [property:3, group:"group1"], [property:4, group:"group1"],
                [property:5, group:"group2"], [property:6, group:"group2"], [property:7, group:"group2"], [property:8, group:"group3"]
        ]

        when:
        data.each {
            filteredAggregator.aggregate(it)
        }
        AggregationResult result = filteredAggregator.result()

        then:
        result.label == aggregationConfig.label
        result.count == 4
        result.result == 10
    }

    def "multiple sub-aggregations can be produced from filtered data"() {

        given:
        Aggregation score = new Aggregation(property:"property", type:"SUM")
        Aggregation score2 = new Aggregation(property:"property", type:"AVERAGE")

        GroupingConfig filter = new GroupingConfig(property:"group", filterValue: "group1", type:"filter")
        FilteredAggregationConfig aggregationConfig = new FilteredAggregationConfig(label:"test", childAggregations: [score, score2], filter:filter)

        FilteredAggregator filteredAggregator = new FilteredAggregator(aggregationConfig)

        List<Map> data = [
                [property:1, group:"group1"], [property:2, group:"group1"], [property:3, group:"group1"], [property:4, group:"group1"],
                [property:5, group:"group2"], [property:6, group:"group2"], [property:7, group:"group2"], [property:8, group:"group3"]
        ]

        when:
        data.each {
            filteredAggregator.aggregate(it)
        }
        AggregationResult result = filteredAggregator.result()

        then:
        result.label == aggregationConfig.label
        result.count == 4

        List<AggregationResult> groups = ((GroupedAggregationResult)result).groups

        groups.size() == 1
        groups[0].group == filter.filterValue

        groups[0].count == 4
        groups[0].results.size() == 2
        groups[0].results[0].result == 10
        groups[0].results[1].result == 10/4
    }

    def "filters can be configured to exclude a value rather than select a value"() {
        given:
        Aggregation score = new Aggregation(property:"property", type:"SUM")

        GroupingConfig filter = new GroupingConfig(property:"group", filterValue: "!group1", type:"filter")
        FilteredAggregationConfig aggregationConfig = new FilteredAggregationConfig(label:"test", childAggregations: [score], filter:filter)

        FilteredAggregator filteredAggregator = new FilteredAggregator(aggregationConfig)

        List<Map> data = [
                [property:1, group:"group1"], [property:2, group:"group1"], [property:3, group:"group1"], [property:4, group:"group1"],
                [property:5, group:"group2"], [property:6, group:"group2"], [property:7, group:"group2"], [property:8, group:"group3"]
        ]

        when:
        data.each {
            filteredAggregator.aggregate(it)
        }
        SingleResult result = filteredAggregator.result()

        then: "group1 values are excluded from the result"
        result.label == aggregationConfig.label
        result.count == 4

        result.result == 5+6+7+8
    }
}
