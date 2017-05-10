package au.org.ala.ecodata

import au.org.ala.ecodata.Score
import au.org.ala.ecodata.reporting.*
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.grails.plugins.csv.CSVReaderUtils

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX

/**
 * The ReportService aggregates and returns output scores.
 * It is also responsible for managing Reports submitted by users.
 */
class ReportService {

    def grailsApplication, activityService, elasticSearchService, projectService, siteService, outputService, metadataService, userService, settingService, webService


    def findScoresByLabel(List labels) {
        Score.findAllByLabelInList(labels)
    }

    def findScoresByCategory(String category) {
        Score.findAllByCategory(category)
    }

    def runActivityReport(String searchTerm, List filters, Map reportConfig, boolean approvedActivitiesOnly) {
        AggregatorIf aggregator = new AggregatorFactory().createAggregator(reportConfig)

        Map metadata = [distinctActivities:new HashSet() , distinctSites:new HashSet(), distinctProjects:new HashSet(), activitiesByType:[:]]

        Closure aggregateActivityWithMetadata =  { AggregatorIf aggregatorIf, Map activity ->
            aggregateActivity(aggregatorIf, activity)
            updateMetadata(activity, metadata)
        }
        queryPaginated(filters, searchTerm, approvedActivitiesOnly, aggregator, aggregateActivityWithMetadata)

        GroupedAggregationResult allResults = aggregator.result()

        [results:allResults, metadata:[activities: metadata.distinctActivities.size(), sites:metadata.distinctSites.size(), projects:metadata.distinctProjects, activitiesByType:metadata.activitiesByType]]
    }

    def queryPaginated(List filters, String searchTerm, boolean approvedActivitiesOnly, AggregatorIf aggregator, Closure action) {

        Map params = [offset:0, max:20, fq:filters]

        def results = elasticSearchService.search(searchTerm, params, HOMEPAGE_INDEX)
        def total = results.hits.totalHits
        while (params.offset < total) {

            results.hits.hits.each { hit ->
                Map project = hit.source

                List activities = project.activities
                if (approvedActivitiesOnly) {
                    activities = activities?.findAll{it.publicationStatus == Report.REPORT_APPROVED}
                }
                if (activities) {
                    List activityIds = activities?.collect{it.activityId}
                    Output.withNewSession {
                        List outputs = Output.findAllByActivityIdInListAndStatusNotEqual(activityIds, Status.DELETED)
                        Map<String, List> outputsByActivityId = outputs.groupBy { it.activityId }
                        activities?.each { activity ->
                            activity.outputs = outputsByActivityId[activity.activityId] ?: []
                            action(aggregator, activity)
                        }
                    }
                }
            }
            params.offset += params.max

            results  = elasticSearchService.search(searchTerm, params, HOMEPAGE_INDEX)
        }
    }

    def aggregate(List filters, String searchTerm) {
        List<Score> scores = Score.findAll()
        aggregate(filters, searchTerm, scores)
    }

    def aggregate(List filters) {
        aggregate(filters, null)
    }

    def aggregate(List filters, String searchTerm, List<Score> toAggregate, topLevelGrouping = null, boolean approvedActivitiesOnly = true) {

        GroupingAggregationConfig topLevelConfig = aggregationConfigFromScores(toAggregate, topLevelGrouping)

        AggregatorIf aggregator = new AggregatorFactory().createAggregator(topLevelConfig)

        Map metadata = [distinctActivities:new HashSet() , distinctSites:new HashSet(), distinctProjects:new HashSet(), activitiesByType:[:]]

        Closure aggregateActivityWithMetadata =  { AggregatorIf aggregatorIf, Map activity ->
            aggregateActivity(aggregatorIf, activity)
            updateMetadata(activity, metadata)
        }
        queryPaginated(filters, searchTerm, approvedActivitiesOnly, aggregator, aggregateActivityWithMetadata)

        GroupedAggregationResult allResults = aggregator.result()
        def outputData = allResults
        if (topLevelGrouping == null ) {
            outputData = postProcessOutputData(allResults.groups? allResults.groups[0].results : [], toAggregate)
        }

        [outputData:outputData, metadata:[activities: metadata.distinctActivities.size(), sites:metadata.distinctSites.size(), projects:metadata.distinctProjects, activitiesByType:metadata.activitiesByType]]
    }

    private List postProcessOutputData(List outputData, List scores) {
        List processedOutputData = []
        outputData.each { result ->
            Score score = scores.find{it.label == result.label}

            if (!score) {
                println "No score for ${result.label}"
                return
            }

            Map resultMap = [scoreId:score.scoreId, label:score.label, description:score.description, displayType:score.displayType, outputType: score.outputType, isOutputTarget: score.isOutputTarget, category: score.category, result: result.properties]
            processedOutputData << resultMap
        }
        processedOutputData
    }

    private GroupingAggregationConfig aggregationConfigFromScores(List<Score> scores = null, Map topLevelGrouping = null) {
        if (!scores) {
            scores = Score.findAll()
        }

        List config = scores.collect{
            it.configuration.label = it.label
            it.configuration
        }
        GroupingConfig topLevelGroupingConfig = new GroupingConfig(topLevelGrouping?:[:])
        new GroupingAggregationConfig(childAggregations: config, groups:topLevelGroupingConfig)
    }

    private def outputType(List scores) {
        def result = scores.find{it.score.outputName != 'Output Details'}?.score?.outputName
        if (!result) {
            result = scores[0].score.outputName
        }

        result
    }


    private def generateScores(List<Map<String, Score>> toAggregate, Map topLevelGrouping = null) {

        Map<String, List> groupedScores = toAggregate.groupBy { it.score.label }

        AggregationConfig aggregationConfig
        groupedScores.collect { label, scores ->

            List<AggregationConfig> config = scores.collect {configFor(it.score)}
            if (config.size() > 1) {
                aggregationConfig = new CompositeAggregationConfig(childAggregations: config, label:label)
            }
            else {
                aggregationConfig = config[0]
            }

            [
                    label:label,
                    description:scores[0].score.description,
                    config:aggregationConfig,
                    isOutputTarget:scores[0].score.isOutputTarget,
                    category:scores[0].score.category,
                    outputType:outputType(scores),
                    displayType:scores[0].score.displayType,
                    entity:Activity.class.name,
                    entityTypes:scores.collect{it.activities}.flatten()

            ]
        }

    }

    private AggregationConfig configFor(au.org.ala.ecodata.reporting.Score score) {
        AggregationConfig aggregationConfig

        String property = 'data.'
        if (score.listName) {
            property+=score.listName+'.'
        }
        property+=score.name

        Aggregation aggregation = new Aggregation([type: score.aggregationType?.name(), property: property, label:score.label])
        if (score.filterBy) {
            Map groupingProperties = score.defaultGrouping()
            aggregationConfig = new FilteredAggregationConfig(
                    label: score.label,
                    childAggregations: [aggregation],
                    filter: new GroupingConfig([property: groupingProperties.property, filterValue: groupingProperties.filterBy, type: groupingProperties.type]))
        } else if (score.groupBy) {
            Map groupingProperties = score.defaultGrouping()
            aggregationConfig = new GroupingAggregationConfig(
                    label: score.label,
                    childAggregations: [aggregation],
                    groups: new GroupingConfig([property: groupingProperties.property, type: groupingProperties.type]))
        } else {
            aggregationConfig = aggregation
        }
        // All scores need to be filtered by output
        GroupingConfig outputFilter = new GroupingConfig(property: 'name', filterValue: score.outputName, type:'filter')
        FilteredAggregationConfig filteredConfig = new FilteredAggregationConfig([label:score.label, filter:outputFilter, childAggregations:[aggregationConfig]])

        filteredConfig
    }

    private def aggregateActivity (AggregatorIf aggregator, Map activity) {

        activity.outputs.each { output ->
            Map outputData = outputService.toMap(output)
            outputData.activity = activity
            aggregator.aggregate(outputData)
        }
    }

    private def updateMetadata(Map activity, Map metadata) {

        metadata.distinctActivities << activity.activityId
        if (!metadata.activitiesByType[activity.type]) {
            metadata.activitiesByType[activity.type] = 0
        }
        metadata.activitiesByType[activity.type] = metadata.activitiesByType[activity.type] + 1

        metadata.distinctProjects << activity?.projectId
        if (activity?.sites) {
            metadata.distinctSites << activity.sites.siteId
        }
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
    def projectSummary(String projectId, List aggregationSpec, boolean approvedActivitiesOnly = false) {


       // We definitely could be smarter about this query - only getting activities with outputs of particular
        // types or containing particular scores for example.
        List activities = activityService.findAllForProjectId(projectId, 'FLAT')
        if (approvedActivitiesOnly) {
            activities = activities.findAll{it.publicationStatus == 'published'}
        }

        AggregationConfig aggregationConfig = aggregationConfigFromScores(aggregationSpec)
        AggregatorIf aggregator = new AggregatorFactory().createAggregator(aggregationConfig)

        activities.each { activity ->
            aggregateActivity(aggregator, activity)
        }

        GroupedAggregationResult allResults = aggregator.result()

        return postProcessOutputData(allResults.groups[0]?.results?:[], aggregationSpec)
    }

    def outputTargetReport(List filters, String searchTerm = null) {
        def scores = Score.findAllByIsOutputTarget(true)

        outputTargetReport(filters, searchTerm, scores)
    }

    def outputTargetReport(List filters, String searchTerm, scores) {

        def groupingSpec = [property:'activity.programSubProgram', type:'discrete']

        aggregate(filters, searchTerm, scores, groupingSpec)
    }

    def outputTargetsBySubProgram(params) {
        outputTargetsBySubProgram(params, null)
    }

    def outputTargetsBySubProgram(params, scores) {

        params += [offset:0, max:100]
        def targetsBySubProgram = [:]
        def queryString = params.query ?: "*:*"
        def results = elasticSearchService.search(queryString, params, "homepage")

        def propertyAccessor = new PropertyAccessor("target")
        def total = results.hits.totalHits
        while (params.offset < total) {

            def hits = results.hits.hits
            for (def hit : hits) {
                def project = hit.source
                project.outputTargets?.each { target ->
                    def program = project.associatedProgram + ' - ' + project.associatedSubProgram
                    if (!targetsBySubProgram[program]) {
                        targetsBySubProgram[program] = [projectCount:0]
                    }
                    if (target.scoreLabel && target.target) {
                        if (!scores || scores.find {it.label == target.scoreLabel}) {
                            def value = propertyAccessor.getPropertyAsNumeric(target)
                            if (value == null) {
                                log.warn project.projectId + ' ' + target.scoreLabel + ' ' + target.target + ':' + value
                            } else {
                                if (!targetsBySubProgram[program][target.scoreLabel]) {
                                    targetsBySubProgram[program][target.scoreLabel] = [count: 0, total: 0]
                                }
                                targetsBySubProgram[program][target.scoreLabel].total += value
                                targetsBySubProgram[program][target.scoreLabel].count++

                            }
                        }
                    }
                }
            }
            params.offset += params.max

            results  = elasticSearchService.search("*:*", params, "homepage")
        }
        targetsBySubProgram
    }

    /**
     * Produces a list of users for the matching projects.  Also adds any users containing the extra roles, even
     * if they don't have any explicit project access
     */
    def userSummary(Set projectIds, List roles) {

        def levels = [100:'admin',60:'caseManager', 40:'editor', 20:'favourite']

        def userSummary = [:]
        def users = UserPermission.findAllByEntityIdInList(projectIds).groupBy{it.userId}
        users.each { userId, projects ->
            def userDetails = userService.lookupUserDetails(userId)


            userSummary[userId] = [userId:userDetails.userId, name:userDetails.displayName, email:userDetails.userName, role:'FC_USER']
            userSummary[userId].projects = projects.collect {
                def project = projectService.get(it.entityId, ProjectService.FLAT)

                [projectId: project.projectId, grantId:project.grantId, externalId:project.externalId, name:project.name, access:levels[it.accessLevel.code]]
            }
        }


        int batchSize = 500

        String url = grailsApplication.config.userDetails.admin.url
        url += "/userRole/list?format=json&max=${batchSize}&role="
        roles.each { role ->
            int offset = 0
            Map result = webService.getJson(url+role+'&offset='+offset)

            while (offset < result?.resp?.count && !result?.error) {

                List usersForRole = result?.resp?.users ?: []
                usersForRole.each { user ->
                    if (userSummary[user.userId]) {
                        userSummary[user.userId].role = role
                    }
                    else {
                        user = [:]
                        userSummary[user.userId] = user
                        def userDetails = userService.lookupUserDetails(user.userId)
                        user.userId = userDetails.userId
                        user.name = userDetails.displayName
                        user.email = userDetails.userName
                        user.projects = []
                    }
                }

                offset += batchSize
                result = webService.getJson(url+role+'&offset='+offset)
            }

            if (!result || result.error) {
                log.error("Error getting user details for role: "+role)
                return
            }
        }

        userSummary
    }

    def exportShapeFile(projectIds, name, outputStream) {

        ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
        builder.setName(name)
        projectIds.each { projectId ->
            builder.addProject(projectId)
        }
        builder.writeShapefile(outputStream)
    }
}
