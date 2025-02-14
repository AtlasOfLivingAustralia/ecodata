package au.org.ala.ecodata.reporting



class AggregationConfig {
    String label
    String type
}

class GroupingAggregationConfig extends AggregationConfig {
    List<AggregationConfig> childAggregations
    GroupingConfig groups
}

class CompositeAggregationConfig extends AggregationConfig {
    List<AggregationConfig> childAggregations
}

class FilteredAggregationConfig extends AggregationConfig {
    List<AggregationConfig> childAggregations
    GroupingConfig filter
    public void setFilter(GroupingConfig filter) {
        this.filter = filter
        this.filter.type = 'filter'
    }
}

class ExpressionAggregationConfig extends CompositeAggregationConfig {
    /** The expression to evaluate */
    String expression

    /** The value to return if the expression evaluation fails (e.g. missing variables due to no data, divide by 0) */
    def defaultValue
}

class GroupingConfig extends Aggregation {
    String type // DATE, DISCRETE, FILTER, HISTOGRAM
    Object filterValue
    List<String> buckets
    String format
}

class Aggregation extends AggregationConfig {
    String property
}

class DistinctAggregationConfig extends Aggregation {
    String keyProperty
}

class AggregationResult {
    String label
    int count
}

class SingleResult extends AggregationResult {
    def result
}

class GroupedResult {
    String group
    int count
    List<AggregationResult> results
}

class GroupedAggregationResult extends AggregationResult {
    List<GroupedResult> groups
}
