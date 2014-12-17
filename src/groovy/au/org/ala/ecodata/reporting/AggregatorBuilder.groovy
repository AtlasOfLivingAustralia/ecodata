package au.org.ala.ecodata.reporting

/**
 * An AggregationBuilder can create instances of the Aggregator class based on the supplied information about
 * how the aggregation should be performed.
 */
class AggregatorBuilder {

    Map groupingSpec
    List<Score> scores = []

    public AggregatorBuilder groupBy(groupingSpec) {
        this.groupingSpec = groupingSpec
        this
    }

    public AggregatorBuilder scores(List<Score> scores) {
        this.scores = scores
        this
    }

    public AggregatorBuilder accumulate(Score score) {
        this.scores << score
    }


    public Aggregator build() {
        if (!scores) {
            throw new IllegalArgumentException("At least one score must be supplied.")
        }
        return new Aggregator(scores[0].label, scores, this)
    }

    /**
     * Creates an appropriate OutputAggregator for the supplied score.
     * @param score the score to be aggregated.
     * @param group the value of the group that the Aggregator is aggregating.
     * @return a new instance of OutputAggregator
     */
    def createAggregator(label, aggregationType, group = '') {

        def params = [label:label, group:group]
        switch (aggregationType) {
            case Score.AGGREGATION_TYPE.SUM.name():
                return new Aggregators.SummingAggegrator(params)
                break;
            case Score.AGGREGATION_TYPE.COUNT.name():
                return new Aggregators.CountingAggregator(params)
                break;
            case Score.AGGREGATION_TYPE.AVERAGE.name():
                return new Aggregators.AverageAggregator(params)
                break;
            case Score.AGGREGATION_TYPE.HISTOGRAM.name():
                return new Aggregators.HistogramAggregator(params)
                break;
            case Score.AGGREGATION_TYPE.SET.name():
                return new Aggregators.SetAggregator(params)
                break;
            default:
                throw new IllegalAccessException('Invalid aggregation type: '+aggregationType)
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
    ReportGroups.GroupingStrategy groupingStategyFor(groupingSpec) {

        if (!groupingSpec) {
            return new ReportGroups.NotGrouped()
        }

        final String property = groupingSpec.property

        switch (groupingSpec.entity) {

            case 'activity':
                return buildGroupingStrategy(property, 'activity', groupingSpec.filterBy)
            case 'output':

                return buildGroupingStrategy(property, '', groupingSpec.filterBy)
            case 'site':
                return buildGroupingStrategy(property, 'site', groupingSpec.filterBy)
            case 'project':
                return buildGroupingStrategy(property, 'project', groupingSpec.filterBy)
            case '*':
                return {""}  // No grouping required.
            default:
                throw new IllegalArgumentException("Invalid grouping Entity: "+groupingSpec.entity)
        }


    }


    static Map cachedStrategies = [:]
    def buildGroupingStrategy(String property, String propertyPrefix, String filterValue) {

        def nestedProperty = propertyPrefix ? propertyPrefix+'.'+property : property

        def key = filterValue ? nestedProperty + ':' + filterValue : nestedProperty
        if (cachedStrategies.containsKey(key)) {
            return cachedStrategies[key]
        }

        ReportGroups.GroupingStrategy strategy

        if (!filterValue) {
            strategy = new ReportGroups.DiscreteGroup(nestedProperty)
        }
        else {
            strategy = new ReportGroups.FilteredGroup(nestedProperty, filterValue)
        }
        cachedStrategies.put(key, strategy)

        return strategy
    }
}
