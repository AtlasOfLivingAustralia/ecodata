package au.org.ala.ecodata.reporting

/**
 * An AggregationBuilder can create instances of the Aggregator class based on the supplied information about
 * how the aggregation should be performed.
 */
class AggregatorBuilder {

    Map groupingSpec
    Score score

    public AggregatorBuilder groupBy(groupingSpec) {
        this.groupingSpec = groupingSpec
        this
    }

    public AggregatorBuilder score(score) {
        this.score = score
        this
    }


    public Aggregator build() {
        def groupingFunction = createGroupingFunction(groupingSpec)
        return new Aggregator(groupingSpec.groupTitle, groupingFunction, score, this)
    }

    /**
     * Creates an appropriate OutputAggregator for the supplied score.
     * @param score the score to be aggregated.
     * @param group the value of the group that the Aggregator is aggregating.
     * @return a new instance of OutputAggregator
     */
    def createAggregator(score, group = '') {

        def params = [score:score, group:group]
        switch (score.aggregationType) {
            case Score.AGGREGATION_TYPE.SUM:
                return new Aggregators.SummingAggegrator(params)
                break;
            case Score.AGGREGATION_TYPE.COUNT:
                return new Aggregators.CountingAggregator(params)
                break;
            case Score.AGGREGATION_TYPE.AVERAGE:
                return new Aggregators.AverageAggregator(params)
                break;
            case Score.AGGREGATION_TYPE.HISTOGRAM:
                return new Aggregators.HistogramAggregator(params)
                break;
            case Score.AGGREGATION_TYPE.SET:
                return new Aggregators.SetAggregator(params)
                break;
            default:
                throw new IllegalAccessException('Invalid aggregation type: '+score.aggregationType)
        }


    }

    /**
     * Creates and returns a function capable of classifying an Output/Activity pair according to the
     * supplied grouping specification.
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
                return {activity, output -> Eval.x(activity, 'x?.'+property.replace('.', '?.'))}
            case 'output':
                return {activity, output -> Eval.x(output.data, 'x?.'+property.replace('.', '?.'))}
            case 'site':
                return {activity, output -> activity.site ? Eval.x(activity.site, 'x?.'+property.replace('.', '?.')) : null} // Use of Eval allows nested property access
            case 'project':
                return {activity, output -> activity.project ? Eval.x(activity.project, 'x?.'+property.replace('.', '?.')) : null} // Use of Eval allows nested property access
            case '*':
                return {activity, output -> ""}  // No grouping required.
            default:
                throw new IllegalArgumentException("Invalid grouping Entity: "+groupingSpec.entity)
        }

    }
}
