package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.Aggregator
import au.org.ala.ecodata.reporting.AggregatorBuilder
import au.org.ala.ecodata.reporting.Score
/**
 * The ReportService aggregates and returns output scores.
 */
class ReportService {

    def activityService, elasticSearchService, projectService, siteService, outputService, metadataService

    static final String PUBLISHED_ACTIVITIES_FILTER = 'publicationStatus:published'

    /**
     * Creates an aggregation specification from the Scores defined in the activities model.
     */
    def buildReportSpec() {
        def toAggregate = []

        metadataService.activitiesModel().outputs?.each{
            Score.outputScores(it).each { score ->
                def scoreDetails = [score:score]
                if (score.groupBy) {
                    def bits = score.groupBy.split(':')
                    if (bits.length != 2) {
                        log.error("Misconfigured score grouping: "+score.groupBy)
                    }
                    else {
                        scoreDetails << [groupBy: [entity: bits[0], property: bits[1], groupTitle: score.label]]
                    }
                }

                toAggregate << scoreDetails
            }
        }
        toAggregate
    }

    def queryPaginated(List filters, Closure action) {

        // Only dealing with approved activities.
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER]
        additionalFilters.addAll(filters)

        Map params = [offset:0, max:100]

        def results = elasticSearchService.searchActivities(additionalFilters, params)

        def total = results.hits.totalHits
        while (params.offset < total) {

            def hits = results.hits.hits
            for (def hit : hits) {
                action(hit.source)
            }
            params.offset += params.max

            results  = elasticSearchService.searchActivities(additionalFilters, params)
        }
    }

    def aggregate(List filters) {

        def toAggregate = buildReportSpec()

        List<Aggregator> aggregators = buildAggregators(toAggregate)
        def metadata = [activities: 0, distinctSites:new HashSet(), distinctProjects:new HashSet()]

        def aggregateActivity = { activity ->
            metadata.activities++
            metadata.distinctProjects << activity.projectId
            if (activity.sites) {
                metadata.distinctSites << activity.sites.siteId
            }

            Output.withNewSession {
                def outputs = outputService.findAllForActivityId(activity.activityId, ActivityService.FLAT)
                activity.outputs = outputs
                aggregators.each { it.aggregate(activity) }
            }
        }

        queryPaginated(filters, aggregateActivity)

        def allResults = aggregators.collect {it.results()}
        def outputData = allResults.findAll{it.results}
        [outputData:outputData, metadata:[activities: metadata.activities, sites:metadata.distinctSites.size(), projects:metadata.distinctProjects.size()]]
    }

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

        List<Aggregator> aggregators = buildAggregators(aggregationSpec)

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

    def buildAggregators(aggregationSpec) {
        List<Aggregator> aggregators = []

        aggregationSpec.each {
            aggregators << new AggregatorBuilder().score(it.score).groupBy(it.groupBy?:it.groupBy ?: [entity:'*']).build()
        }

        aggregators
    }



}
