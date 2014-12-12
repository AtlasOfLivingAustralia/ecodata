package au.org.ala.ecodata.reporting

/**
 * Grouping strategies for the reporting subsystem.
 */
class ReportGroups {

    interface GroupingStrategy {
        def group(data)
    }

    static abstract class SinglePropertyGroupingStrategy implements GroupingStrategy {

        PropertyAccessor propertyAccessor

        public SinglePropertyGroupingStrategy(nestedProperty) {
            propertyAccessor = new PropertyAccessor(nestedProperty)
        }

    }

    static class DiscreteGroup extends SinglePropertyGroupingStrategy {

        public DiscreteGroup(nestedProperty) {
            super(nestedProperty)
        }

        def group(data) {
            return propertyAccessor.getPropertyValue(data)
        }
    }

    static class HistogramGroup extends SinglePropertyGroupingStrategy {

        def buckets

        /**
         * @param nestedProperty the name of the property to use to get the value that will be sorted into a bucket.
         * @param buckets a list of numbers - each number defines the start of the next bucket.  Each bucket starts
         * at the current number (inclusive) and ends at the next number (exclusive).
         */
        public HistogramGroup(nestedProperty, List buckets) {
            super(nestedProperty)
            this.buckets = buckets.collect{new BigDecimal(it)}
            Collections.sort(buckets)
        }

        def group(data) {
            def value = propertyAccessor.getPropertyAsNumeric(data)

            int result = Collections.binarySearch(buckets, value)

            return result >= 0 ? groupName(result) : groupName((-result)-2)
        }

        def groupName(index) {

            if (index == -1) {
                return "[-,${buckets[0]})"
            }
            else if (index >= buckets.size() - 1) {
                return "[${buckets[buckets.size()-1]},-)"
            }
            return "[${buckets[index]},${buckets[index+1]})"
        }
    }



}
