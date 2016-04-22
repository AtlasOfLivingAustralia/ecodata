package au.org.ala.ecodata.reporting

import org.apache.log4j.Logger

/**
 * Convenience class to group together implementations of various types of aggregration functions (summing, counting etc)
 */
class Aggregators {

    def log = Logger.getLogger(getClass())

    public static abstract class OutputAggregator implements AggregatorIf {

        String group
        int count = 0
        String label

        PropertyAccessor propertyAccessor

        public OutputAggregator(String label, String property) {
            this.label = label
            propertyAccessor = new PropertyAccessor('data.'+property)
        }

        public void aggregate(Map values) {
            Object value = propertyAccessor.getPropertyValue(values)
            if (value != null) {
                count++;
                doAggregation(value);
            }
        }

        public abstract void doAggregation(output);

        public abstract AggregrationResult result();

    }

    /**
     * Returns an average of the aggregated output scores
     */
    static class AverageAggregator extends OutputAggregator {

        public AverageAggregator(String label, String property) {
            super(label, property)
        }
        double total = 0

        public void doAggregation(value) {
            def numericValue = propertyAccessor.getValueAsNumeric(value)
            if (numericValue) {
                total += numericValue
            }
        }

        public AggregrationResult result() {
            return new AggregrationResult([label:label, group:group, count: count, result:count > 0 ? total/count : 0])
        }
    }

    /**
     * Returns a the sum of the aggregated output scores
     */
    static class SummingAggegrator extends OutputAggregator {

        double total = 0
        public SummingAggegrator(String label, String property) {
            super(label, property)
        }

        public void doAggregation(value) {

            def numericValue = propertyAccessor.getValueAsNumeric(value)
            println numericValue
            if (numericValue) {
                total += numericValue
            }

        }
        public AggregrationResult result() {
            return new AggregrationResult([label:label, group:group, count:count, result:total])
        }

    }

    /**
     * Returns the count of outputs containing the score. TODO counting entities may make more sense.
     */
    static class CountingAggregator extends OutputAggregator {

        public CountingAggregator(String label, String property) {
            super(label, property)
        }
        public void doAggregation(output) {}

        public AggregrationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new AggregrationResult([label:label, units:"", group:group, count:count, result:count])
        }
    }

    /**
     * Returns a Map with keys being distinct values of the score and values being the number of times
     * that score value occurred.
     */
    static class HistogramAggregator extends OutputAggregator {

        Map histogram = [:].withDefault { 0 };
        public HistogramAggregator(String label, String property) {
            super(label, property)
        }

        public void doAggregation(value) {

            if (value =~ /name:/) {
                // extract sci name from complex key. e.g.
                // [guid:urn:lsid:biodiversity.org.au:apni.taxon:56760, listId:Atlas of Living Australia, name:Paspalum punctatum, list:]
                // TODO move this code to somewhere else where the string has not already been encoded
                //log.debug "value = '${value}'"
                def m = value =~ /(?:name:)(.*?),/
                //log.debug "m = ${m[0][1]?:'[unknown]'} "
                value = m[0][1]?:'[unknown]'
            }
            histogram[value]++
        }

        public AggregrationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new AggregrationResult([label:label, units:"", group:group, count:count, result:histogram])
        }
    }

    /**
     * Returns Set containing distinct values of the output score
     */
    static class SetAggregator extends OutputAggregator {

        List values = []
        public SetAggregator(String label, String property) {
            super(label, property)
        }

        public void doAggregation(value) {
            values << value
        }

        public AggregrationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new AggregrationResult([label:label, units:"", group:group, count:count, result:values])
        }
    }

}
