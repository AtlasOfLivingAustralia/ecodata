package au.org.ala.ecodata.reporting

/**
 * An AggregationBuilder can create instances of the Aggregator class based on the supplied information about
 * how the aggregation should be performed.
 */
class AggregatorFactory {

    public AggregatorIf createAggregator(AggregationConfig config) {
        if (config instanceof GroupingAggregationConfig) {
            new GroupingAggregator(config)
        }
        else if (config instanceof CompositeAggregationConfig) {

            new CompositeAggregator(config)
        }
        else if (config instanceof FilteredAggregationConfig) {
            new FilteredAggregator(config)
        }
        else {
            return createAggregator((Aggregation)config)
        }
    }

    /**
     * Creates an appropriate OutputAggregator for the supplied score.
     * @param score the score to be aggregated.
     * @param group the value of the group that the Aggregator is aggregating.
     * @return a new instance of OutputAggregator
     */
    AggregatorIf createAggregator(Aggregation config) {

       switch (config.type) {
            case Score.AGGREGATION_TYPE.SUM.name():
                return new Aggregators.SummingAggegrator(config.label, config.property)
                break;
            case Score.AGGREGATION_TYPE.COUNT.name():
                return new Aggregators.CountingAggregator(config.label, config.property)
                break;
            case Score.AGGREGATION_TYPE.AVERAGE.name():
                return new Aggregators.AverageAggregator(config.label, config.property)
                break;
            case Score.AGGREGATION_TYPE.HISTOGRAM.name():
                return new Aggregators.HistogramAggregator(config.label, config.property)
                break;
            case Score.AGGREGATION_TYPE.SET.name():
                return new Aggregators.SetAggregator(config.label, config.property)
                break;
            default:
                throw new IllegalAccessException('Invalid aggregation type: '+config.type)
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
    ReportGroups.GroupingStrategy createGroupingStrategy(GroupingConfig groupingSpec) {

        if (!groupingSpec) {
            return new ReportGroups.NotGrouped()
        }

        final String property = groupingSpec.property
        if (!property || property == '*') {
            return new ReportGroups.NotGrouped()
        }

        return buildGroupingStrategy(property, groupingSpec)
    }

    static Map cachedStrategies = [:]

    private String buildCacheKey(String nestedProperty, GroupingConfig groupingSpec) {
        def key = groupingSpec.type + ':' + nestedProperty

        if (groupingSpec.filterValue) {
            key += ':'+groupingSpec.filterValue
        }
        if (groupingSpec.buckets) {
            key += ':'+groupingSpec.buckets.join(',')
        }
        key
    }

    def buildGroupingStrategy(String nestedProperty, GroupingConfig groupingSpec) {

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
                strategy = new ReportGroups.FilteredGroup(nestedProperty, groupingSpec.filterValue)
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
