package au.org.ala.ecodata.reporting

/**
 * An Aggregator is responsible for aggregating a list of scores.
 * It is capable of grouping the results by a single value.
 */
class Aggregator {

    static String DEFAULT_GROUP = ""

    Map<String, Aggregators.OutputAggregator> aggregatorsByGroup
    Score score
    Closure groupingFunction
    String title
    String outputListName

    public Aggregator(String title, groupingFunction, Score score, AggregatorBuilder builder) {

        this.groupingFunction = groupingFunction
        this.score = score
        this.title = title

        if (score.listName) {
            outputListName = score.listName
        }

        aggregatorsByGroup = [:].withDefault { key ->
            builder.createAggregator(score, key)
        }
    }

    def aggregate(activity) {

        activity.outputs?.each { output ->
            if (outputListName) {
                output.data[outputListName].each{
                    List<Aggregators.OutputAggregator> aggregators = aggregatorFor(activity, it)
                    aggregators.each {aggregator -> aggregator.aggregate(output)}
                }
            }
            else {
                List<Aggregators.OutputAggregator> aggregators = aggregatorFor(activity, output)
                aggregators.each {aggregator -> aggregator.aggregate(output)}
            }
        }

    }

    /**
     *  Returns the results of the aggregation.
     *  The results will be formatted like:
     *     {
     *        //TODO document me
     *     }
     */
    def results() {
        def results = aggregatorsByGroup.values().collect { it.result() }.findAll{ it.count > 0 }

        return [groupTitle:title, score:score, results:results]
    }

    /**
     * Classifies the supplied output according to the groupingFunction and returns the
     * Aggregator(s) that are aggregrating results for that group.
     * @param activity the activity to be aggregated - this is optionally used to perform the grouping.
     * @param output the output containing the scores to be aggregated. The output itself may also be used by the
     * grouping function.
     */
    List<Aggregators.OutputAggregator> aggregatorFor(activity, output) {

        output.activity = activity
        // TODO the grouping function should probably specify the default group.
        def group = groupingFunction(output)

        if (group instanceof List) {
            return group.collect { aggregatorsByGroup[group]}
        }

        if (!group) {
            group = DEFAULT_GROUP
        }
        return [aggregatorsByGroup[group]]
    }


}