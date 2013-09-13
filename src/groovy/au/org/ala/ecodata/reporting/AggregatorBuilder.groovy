package au.org.ala.ecodata.reporting

/**
 * An AggregationBuilder can create instances of the Aggregator class based on the supplied information about
 * how the aggregation should be performed.
 */
class AggregatorBuilder {

    Map groupingSpec
    List<Score> scores

    public AggregatorBuilder groupBy(groupingSpec) {
        this.groupingSpec = groupingSpec
        this
    }


    public AggregatorBuilder scores(scores) {
        this.scores = scores
        this
    }


    public Aggregator build() {
        def groupingFunction = createGroupingFunction(groupingSpec)

        return new Aggregator(groupingSpec.title, groupingFunction, scores, this)
    }

    /**
     * Creates an appropriate OutputAggregator for the supplied score.
     * @param score the score to be aggregated.
     * @param group the value of the group that the Aggregator is aggregating.
     * @return a new instance of OutputAggregator
     */
    def createAggregator(score, group) {

        switch (score.aggregationType) {
            case Score.AGGREGATION_TYPE.SUM:
                return new Aggregators.SummingAggegrator([score:score, group:group])
                break;
            case Score.AGGREGATION_TYPE.COUNT:
                return new Aggregators.CountingAggregator([score:score, group:group])
                break;
            case Score.AGGREGATION_TYPE.AVERAGE:
                return new Aggregators.AverageAggregator([score:score, group:group])
                break;
            case Score.AGGREGATION_TYPE.HISTOGRAM:
                throw new RuntimeException("Not supported yet!")
                break;
        }


    }

    /**
     * Creates and returns a function capable of classifying an Output/Activity pair according to the
     * supplied grouping specification.
     * TODO we need to support sites & projects as grouping entities.  Nested properties are also not yet supported.
     * @param groupingSpec specifies the grouping criteria.  Should be of the format:
     * {
     *     entity : String <one of 'activity', 'output', '*'>
     *     property : String <the property of the entity used to determine the group.  Unused if the entity is '*'>
     * }
     * @return
     */
    Closure createGroupingFunction(groupingSpec) {

        final String property = groupingSpec.property
        switch (groupingSpec.entity) {
            case 'activity':
                return {activity, output -> activity[property]}
            case 'output':
                return {activity, output -> output[property]}
            case '*':
                return {activity, output -> ""}  // No grouping required.
            default:
                throw new IllegalArgumentException("Invalid grouping Entity: "+groupingSpec.entity)
        }

    }
}
