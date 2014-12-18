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
     *     type : String <one of 'discrete', 'histogram', 'date', 'filter'>
     *     entity : String <one of 'activity', 'output', '*'>
     *     property : String <the property of the entity used to determine the group.  Unused if the entity is '*'>
     *     buckets: List<String> list of values defining the buckets for a histogram or date group.  Each bucket will be inclusive of
     *     the first value and exclusive of the next.
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
            case 'project':
            case 'site':
                property = groupingSpec.entity+'.'+property
                break
            case 'output':
                break // aggregation is done on outputs at the moment.
            case '*':
                return {""}  // No grouping required.
            default:
                throw new IllegalArgumentException("Invalid grouping Entity: "+groupingSpec.entity)
        }


        return buildGroupingStrategy(property, groupingSpec)
    }

    static Map cachedStrategies = [:]

    private String buildCacheKey(String nestedProperty, groupingSpec) {
        def key = groupingSpec.type + ':' + nestedProperty

        if (groupingSpec.filterBy) {
            key += ':'+groupingSpec.filterBy
        }
        key
    }

    def buildGroupingStrategy(String nestedProperty, groupingSpec) {

        def key = buildCacheKey(nestedProperty, groupingSpec)
            if (cachedStrategies.containsKey(key)) {
            return cachedStrategies[key]
        }

        ReportGroups.GroupingStrategy strategy


        switch (groupingSpec.type) {
            case 'histogram':
                strategy = new ReportGroups.HistogramGroup(nestedProperty, groupingSpec.buckets)
                break
            case 'date':
                strategy = new ReportGroups.DateGroup(nestedProperty, groupingSpec.buckets, groupingSpec.format)
                break
            case 'filter':
                strategy = new ReportGroups.FilteredGroup(nestedProperty, groupingSpec.filterBy)
                break
            case 'discrete':
            default:
                strategy = new ReportGroups.DiscreteGroup(nestedProperty)
                break

        }

        cachedStrategies.put(key, strategy)

        return strategy
    }
}
