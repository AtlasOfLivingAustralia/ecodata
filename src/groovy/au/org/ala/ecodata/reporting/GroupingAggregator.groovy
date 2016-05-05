package au.org.ala.ecodata.reporting


/**
 * Categorises an activity into a group based on a supplied grouping criteria then delegates to the appropriate
 * Aggregator.
 */
class GroupingAggregator extends BaseAggregator {

    Map<String, List> aggregatorsByGroup
    Map<String, Integer> countsByGroup
    int count

    AggregatorFactory factory = new AggregatorFactory()
    ReportGroups.GroupingStrategy groupingStrategy

    GroupingAggregationConfig config


    public GroupingAggregator(GroupingAggregationConfig config) {

        this.config = config
        this.groupingStrategy = factory.createGroupingStrategy(config.groups)

        aggregatorsByGroup = new LinkedHashMap().withDefault { key ->
            newAggregator()
        }
        countsByGroup = [:].withDefault { 0 }
    }

    private List<AggregatorIf> newAggregator() {
        List<Aggregator> aggregators = []

        config.childAggregations?.each { child ->
            aggregators << factory.createAggregator(child)
        }

        aggregators
    }

    PropertyAccessor getPropertyAccessor() {
        return groupingStrategy.propertyAccessor
    }

    void aggregateSingle(Map output) {

        def group = groupingStrategy.group(output)
        if (group == null) {
            return
        }

        if (group) {
            count++
        }
        incrementGroupCount(group)

        List<AggregatorIf> aggregators = aggregatorFor(group)
        aggregators.each {
            it.aggregate(output)
        }

    }

    GroupedAggregationResult result() {

        AggregationResult result = new GroupedAggregationResult(label:config.label, count:count)

        result.groups = countsByGroup.collect { Object group, Integer count ->
            new GroupedResult([group:group, count:count, results:aggregatorsByGroup[group].collect{it.result()}])
        }
        result
    }

    /**
     * Classifies the supplied output according to the groupingFunction and returns the
     * Aggregator(s) that are aggregating results for that group.
     * @param activity the activity to be aggregated - this is optionally used to perform the grouping.
     * @param output the output containing the scores to be aggregated. The output itself may also be used by the
     * grouping function.
     */
    List<AggregatorIf> aggregatorFor(group) {

        if (group == null) {
            return []
        }

        return aggregatorsByGroup[group]
    }

    List<AggregatorIf> aggregatorFor(Collection group) {
        if (group == null) {
            return []
        }
        return group.collect { aggregatorsByGroup[it] }.flatten()
    }

    void incrementGroupCount(Collection group) {
        group.each { entry ->
            countsByGroup[entry]++
        }
    }

    void incrementGroupCount(group) {
        countsByGroup[group]++
    }

}
