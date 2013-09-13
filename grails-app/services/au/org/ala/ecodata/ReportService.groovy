package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.Aggregator
import au.org.ala.ecodata.reporting.AggregatorBuilder

/**
 * The ReportService aggegrates and returns output scores.
 */
class ReportService {

    def activityService

    /**
     * Returns aggregated scores for a specified project.
     * @param projectId the project of interest.
     * @param aggregationSpec defines the scores to be aggregated and if any grouping needs to occur.
     * Must be a map with key:<grouping specification>, value: List<Score> (the scores to be grouped according to the grouping specification)
     * where <grouping specification> must be of the format:
     * {
     *     entity : String <one of 'activity', 'output', '*'>
     *     property : String <the property of the entity used to determine the group.  Unused if the entity is '*'>
     * }
     *
     * @return the results of the aggregration.  The results will be an array of maps, the structure of each Map is
     * described in @see au.org.ala.ecodata.reporting.Aggregation.results()
     *
     */
    def projectSummary(String projectId, Map aggregationSpec) {

        // We definitely could be smarter about this query - only getting activities with outputs of particular
        // types or containing particular scores for example.
        List activities = activityService.findAllForProjectId(projectId, OutputService.SCORES)

        return aggregate(aggregationSpec, activities)
    }


    def aggregate(aggregationSpec, List<Activity> activities) {

        List<Aggregator> aggregators = []

        aggregationSpec.entrySet().each {
            aggregators << new AggregatorBuilder().groupBy(it.key).scores(it.value).build()
        }

        activities.each { activity ->
            aggregators.each { it.aggregate(activity) }
        }

        return aggregators.collect({it.results()})
    }
}
