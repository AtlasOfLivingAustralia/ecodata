package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import au.org.ala.ecodata.reporting.AggregatorFactory
import au.org.ala.ecodata.reporting.AggregatorIf
import au.org.ala.ecodata.reporting.Aggregration
import au.org.ala.ecodata.reporting.AggregrationConfig
import au.org.ala.ecodata.reporting.GroupingAggregator
import au.org.ala.ecodata.reporting.GroupingConfig
import au.org.ala.ecodata.reporting.PropertyAccessor
import au.org.ala.ecodata.reporting.Score
import au.org.ala.ecodata.reporting.ShapefileBuilder
import org.grails.plugins.csv.CSVReaderUtils


/**
 * The ReportService aggregates and returns output scores.
 * It is also responsible for managing Reports submitted by users.
 */
class ReportService {

    def activityService, elasticSearchService, projectService, siteService, outputService, metadataService, userService, settingService


    /**
     * Creates an aggregation specification from the Scores defined in the activities model.
     */
    def buildReportSpec() {
        def toAggregate = []

        metadataService.activitiesModel().outputs?.each{
            Score.outputScores(it).each { score ->
                def scoreDetails = [score:score]
                toAggregate << scoreDetails
            }
        }
        toAggregate
    }

    def findScoresByLabel(List labels) {
        def scores = []
        metadataService.activitiesModel().outputs?.each{
            Score.outputScores(it).each { score ->
                if (score.label in labels) {
                    scores << [score:score]
                }
            }
        }
        scores
    }

    def findScoresByCategory(String category) {
        def scores = []
        metadataService.activitiesModel().outputs?.each{
            Score.outputScores(it).each { score ->
                if (score.category == category) {
                    scores << [score:score]
                }
            }
        }
        scores
    }

    def runReport(List filters, String reportName, params) {


        //def report = JSON.parse(settingService.getSetting("report.${reportName}"))

        def report = [:]
        report.groupingSpec = [entity:'activity', property:'plannedEndDate', type:'date', format:'MMM yyyy', buckets:params.getList("dates")]
        report.scores = findScoresByCategory("Green Army")
        aggregate(filters, null, report.scores, report.groupingSpec)
    }

    def queryPaginated(List filters, String searchTerm, AggregatorIf aggregator, Closure action) {

        // Only dealing with approved activities.


        Map params = [offset:0, max:100]

        def results = elasticSearchService.searchActivities(filters, params, searchTerm)

        def total = results.hits.totalHits
        while (params.offset < total) {

            def hits = results.hits.hits
            for (def hit : hits) {
                action(aggregator, hit.source)
            }
            params.offset += params.max

            results  = elasticSearchService.searchActivities(filters, params, searchTerm)
        }
    }

    def aggregate(List filters) {
        aggregate(filters, null, buildReportSpec())
    }

    def aggregate(List filters, String searchTerm, List<Map<String, Score>> toAggregate, topLevelGrouping = null) {

        List<AggregrationConfig> config = toAggregate.collect {
            Score score = it.score

            AggregrationConfig aggConfig = new AggregrationConfig(score:new Aggregration([label:score.label, type:score.aggregationType?.name(), property:score.name]))
            if (score.filterBy || score.groupBy) {
                Map props = score.defaultGrouping()
                aggConfig.groups = new GroupingConfig([property:props.property, filterValue:props.filterBy, type:props.type, entity:props.entity])
            }

            aggConfig
        }
        AggregrationConfig topLevelConfig = new AggregrationConfig(children:config, groups:topLevelGrouping?:new GroupingConfig())

        AggregatorIf aggregator = new AggregatorFactory().createAggregator(topLevelConfig)

        queryPaginated(filters, searchTerm, aggregator, this.&aggregateActivity)

        def allResults = aggregator.result()
        def metadata = allResults.metadata
        def results = allResults.result

        def outputData = results.findAll{it.results}
        [outputData:outputData, metadata:[activities: metadata.distinctActivities.size(), sites:metadata.distinctSites.size(), projects:metadata.distinctProjects, activitiesByType:metadata.activitiesByType]]
    }

    private def aggregateActivity (GroupingAggregator aggregator, Map activity) {

        Output.withNewSession {
            def outputs = outputService.findAllForActivityId(activity.activityId, ActivityService.FLAT)
            outputs.each { output ->
                OutputMetadata outputMetadata = new OutputMetadata(metadataService.getOutputDataModel(output.name))
                List outputData = new OutputModelProcessor().flatten(output, outputMetadata, true)

                outputData.each {
                    it.activity = activity
                    aggregator.aggregate(it)
                }
            }
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

        GroupingAggregator aggregator = new GroupingAggregator(null, aggregationSpec)

        activities.each { activity ->
            aggregateActivity(aggregator, activity)
        }

        def results = aggregator.results().results
        return results?results[0].results:[]
    }

    def outputTargetReport(List filters, String searchTerm = null) {
        def scores = []

        def labels = []
        metadataService.activitiesModel().outputs?.each{
            Score.outputScores(it).each { score ->
                if (score.isOutputTarget) {
                    scores << [score: score]
                    labels << score.label
                }
            }
        }
        // Add all supplementary scores from bulk loads that match output targets
        metadataService.activitiesModel().outputs?.each {
            Score.outputScores(it).each { score ->
                if (!score.isOutputTarget && labels.contains(score.label)) {
                    scores << [score:score]
                }
            }
        }
        outputTargetReport(filters, searchTerm, scores)
    }

    def outputTargetReport(List filters, String searchTerm, scores) {

        def groupingSpec = [entity:'activity', property:'programSubProgram', type:'discrete']

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
                        if (!scores || scores.find {it.score.label == target.scoreLabel}) {
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

    /** Temporary method to assist running the user report.  Needs work */
    def userSummary() {

        def levels = [100:'admin',60:'caseManager', 40:'editor', 20:'favourite']

        def userSummary = [:]
        def users = UserPermission.findAllByEntityType('au.org.ala.ecodata.Project').groupBy{it.userId}
        users.each { userId, projects ->
            def userDetails = userService.lookupUserDetails(userId)


            userSummary[userId] = [userId:userDetails.userId, name:userDetails.displayName, email:userDetails.userName, role:'FC_USER']
            userSummary[userId].projects = projects.collect {
                def project = projectService.get(it.entityId, ProjectService.FLAT)

                [projectId: project.projectId, grantId:project.grantId, externalId:project.externalId, name:project.name, access:levels[it.accessLevel.code]]
            }
        }

        // TODO need a web service from auth to support this properly.
        def fcOfficerList = new File('/Users/god08d/Documents/MERIT/Reports/fc_officer.csv')
        def fcReadOnlyList = new File('/Users/god08d/Documents/MERIT/Reports/fc_read_only.csv')
        def fcadminList = new File('/Users/god08d/Documents/MERIT/Reports/fc_admin.csv')

        [fcOfficerList, fcReadOnlyList, fcadminList].each { file ->
            CSVReaderUtils.eachLine(file, { String[] tokens ->
                def userIdStr = tokens[0]
                try {
                    int userId = Integer.parseInt(userIdStr.replaceAll(',', ''))
                    userIdStr = Integer.toString(userId)

                    def user = userSummary[userIdStr]
                    if (!user) {
                        user = [:]
                        userSummary[userId] = user
                        def userDetails = userService.lookupUserDetails(userIdStr)
                        user.userId = userDetails.userId
                        user.name = userDetails.displayName
                        user.email = userDetails.userName
                        user.projects = []
                    }
                    user.role = tokens[2]
                }
                catch (NumberFormatException e) {}
            })
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
