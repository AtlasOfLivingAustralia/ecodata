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

    def findScoresByScoreId(List scoreIds) {
        Score.findAllByScoreIdInList(scoreIds)
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

        AggregationConfig topLevelConfig = aggregationConfigFromScores(toAggregate, topLevelGrouping)

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

    private AggregationConfig aggregationConfigFromScores(List<Score> scores = null, Map topLevelGrouping = null) {
        if (!scores) {
            scores = Score.findAll()
        }

        List config = scores.collect{
            it.configuration.label = it.label
            it.configuration
        }
        GroupingConfig topLevelGroupingConfig = new GroupingConfig(topLevelGrouping?:[:])

        AggregationConfig aggregationConfig
        if (topLevelGrouping?.filterValue) {
            aggregationConfig = new FilteredAggregationConfig(childAggregations: config, filter:topLevelGroupingConfig)
        }
        else {
            aggregationConfig = new GroupingAggregationConfig(childAggregations: config, groups:topLevelGroupingConfig)
        }
        aggregationConfig
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
     * @return the results of the aggregration.  The results will be a List of Maps, the structure of each Map is
     * described in @see au.org.ala.ecodata.reporting.Aggregation.results()
     *
     */
    List projectSummary(String projectId, List aggregationSpec, boolean approvedActivitiesOnly = false, Map topLevelAggregationConfig = null) {


       // We definitely could be smarter about this query - only getting activities with outputs of particular
        // types or containing particular scores for example.
        List activities = activityService.findAllForProjectId(projectId, 'FLAT')
        if (approvedActivitiesOnly) {
            activities = activities.findAll{it.publicationStatus == 'published'}
        }

        AggregationConfig aggregationConfig = aggregationConfigFromScores(aggregationSpec, topLevelAggregationConfig)
        AggregatorIf aggregator = new AggregatorFactory().createAggregator(aggregationConfig)

        activities.each { activity ->
            aggregateActivity(aggregator, activity)
        }

        GroupedAggregationResult allResults = aggregator.result()

        allResults.groups.each { group ->
            group?.results = postProcessOutputData(allResults.groups[0]?.results?:[], aggregationSpec)
        }
        return topLevelAggregationConfig ? allResults.groups : allResults.groups[0]?.results
    }

    def outputTargetReport(List filters, String searchTerm = null) {
        def scores = Score.findAllByIsOutputTarget(true)

        outputTargetReport(filters, searchTerm, scores)
    }

    def outputTargetReport(List filters, String searchTerm, scores, boolean approvedActivitiesOnly = true) {

        def groupingSpec = [property:'activity.programSubProgram', type:'discrete']

        aggregate(filters, searchTerm, scores, groupingSpec, approvedActivitiesOnly)
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
                        targetsBySubProgram[program] = [:]
                    }
                    if ((target.scoreId || target.scoreLabel) && target.target) {
                        if (!scores || scores.find {(it.scoreId == target.scoreId) || (it.label == target.scoreLabel)}) {
                            def value = propertyAccessor.getPropertyAsNumeric(target)
                            if (value == null) {
                                log.warn project.projectId + ' ' + target.scoreLabel + ' ' + target.target + ':' + value
                            } else {
                                String label = target.scoreLabel
                                if (!label && scores) {
                                    def score = scores.find({it.scoreId == target.scoreId})
                                    label = score.label
                                }
                                if (!targetsBySubProgram[program][label]) {
                                    targetsBySubProgram[program][label] = [scoreId: target.scoreId, count: 0, total: 0]
                                }
                                targetsBySubProgram[program][label].total += value
                                targetsBySubProgram[program][label].count++

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
        Map users = UserPermission.findAllByEntityIdInList(projectIds).groupBy{it.userId}

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

            while (offset < result?.count && !result?.error) {

                List usersForRole = result?.users ?: []
                usersForRole.each { user ->
                    if (userSummary[user.userId]) {
                        userSummary[user.userId].role = role
                    }
                    else {
                        user.projects = []
                        user.name = (user.firstName ?: "" + " " +user.lastName ?: "").trim()
                        user.role = role
                        userSummary[user.userId] = user
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
