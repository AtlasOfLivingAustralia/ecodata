package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.*
import au.org.ala.web.AuthService
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX

/**
 * The ReportService aggregates and returns output scores.
 * It is also responsible for managing Reports submitted by users.
 */
@Slf4j
class ReportService {

    ActivityService activityService
    ElasticSearchService elasticSearchService
    ProjectService projectService
    SiteService siteService
    OutputService outputService
    MetadataService metadataService
    UserService userService
    AuthService authService

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

    private void queryPaginated(List filters, String searchTerm, Closure action, String index = HOMEPAGE_INDEX) {
        Map params = [offset:0, max:20, fq:filters]

        SearchResponse results = elasticSearchService.search(searchTerm, params, index)
        def total = results.hits.totalHits.value
        while (params.offset < total) {

            results.hits.hits.each { hit ->
                Map project = hit.sourceAsMap
                action(project)
            }
            params.offset += params.max

            results  = elasticSearchService.search(searchTerm, params, index)
        }
    }

    private void queryPaginated(List filters, String searchTerm, boolean approvedActivitiesOnly, AggregatorIf aggregator, Closure action) {

        Closure aggregateActivityData = { Map project ->
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

        queryPaginated(filters, searchTerm, aggregateActivityData)
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
     * @return the results of the aggregation.  The results will be a List of Maps, the structure of each Map is
     * described in @see au.org.ala.ecodata.reporting.Aggregation.results()
     *
     */
    List projectSummary(String projectId, List aggregationSpec, boolean approvedActivitiesOnly = false, Map topLevelAggregationConfig = null) {

        // We definitely could be smarter about this query - only getting activities with outputs of particular
        // types or containing particular scores for example.
        List activities = activityService.findAllForProjectId(projectId, 'FLAT')
        aggregate(activities, aggregationSpec, approvedActivitiesOnly, topLevelAggregationConfig)
    }

    List aggregate(List activities, List aggregationSpec, boolean approvedActivitiesOnly = false, Map topLevelAggregationConfig = null) {
        aggregateActivities(activities, aggregationSpec, approvedActivitiesOnly, topLevelAggregationConfig)
    }

    List aggregateActivities(List activities, List aggregationSpec, boolean approvedActivitiesOnly = false, Map topLevelAggregationConfig = null) {
        if (approvedActivitiesOnly) {
            activities = activities.findAll{it.publicationStatus == Report.REPORT_APPROVED}
        }

        AggregationConfig aggregationConfig = aggregationConfigFromScores(aggregationSpec, topLevelAggregationConfig)
        AggregatorIf aggregator = new AggregatorFactory().createAggregator(aggregationConfig)

        activities.each { activity ->
            aggregateActivity(aggregator, activity)
        }

        GroupedAggregationResult allResults = aggregator.result()

        allResults.groups.each { group ->
            group?.results = postProcessOutputData(group?.results?:[], aggregationSpec)
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
        SearchResponse results = elasticSearchService.search(queryString, params, "homepage")

        def propertyAccessor = new PropertyAccessor("target")
        long total = results.hits.totalHits.value
        while (params.offset < total) {

            SearchHit[] hits = results.hits.hits
            for (SearchHit hit : hits) {
                Map project = hit.sourceAsMap
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
     * Produces a list of users with permissions for the supplied hub
     */
    def userSummary(String hubId, PrintWriter writer) {

        // Find all users with a recorded login to MERIT
        // Find all ACL entries matching those users.
        int batchSize = 100
        writeUserSummaryHeader(writer)
        Map batchOptions =  [max:batchSize, offset:0, sort:'userId', order:'asc']
        List users = User.findAllByLoginHub(hubId,batchOptions)
        while (users) {
            Map<String, Map> permissionsByUser = UserPermission.findAllByUserIdInList(users.collect{it.userId}).groupBy{it.userId}
            Map<String, au.org.ala.web.UserDetails> userDetails = lookupUserDetails(users.collect{it.userId})
            Map<String, Map> userSummary = [:]
            permissionsByUser.each { String userId, List<UserPermission> permissions ->
                User user = users.find{it.userId == userId}
                Map permissionsForHub = processUser(hubId, user, permissions)
                if (userDetails[userId]) {
                    userSummary[userId] = [email:userDetails[userId].email, displayName:userDetails[userId].displayName] + permissionsForHub
                }
                else {
                    userSummary[userId] = permissionsForHub
                }
            }

            writeUsers(userSummary, writer)

            batchOptions.offset += batchSize
            log.info("Processed "+batchOptions.offset+" users for the user summary report")
            users = User.findAllByLoginHub(hubId, batchOptions)
        }

    }


    private Map<String, au.org.ala.web.UserDetails> lookupUserDetails(List<String> userIds) {
        def userList = authService.getUserDetailsById(userIds)
        userList?.users
    }

    private Map processUser(String hubId, User user, List<UserPermission> permissions) {
        Map userSummary = [userId: user.userId, lastLoginTime:user.getUserHub(hubId).lastLoginTime]
        List hubPermissions = permissions.findAll{it.entityType == Hub.class.name}
        userSummary.hubPermissions = hubPermissions.collect{it.accessLevel.name()}

        List projectIds = permissions.findAll{it.entityType == Project.class.name}.collect{it.entityId}
        List muIds = permissions.findAll{it.entityType == ManagementUnit.class.name}.collect{it.entityId}
        List programIds = permissions.findAll{it.entityType == Program.class.name}.collect{it.entityId}

        // TODO not sure what to do about organisations at this time.
        List<Project> projects = Project.findAllByHubIdAndProjectIdInList(hubId, projectIds)
        List<ManagementUnit> mus = ManagementUnit.findAllByHubIdAndManagementUnitIdInList(hubId, muIds)
        List<Program> programs = Program.findAllByHubIdAndProgramIdInList(hubId, programIds)

        userSummary.projects = projects.collect { Project project ->
            AccessLevel level = permissions.find{it.entityId == project.projectId}.accessLevel
            [projectId: project.projectId, grantId: project.grantId, externalId: project.externalId, name: project.name, access: level.name()]
        }
        userSummary.managementUnits = mus.collect { ManagementUnit mu ->
            AccessLevel level = permissions.find{it.entityId == mu.managementUnitId}.accessLevel
            [managementUnitId: mu.managementUnitId, name: mu.name, access: level.name()]
        }
        userSummary.programs = programs.collect { Program program ->
            AccessLevel level = permissions.find{it.entityId == program.programId}.accessLevel
            [programId: program.programId, name: program.name, access: level.name()]
        }
        userSummary
    }

    private void writeUserSummaryHeader(PrintWriter writer) {
        writer.println("User Id, Name, Email, Last Login, Role, Type, ID, Grant ID, External ID, Name, Access Role")
    }

    private void writeUsers(Map<String, Map> userSummary, PrintWriter writer) {
        userSummary.values().each { user->
            boolean firstRow = true
            String role = user.hubPermissions?.join(',')?:'none'
            String userDetails = user.userId+","+user.displayName+","+user.email+","+user.lastLoginTime+","+role+','
            String blanks = ",,,,,"

            user.projects?.each { project ->
                writer.print(firstRow?userDetails:blanks)
                writer.println("Project,"+project.projectId+","+project.grantId+","+project.externalId+",\""+project.name+"\","+project.access)
                firstRow = false
            }
            user.managementUnits?.each {
                writer.print(firstRow?userDetails:blanks)
                writer.println("Management Unit,"+it.managementUnitId+",,,\""+it.name+"\","+it.access)
                firstRow = false
            }
            user.programs?.each {
                writer.print(firstRow?userDetails:blanks)
                writer.println("Program,"+it.programId+",,,\""+it.name+"\","+it.access)
                firstRow = false
            }
        }
    }

    def exportShapeFile(projectIds, name, outputStream) {

        ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
        builder.setName(name)
        projectIds.each { projectId ->
            builder.addProject(projectId)
        }
        builder.writeShapefile(outputStream)
    }

    /**
     *
     * @param id management unit Id
     * @return
     */
    List getReportsOfManagementUnit(String id){
        List<Report> reports = Report.findAllByManagementUnitIdAndStatusNotEqual(id,Status.DELETED)
        List<Map> activities = activityService.getAll(reports.activityId,['all'])

        List hasReports = activities.findAll{
            it.outputs?.size()>0
        }
        hasReports.each{
            def report = reports.find {it.activityId == it.activityId}
            if (report){
                it['reportId'] = report['reportId']
                it['reportName'] = report['name']
                it['reportDesc'] = report['description']
            }
        }
        return hasReports
    }

    /**
     *
     * @param muIds a list of management unit Ids
     * @return
     */
    Date[] getPeriodOfManagmentUnitReport(String[] muIds ){
        List<String> activityIds = Report.findAllByManagementUnitIdInList(muIds.toList()).activityId
        Date[] period = activityService.getPeriod(activityIds)
    }
}
