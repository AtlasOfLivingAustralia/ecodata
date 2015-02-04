package au.org.ala.ecodata.reporting

/**
 * Categorises an activity into a group based on a supplied grouping criteria then delegates to the appropriate
 * Aggregator.
 */
class GroupingAggregator {

    Map<String, List> aggregatorsByGroup
    def metadata = [distinctActivities:new HashSet() , distinctSites:new HashSet(), distinctProjects:new HashSet()]


    ReportGroups.GroupingStrategy groupingStrategy


    public GroupingAggregator(groupingSpec, List scores) {

        this.groupingStrategy = new AggregatorBuilder().groupingStategyFor(groupingSpec)

        aggregatorsByGroup = new LinkedHashMap().withDefault { key ->

            newAggregator(scores)
        }

        if (groupingSpec?.buckets) {
            groupingStrategy.groups().each { aggregatorsByGroup.get(it) }  // Prepop the groups in the correct order.
        }
    }

    private List newAggregator(scores) {
        List<Aggregator> aggregators = []

        def groupedScores = scores.groupBy { it.score.label }

        groupedScores.each { k, v ->
            aggregators << new AggregatorBuilder().scores(v.collect { it.score }).build()
        }

        aggregators
    }

    def aggregate(output) {

        updateMetadata(output)

        List aggregrators = aggregatorFor(output)

        aggregrators.each {
            it.aggregate(output)
        }
    }

    private def updateMetadata(output) {

       metadata.distinctActivities << output.activity.activityId

        metadata.distinctProjects << output.activity?.projectId
        if (output.activity?.sites) {
            metadata.distinctSites << output.activity.sites.siteId
        }
    }

    def results() {

        def results = aggregatorsByGroup.collect {k, v ->  [group:k, results: v.collect{it.results()}]}


        [metadata:metadata, results:results]
    }

    /**
     * Classifies the supplied output according to the groupingFunction and returns the
     * Aggregator(s) that are aggregating results for that group.
     * @param activity the activity to be aggregated - this is optionally used to perform the grouping.
     * @param output the output containing the scores to be aggregated. The output itself may also be used by the
     * grouping function.
     */
    List aggregatorFor(output) {

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
