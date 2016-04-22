package au.org.ala.ecodata.reporting

/**
 * An Aggregator is responsible for aggregating a list of scores.
 * It is capable of grouping the results by a single value.
 */

class Aggregator {

    static String DEFAULT_GROUP = ""

    Map<String, Aggregators.OutputAggregator> aggregatorsByGroup

    List<Score> scores = []
    String title
    AggregatorFactory builder

    public Aggregator(String title, List<Score> scores, AggregatorFactory builder) {

        this.scores = scores
        this.title = title
        this.builder = builder

        aggregatorsByGroup = [:].withDefault { key ->
            // It's safe to use the first score as grouped scores must have the same label and aggregation type.
            builder.createAggregator(scores[0].label, scores[0].aggregationType.name(), key)
        }
    }

    def aggregate(output) {


        scores.each { score ->
            if (output.name == score.outputName) {
                if (score.listName) {
                    output.data[score.listName].each{
                        List<Aggregators.OutputAggregator> aggregators = aggregatorFor(score, it)
                        aggregators.each {aggregator -> aggregateOutput(score, aggregator, it)}
                    }
                }
                else {
                    List<Aggregators.OutputAggregator> aggregators = aggregatorFor(score, output)
                    aggregators.each {aggregator -> aggregateOutput(score, aggregator, output.data)}
                }
            }
        }


    }



    private void aggregateOutput(score, aggregator, output) {

        def val = getValue(output, score.name)
        if (val instanceof List) {
            val.each {aggregator.aggregateValue(it)}
        }
        else {
            aggregator.aggregateValue(val)
        }
    }

    def getValue(output, property) {
        return output[property]
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

        return [groupTitle:title, score:scores[0], results:results]
    }

    /**
     * Classifies the supplied output according to the groupingFunction and returns the
     * Aggregator(s) that are aggregating results for that group.
     * @param activity the activity to be aggregated - this is optionally used to perform the grouping.
     * @param output the output containing the scores to be aggregated. The output itself may also be used by the
     * grouping function.
     */
    List<Aggregators.OutputAggregator> aggregatorFor(score, output) {

        ReportGroups.GroupingStrategy groupingStrategy = builder.groupingStategyFor(score.defaultGrouping())
        def group = groupingStrategy.group(output)

        if (group == null) {
            return []
        }
        if (group instanceof List) { // values are lists when the data is collected via multi select
            return group.collect { aggregatorsByGroup[it] }
        }

        return [aggregatorsByGroup[group]]
    }
}