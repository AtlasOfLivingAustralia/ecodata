package au.org.ala.ecodata.reporting

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat


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

    static class FilteredGroup extends DiscreteGroup {
        def filterValue
        public FilteredGroup(nestedProperty, filterValue) {
            super(nestedProperty)
            this.filterValue = filterValue
        }

        def group(data) {
            def group = super.propertyAccessor.getPropertyValue(data)
            if (group instanceof List) { // values are lists when the data is collected via multi select
                return group.contains(filterValue) ? Aggregator.DEFAULT_GROUP : null
            }
            else {
                return group == filterValue ? Aggregator.DEFAULT_GROUP : null
            }
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
                return "< ${buckets[0]}"
            }
            else if (index >= buckets.size() - 1) {
                return "> ${buckets[buckets.size()-1]}"
            }
            return "${buckets[index]} - ${buckets[index+1]}"
        }
    }


    static class DateGroup extends SinglePropertyGroupingStrategy {

        static DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC()
        DateTimeFormatter dateFormatter
        def buckets
        def bucketStartLabels
        def bucketEndLabels

        /**
         * @param nestedProperty the name of the property to use to get the value that will be sorted into a bucket.
         * @param buckets a list of numbers - each number defines the start of the next bucket.  Each bucket starts
         * at the current number (inclusive) and ends at the next number (exclusive).
         * @param dateFormat the format to use when formatting group names.
         */
        public DateGroup(nestedProperty, buckets, dateFormat) {
            super(nestedProperty)

            this.buckets = new ArrayList(buckets)
            Collections.sort(this.buckets)

            this.dateFormatter = DateTimeFormat.forPattern(dateFormat).withZoneUTC()

            this.bucketStartLabels = buckets.collect{
                this.dateFormatter.print(parser.parseDateTime(it))
            }
            this.bucketEndLabels = buckets.collect {
                // Because the buckets are half open, we subtract one millisecond from the end dates so
                // that a range of 1 Jan - 1 Feb is printed as Jan (otherwise it would be Jan - Feb which is
                // incorrect)
                this.dateFormatter.print(parser.parseDateTime(it).minusMillis(1))
            }

        }

        def group(data) {
            def value = propertyAccessor.getPropertyValue(data)

            int result = Collections.binarySearch(buckets, value)

            return result >= 0 ? groupName(result) : groupName((-result)-2)
        }

        def groupName(index) {

            if (index == -1) {
                return "Before ${bucketStartLabels[0]}"
            }
            else if (index >= buckets.size() - 1) {
                return "After ${bucketEndLabels[buckets.size()-1]}"
            }
            def start = bucketStartLabels[index]
            def end = bucketEndLabels[index+1]
            if (start == end) {
                return start
            }
            return "${start} - ${end}"
        }
    }


    static class NotGrouped implements GroupingStrategy {
        public group(data) {
            return Aggregator.DEFAULT_GROUP
        }
    }



}
