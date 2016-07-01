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

class GroupingConfig extends Aggregation {
    String type // DATE, DISCRETE, FILTER, HISTOGRAM
    String filterValue
    List<String> buckets
    String format
}

class Aggregation extends AggregationConfig {
    String property
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
