package au.org.ala.ecodata.reporting



class AggregrationConfig {

    String label

    Aggregration score
    List<AggregrationConfig> children
    GroupingConfig groups
    GroupingConfig filter

}


class GroupingConfig extends Aggregration {
    String type // DATE, DISCRETE, FILTER, HISTOGRAM
    String filterValue
    List<String> buckets
}

class Aggregration extends AggregrationConfig {
    String type // SUM, COUNT, AVERAGE, SET, HISTOGRAM

    String entity
    String property
}

/**
 * Defines the format of the result of the aggregation
 */
class AggregrationResult {
    /** The score label */
    String label

    /** The units of the aggregation */
    String units
    /** The group that was aggregrated */
    String group

    int count;

    /**
     * The result of the aggregation. Normally will be a numerical value (e.g. for a sum etc) or a List (for a collecting aggregator)
     */
    def result

    Map metadata;

    public String toString() {
        return "$label:,count=$count,result=$result"
    }
}

