package au.org.ala.ecodata.reporting

/**
 * Combines the results of other aggregations into a single value.
 * Currently only supports summed values.
 */
class CompositeAggregator implements AggregatorIf {

    List<AggregatorIf> aggregators
    CompositeAggregationConfig config
    int count

    public CompositeAggregator(CompositeAggregationConfig config) {
        this.config = config
        aggregators = config.childAggregations.collect {
            new AggregatorFactory().createAggregator(it)
        }
    }

    public void aggregate(Map data) {
        count++
        aggregators.each {
            it.aggregate(data)
        }
    }

    public AggregationResult result() {

        AggregationResult result
        // If more than one child aggregation exists, we will sum the results of all child aggregations and return a single result. If only one child aggregation exists, we will return the result of that aggregation directly.
        if (aggregators.size() > 1) {
            Number sum = aggregators.sum { it.result().result }
            result = new SingleResult([label:config.label, count:count, result:sum])
        }
        else {
            result = aggregators[0].result()
            result.label = config.label
        }

        result
    }

}






