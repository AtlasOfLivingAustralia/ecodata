package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.Aggregator
import au.org.ala.ecodata.reporting.AggregatorBuilder
/**
 * The ReportService aggregates and returns output scores.
 */
class ReportService {

    def activityService, elasticSearchService, projectService, siteService

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
        List activities = activityService.findAllForProjectId(projectId, OutputService.SCORES)

        return aggregate(aggregationSpec, activities)
    }


    def aggregate(aggregationSpec, List<Activity> activities) {

        // If there are duplicate grouping functions it'll be better to run them once.
        def scoresByGroup = [:]
        aggregationSpec.each{
            def groupBy = it.groupBy ?: [entity:'*']
            if (groupBy) {

                if (!scoresByGroup[groupBy]) {
                    scoresByGroup[groupBy] = []
                }
                scoresByGroup[groupBy] << it.score
            }
        }

        // Determine if we need to group by site or project properties, if not we can avoid a lot of queries.
        boolean projectGrouping = scoresByGroup.keySet().find {it.entity == 'project'}
        boolean siteGrouping = scoresByGroup.keySet().find {it.entity == 'site'}

        List<Aggregator> aggregators = []

        scoresByGroup.each {
            aggregators << new AggregatorBuilder().scores(it.value).groupBy(it.key).build()
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
            aggregators.each { it.aggregate(activity) }
        }

        def allScores = []
        aggregators.each {

            def results = it.results()
            if (!results.groupName) {
                results.scores.each { score ->
                    allScores << [outputLabel: score.outputLabel, count:score.values[0].count, scoreLabel:score.scoreLabel, aggregatedResult:score.values[0].aggregatedResult]
                }
            }
            else {
                results.scores.each { score ->
                    score << [group:results.groupName]
                    allScores << score
                }
            }

        }
        allScores
    }



}
