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
                                "type": "SUM"
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
}
