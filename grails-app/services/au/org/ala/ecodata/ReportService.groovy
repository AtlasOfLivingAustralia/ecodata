package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.Aggregator
import au.org.ala.ecodata.reporting.AggregatorBuilder
import au.org.ala.ecodata.reporting.Score
import org.elasticsearch.search.SearchHit
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
                if (score.category) {
                    println score.category
                }
                toAggregate << [score:score]
            }
        }
        toAggregate << [score:new Score([outputName:'Revegetation Details', aggregationType:Score.AGGREGATION_TYPE.SUM, name:'totalNumberPlanted', label:'Number of plants planted', units:'kg'] ), groupBy:[groupTitle: 'Plants By Theme', entity:'activity', property:'mainTheme']]
        toAggregate << [score:new Score([outputName:'Weed Treatment Details', aggregationType:Score.AGGREGATION_TYPE.SUM, name:'areaTreatedHa', listName:'weedsTreated', label:'Area treated', units:'ha'] ), groupBy:[groupTitle: 'Area treated by species', entity:'output',  property:'targetSpecies.name']]
        toAggregate
    }

    def queryPaginated(params, Closure action) {
        def facets = (params.fq?.toList())?: []

        // Only dealing with approved activities.
        facets << PUBLISHED_ACTIVITIES_FILTER

        params.offset = 0
        params.max = 100

        def results = elasticSearchService.searchActivities(facets, params)

        def total = results.hits.totalHits
        def count = 0
        while (params.offset < total) {

            def hits = results.hits.hits
            for (SearchHit hit : hits) {
                action(hit)
            }
            params.offset += params.max
            results  = elasticSearchService.searchActivities(facets, params)
            println count
            count++
        }
    }

    def aggregate(params) {

        def toAggregate = buildReportSpec()

        List<Aggregator> aggregators = buildAggregators(toAggregate)
        def metadata = [activities: 0, distinctSites:new HashSet(), distinctProjects:new HashSet()]

        def aggregateActivity = { hit->
            def activity = hit.getSource()
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

        queryPaginated(params, aggregateActivity)

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
