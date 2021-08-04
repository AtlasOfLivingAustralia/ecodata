package au.org.ala.ecodata.reporting

import spock.lang.Specification

/**
 * Specification for the CompositeAggregator
 */
class CompositeAggregatorSpec extends Specification {

    def "two aggregations can be combined into a single result"() {

        given:
        Aggregation score = new Aggregation(property:"property1", type:"SUM")
        Aggregation score2 = new Aggregation(property:"property2", type:"SUM")
        CompositeAggregationConfig aggregationConfig = new CompositeAggregationConfig(label:"test", childAggregations: [score, score2])

        CompositeAggregator compositeAggregator = new CompositeAggregator(aggregationConfig)


        List<Map> data = [
                [property1:1, property2:3], [property1:2, property2:5], [property1:3, property2:4], [property1:4, property2:5]
        ]

        when:
        data.each {
            compositeAggregator.aggregate(it)
        }
        AggregationResult result = compositeAggregator.result()

        then:
        result.label == aggregationConfig.label
        result.count == data.size()

        result.result == data.sum{it.property1 + it.property2}
    }
}
