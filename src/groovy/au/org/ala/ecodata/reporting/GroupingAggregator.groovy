package au.org.ala.ecodata.reporting

/**
 * Categorises an activity into a group based on a supplied grouping criteria then delegates to the appropriate
 * Aggregator.
 */
class GroupingAggregator implements AggregatorIf {

    Map<String, List> aggregatorsByGroup

    def metadata = [distinctActivities:new HashSet() , distinctSites:new HashSet(), distinctProjects:new HashSet(), activitiesByType:[:]]

    AggregatorFactory factory = new AggregatorFactory()
    ReportGroups.GroupingStrategy groupingStrategy

    AggregrationConfig config


    public GroupingAggregator(AggregrationConfig config) {

        this.config = config
        this.groupingStrategy = factory.createGroupingStategy(config.groups)

        aggregatorsByGroup = new LinkedHashMap().withDefault { key ->
            newAggregator(config)
        }
    }

    private List<AggregatorIf> newAggregator(AggregrationConfig config) {
        List<Aggregator> aggregators = []


        if (config.score) {
            aggregators << factory.createAggregator(config.label, config.score)
        }
        config.children?.each { child ->
            aggregators << factory.createAggregator(child)
        }

        aggregators
    }

    void aggregate(Map output) {

        updateMetadata(output.activity)

        List<AggregatorIf> aggregrators = aggregatorFor(output)

        aggregrators.each {
            it.aggregate(output)
        }

    }

    private def updateMetadata(Map activity) {

        metadata.distinctActivities << activity.activityId
        if (!metadata.activitiesByType[activity.type]) {
            metadata.activitiesByType[activity.type] = 0
        }
        metadata.activitiesByType[activity.type] = metadata.activitiesByType[activity.type] + 1

        metadata.distinctProjects << activity?.projectId
        if (activity?.sites) {
            metadata.distinctSites << activity.sites.siteId
        }
    }

    AggregrationResult result() {

        def results = aggregatorsByGroup.collect { String group, List<AggregatorIf> aggregators ->

            def result = [group:group]
            if (config.score) {
                AggregatorIf agg = aggregators.remove(0)
                result.result = agg.result()
            }
            result.results = aggregators.collect{it.result()}
            result
        }

        def result = new AggregrationResult([metadata:metadata, result:results])
        println result
        result
    }

    /**
     * Classifies the supplied output according to the groupingFunction and returns the
     * Aggregator(s) that are aggregating results for that group.
     * @param activity the activity to be aggregated - this is optionally used to perform the grouping.
     * @param output the output containing the scores to be aggregated. The output itself may also be used by the
     * grouping function.
     */
    List<AggregatorIf> aggregatorFor(output) {

        def group = groupingStrategy.group(output)

        if (group == null) {
            return []
        }

        if (group instanceof List) { // values are lists when the data is collected via multi select
            return group.collect { aggregatorsByGroup[it] }.flatten()
        }

        return aggregatorsByGroup[group]
    }

}
