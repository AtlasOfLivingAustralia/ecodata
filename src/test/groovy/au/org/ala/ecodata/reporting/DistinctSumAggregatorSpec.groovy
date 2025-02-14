package au.org.ala.ecodata.reporting

import spock.lang.Specification

class DistinctSumAggregatorSpec extends Specification {

    // write a test for the DistinctSumAggregator
    def "The DistinctSumAggregator can sum the distinct values of a property"() {
        given:
        Map config = [
            label:"value1",
            "property": "data.value1",
            "type": "DISTINCT_SUM",
            "keyProperty": "data.group"
        ]
        Aggregators.DistinctSumAggregator aggregator = new AggregatorFactory().createAggregator(config)

        when:
        aggregator.aggregate([data:[value1:1, group: "group1"]])
        aggregator.aggregate([data:[value1:1, group: "group1"]])
        aggregator.aggregate([data:[value1:2, group: "group1"]])
        aggregator.aggregate([data:[value1:2, group: "group2"]])
        aggregator.aggregate([data:[value1:3, group: "group3"]])

        AggregationResult result = aggregator.result()

        then:
        result.result == 6
    }
}
