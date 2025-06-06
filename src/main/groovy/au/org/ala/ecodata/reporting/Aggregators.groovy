package au.org.ala.ecodata.reporting

import groovy.util.logging.Slf4j


interface AggregatorIf {
    void aggregate(Map data)
    AggregationResult result()
}

public abstract class BaseAggregator implements AggregatorIf {

    void aggregate(Map output) {
        PropertyAccessor propertyAccessor = getPropertyAccessor()
        if (propertyAccessor?.isNested(output)) {
            propertyAccessor.unroll(output).each {
                aggregateSingle(it)
            }
        }
        else {
            aggregateSingle(output)
        }

    }

    abstract PropertyAccessor getPropertyAccessor()

    abstract void aggregateSingle(Map output)

}

/**
 * Convenience class to group together implementations of various types of aggregration functions (summing, counting etc)
 */
@Slf4j
class Aggregators {

    public static abstract class OutputAggregator extends BaseAggregator {

        int count = 0
        String label

        PropertyAccessor propertyAccessor

        public OutputAggregator(String label, String property) {
            this.label = label
            propertyAccessor = new PropertyAccessor(property)
        }

        public void aggregateSingle(Map values) {
            Object value = propertyAccessor.getPropertyValue(values)
            if (value != null) {
                count++;
                doAggregation(value);
            }
        }

        public abstract void doAggregation(output);

        public abstract AggregationResult result();

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

        public AggregationResult result() {
            return new SingleResult([label:label, count: count, result:count > 0 ? total/count : 0])
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
            if (numericValue) {
                total += numericValue
            }

        }
        public AggregationResult result() {
            return new SingleResult([label:label, count:count, result:total])
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

        public AggregationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new SingleResult([label:label, count:count, result:count])
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

            if (value instanceof Collection) {
                value.each { aggregateSingleValue(it) }
            }
            else {
                aggregateSingleValue(value)
            }

        }

        private void aggregateSingleValue(value) {
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

        public AggregationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new SingleResult([label:label, count:count, result:histogram])
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

        public AggregationResult result() {
            // Units don't make sense for a count, regardless of the units of the score.
            return new SingleResult([label:label, count:count, result:values])
        }
    }

    /**
     * The DistinctSumAggregator will sum the values of a property where a second property is distinct.
     * The use case for this is to sum data in values that appear more than once during aggregation.
     * For example, most aggregation is done with Output data, and each one contains the Activity and owner (e.g. Project)
     * data as well.
     * The specific use case for this is to sum the data from Organisation reports and divide by the total
     * funding, which is a property on the Organisation object.
     * The keyProperty is the property that is used to determine if the value is distinct.  The case of the
     * organisation funding amount, the organisationId would be used as the key property.
     */
    static class DistinctSumAggregator extends SummingAggegrator {
        protected PropertyAccessor keyPropertyAccessor
        protected Set seenKeys
        DistinctSumAggregator(String label, String property, String keyProperty) {
            super(label, property)
            keyPropertyAccessor = new PropertyAccessor(keyProperty)
            seenKeys = new HashSet()
        }

        void aggregateSingle(Map data) {
            def key = keyPropertyAccessor.getPropertyValue(data)
            if (!seenKeys.contains(key)) {
                seenKeys.add(key)
                super.aggregateSingle(data)
            }
        }

    }

}
