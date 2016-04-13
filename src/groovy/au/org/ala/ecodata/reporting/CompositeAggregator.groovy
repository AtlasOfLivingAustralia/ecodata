package au.org.ala.ecodata.reporting

import org.springframework.data.mongodb.core.aggregation.AggregationResults


interface AggregatorIf {
    void aggregate(Map data)
    AggregrationResult result()
}

/**
 * Created by god08d on 5/04/2016.
 */
class CompositeAggregator implements AggregatorIf {

    List<AggregatorIf> aggregators

    public CompositeAggregator(List<AggregrationConfig> config) {
        aggregators = config.children.collect{
            new AggregatorFactory().createAggregator(it)
        }
    }

    public void aggregate(Map data) {
        aggregators.each {
            it.aggregate(data)
        }
    }

    public AggregrationResult result() {

        // We could do a sum
        Number sum = aggregators.collect { it.result() }.sum{ it.result }

        // We could also do averages...
        List<AggregationResults> results = aggregators.collect { it.result() }
        def total = results.sum { it.result * it.count }
        def count = results.sum { it.count }
        def avg = total/count

        AggregationResults result = new AggregrationResult([label:"", count:count, result:sum])

        result
    }

}






