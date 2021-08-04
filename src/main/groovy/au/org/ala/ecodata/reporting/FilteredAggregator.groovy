package au.org.ala.ecodata.reporting
/**
 * Categorises an activity into a group based on a supplied grouping criteria then delegates to the appropriate
 * Aggregator.
 */
class FilteredAggregator extends BaseAggregator {

    List<AggregatorIf> aggregators
    int count

    AggregatorFactory factory = new AggregatorFactory()
    ReportGroups.GroupingStrategy filteringStrategy

    FilteredAggregationConfig config


    public FilteredAggregator(FilteredAggregationConfig config) {

        this.config = config
        this.filteringStrategy = factory.createGroupingStrategy(config.filter)

        aggregators = config.childAggregations.collect {
            factory.createAggregator(it)
        }
    }

    PropertyAccessor getPropertyAccessor() {
        return filteringStrategy.propertyAccessor
    }

    void aggregateSingle(Map output) {

        def group = filteringStrategy.group(output)
        if (group == null) {
            return
        }

        count++
        aggregators.each {
            it.aggregate(output)
        }

    }

    /**
     * If we have a single childAggregation, return a SingleResult, otherwise a
     * GroupedAggregrationResult.
     */
    AggregationResult result() {

        AggregationResult result
        if (config.childAggregations.size() > 1) {
            result = new GroupedAggregationResult(label:config.label, count:count)

            result.groups = [new GroupedResult(group:config.filter.filterValue, count:count)]
            result.groups[0].results = aggregators.collect { it.result() }

        }
        else {
            result = aggregators[0].result()
            result.label = config.label
            result.count = aggregators[0].count
        }

        result
    }
}
