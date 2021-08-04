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

    public SingleResult result() {

        // We could do a sum
        Number sum = aggregators.sum { it.result().result }

//        // We could also do averages...
//        List<SingleResult> results = aggregators.collect { it.result() }
//        def total = results.sum { it.result * it.count }
//        def count = results.sum { it.count }
//        def avg = total/count

        SingleResult result = new SingleResult([label:config.label, count:count, result:sum])

        result
    }

}






