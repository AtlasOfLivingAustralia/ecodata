package au.org.ala.ecodata.reporting

import spock.lang.Specification

class ExpressionAggregatorSpec extends Specification {
    def "The expression aggregator can modify the results from child aggregations"() {

        Map config = [
                label: "score",
                expression: "value1+value2+value3['group1']+1",
                childAggregations: [
                        [
                                label:"value1",
                                "property": "data.value1",
                                "type": "SUM"
                        ],
                        [
                                label:"value2",
                                "property": "data.value2",
                                "keyProperty": "data.group",
                                "type": "DISTINCT_SUM"
                        ],
                        [
                                label:"value3",
                                groups:[
                                        property:"data.group",
                                        type:"discrete"
                                ],
                                childAggregations: [
                                        [
                                                label:"value3",
                                                property:"data.value3",
                                                type:"SUM"
                                        ]
                                ]
                        ]
                ]
        ]
        ExpressionAggregator aggregator = new AggregatorFactory().createAggregator(config)

        aggregator.aggregate([data:[value1:1, value2: 3, group:"group1", value3: 10]])
        AggregationResult result = aggregator.result()

        expect:
        result.result == 15
    }

    def "The expression aggregator can accept a default value for when the expression can't be evaluated"() {
        setup:
        Map config = [
                label: "score",
                expression: "value1/value2",
                "defaultValue":0,
                childAggregations: [
                        [
                             label:"value1",
                             property:"value1",
                             type:"SUM"
                     ],
                        [
                                label:"value2",
                                property:"value2",
                                type:"SUM"
                        ]
                ]
        ]

        ExpressionAggregator aggregator = new AggregatorFactory().createAggregator(config)

        when:
        aggregator.aggregate([data:[:]])
        AggregationResult result = aggregator.result()

        then:
        result.result == 0

        when:
        aggregator.aggregate([value1:0, value2:0]) // cause a divide by zero
        result = aggregator.result()

        then:
        result.result == 0

    }
}
