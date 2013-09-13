package au.org.ala.ecodata.reporting
/**
 * Convenience class to group together implementations of various types of aggregration functions (summing, counting etc)
 */
class Aggregators {

    public static abstract class OutputAggregator {

        String group
        Score score

        public abstract void aggregate(output);

        public abstract AggregrationResult result();

    }

    /**
     * Returns an average of the aggregated output scores
     */
    static class AverageAggregator extends OutputAggregator {

        int count
        double total

        public void aggregate(output) {

            if (output.scores[score.name]) {
                count++
                total += output.scores[score.name]
            }

        }

        public AggregrationResult result() {
            return new AggregrationResult([score:score, group:group, result:count > 0 ? total/count : 0])
        }
    }

    /**
     * Returns a the sum of the aggregated output scores
     */
    static class SummingAggegrator extends OutputAggregator {

        double total

        public void aggregate(output) {

            if (output.scores[score.name]) {
                total += output.scores[score.name]
            }

        }
        public AggregrationResult result() {
            return new AggregrationResult([score:score, group:group, result:total])
        }


    }

    /**
     * Returns the count of outputs containing the score. TODO counting entities may make more sense.
     */
    static class CountingAggregator extends OutputAggregator {

        int count = 0;
        public void aggregate(output) {
            if (output.scores.scoreName) {
                count++
            }
        }

        public AggregrationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new AggregrationResult([score:score, units:"", group:group, result:count])
        }
    }

    /**
     * Defines the format of the result of the aggregation
     */
    static class AggregrationResult {
        /** The score label */
        String score

        String outputName

        /** The units of the aggregation */
        String units
        /** The group that was aggregrated */
        String group

        /**
         * The result of the aggregation. Normally will be a numerical value (e.g. for a sum etc) or a List (for a collecting aggregator)
         */
        def result

        public void setScore(Score score) {
            this.score = score.label
            this.units = score.units
            this.outputName = score.outputName
        }
    }




}
