package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.Aggregator
import au.org.ala.ecodata.reporting.AggregatorBuilder
/**
 * The ReportService aggregates and returns output scores.
 */
class ReportService {

    def activityService, elasticSearchService, projectService, siteService, outputService

    /**
     * Returns aggregated scores for a specified project.
     * @param projectId the project of interest.
     * @param aggregationSpec defines the scores to be aggregated and if any grouping needs to occur.
     * [{score:{name: , units:, aggregationType}, groupBy: {entity: <one of 'activity', 'output', 'project', 'site>, property: String <the entity property to group by>}, ...]
     *
     * @return the results of the aggregration.  The results will be an array of maps, the structure of each Map is
     * described in @see au.org.ala.ecodata.reporting.Aggregation.results()
     *
     */
    def projectSummary(String projectId, List aggregationSpec) {


       // We definitely could be smarter about this query - only getting activities with outputs of particular
        // types or containing particular scores for example.
        List activities = activityService.findAllForProjectId(projectId, 'FLAT')
        List outputs = Output.findAllByActivityIdInListAndStatus(activities.collect{it.activityId}, OutputService.ACTIVE).collect {outputService.toMap(it)}
        Map outputsByActivityId = outputs.groupBy{it.activityId}

        return aggregate(aggregationSpec, activities, outputsByActivityId)
    }


    def aggregate(aggregationSpec, List<Activity> activities, Map outputsByActivityId) {

        // Determine if we need to group by site or project properties, if not we can avoid a lot of queries.
        boolean projectGrouping = aggregationSpec.find {it.groupBy?.entity == 'project'}
        boolean siteGrouping = aggregationSpec.find {it.groupBy?.entity == 'site'}


        List<Aggregator> aggregators = []

        aggregationSpec.each {
            aggregators << new AggregatorBuilder().score(it.score).groupBy(it.groupBy?:it.groupBy ?: [entity:'*']).build()
        }

        activities.each { activity ->
            // This is really a bad way to do this as we are going to be running a lot of queries do do the aggregation.
            // I think the best way is going to be to index Activities with project and site data and do the
            // query via the search index.
            if (projectGrouping && activity.projectId) {
                activity['project'] = projectService.toMap(Project.findByProjectId(activity.projectId), ProjectService.BRIEF)
            }
            if (siteGrouping && activity.siteId) {
                activity['site'] = siteService.toMap(Site.findBySiteId(activity.siteId), SiteService.BRIEF)
            }
            activity['outputs'] = outputsByActivityId[activity.activityId]
            aggregators.each { it.aggregate(activity) }
        }

        aggregators.collect {it.results()}

    }



}
