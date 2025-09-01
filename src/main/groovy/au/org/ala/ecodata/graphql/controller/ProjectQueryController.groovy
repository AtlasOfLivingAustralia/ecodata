package au.org.ala.ecodata.graphql.controller

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import au.org.ala.ecodata.graphql.models.TargetMeasure
import au.org.ala.ecodata.reporting.GroupedResult
import grails.compiler.GrailsCompileStatic
import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import org.dataloader.DataLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.LocalContextValue
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

import java.util.concurrent.CompletableFuture

@GrailsCompileStatic
@Controller
class ProjectQueryController {

    @Autowired
    UserService userService

    @Autowired
    ProjectsFetcher projectsFetcher

    @Autowired
    ReportingService reportingService

    @Autowired
    ReportService reportService

    @Autowired
    ProgramService programService

    @Autowired
    ManagementUnitService managementUnitService

    @Autowired
    SiteService siteService

    @Autowired
    ProjectService projectService


    @QueryMapping
    Map<Integer, List<Project>> searchMeritProjects(DataFetchingEnvironment env) {
        Map hub = (Map)env.graphQlContext.get("hub")
        if (hub.urlPath != "merit") {
            throw new IllegalArgumentException("The searchMeritProjects query is only available via the MERIT hub path")
        }
        projectsFetcher.searchMeritProject(env)
    }

    @SchemaMapping(typeName = "Project", field = "reports")
    DataFetcherResult<List<Report>> reports(Project project, DataFetchingFieldSelectionSet selectionSet) {
        // Create a new local context and store the author value
        GraphQLContext localContext = GraphQLContext.getDefault()
                .put("project", project);

        if (selectionSet.contains("deliveredAgainstTargets")) {
            Map<String, List> deliveredByActivityId = [:]
            List<String> scoreIds = project.outputTargets?.collect {it.scoreId}
            if (scoreIds) {
                Map aggregationConfig = [type:'discrete', property:'activity.activityId']
                List<GroupedResult> results = (List<GroupedResult>)projectService.projectMetrics(project.projectId, false, true, scoreIds, aggregationConfig, false, true)
                deliveredByActivityId = results?.collectEntries {
                    [(it.group): (List)it.results]
                }

            }
            localContext.put("deliveredByActivityId", deliveredByActivityId)

        }

        DataFetcherResult.Builder<List<Report>> resultBuilder = DataFetcherResult.newResult()

        List<Report> resultList = (List<Report>)reportingService.search(projectId:project.projectId, [max:100, offset:0, sort:'dateCreated', order:'desc'])
        return resultBuilder
                .data(resultList)
                .localContext(localContext)
                .build()
    }

    @SchemaMapping(typeName = "Project", field = "documents")
    List<Document> documents(Project project) {
        Document.findAllByProjectIdAndStatusNotEqual(project.projectId, Status.DELETED, [sort: 'dateCreated', order: 'desc'])
    }

    @SchemaMapping(typeName = "Project", field = "program")
    CompletableFuture<Program> program(Project project, DataLoader<String, Program> programDataLoader) {
        programDataLoader.load(project.programId)
    }

    @SchemaMapping(typeName = "Project", field = "managementUnit")
    CompletableFuture<ManagementUnit> managementUnit(Project project, DataLoader<String, ManagementUnit> managementUnitDataLoader) {
        managementUnitDataLoader.load(project.managementUnitId)
    }

    @SchemaMapping(typeName = "Report", field = "activity")
    CompletableFuture<Activity> activity(Report report, DataLoader<String, Activity> activityDataLoader) {

        activityDataLoader.load(report.activityId)

    }

    @SchemaMapping(typeName = "Project", field = "sites")
    List<Site> sites(Project project) {

        Site.findAllByProjectsAndStatusNotEqual(project.projectId, Status.DELETED, [sort: 'dateCreated', order: 'asc'])

    }

    @SchemaMapping(typeName = "Site", field = "geoJson")
    Site geoJson(Site site) {
        // The scalar type converter will handle the conversion to GeoJSON
        site
    }

    @SchemaMapping(typeName = "Activity", field = "outputs")
    CompletableFuture<List<Output>> outputs(Activity activity, DataLoader<String, List<Output>> outputDataLoader, GraphQLContext context) {

        context.put("activity", activity)
        outputDataLoader.load(activity.activityId)

    }

    @SchemaMapping(typeName = "Output", field = "service")
    Service service(Output output, GraphQLContext context ) {

        Activity activity = context.get("activity")
        if (!activity) {
            return null
        }
        if (activity.activityId != output.activityId) {
            throw new IllegalStateException("Activity in context does not match activity for output")
        }
        context.delete("activity")

        String formName = activity.type

        Service.findAll().find {
           it.outputs.find {ServiceForm form -> form.formName == formName && form.sectionName == output.name}
        }

    }

    @SchemaMapping(typeName = "Baseline", field = "targetMeasures")
    CompletableFuture<List<TargetMeasure>> targetMeasures(Baseline baseline, DataLoader<String, TargetMeasure> dataLoader) {
        targetMeasuresFromScoreIds(baseline.relatedTargetMeasures, dataLoader)
    }


    @SchemaMapping(typeName = "KeyThreat", field = "targetMeasures")
    CompletableFuture<List<TargetMeasure>> targetMeasures(KeyThreat keyThreat, DataLoader<String, TargetMeasure> dataLoader) {
        targetMeasuresFromScoreIds(keyThreat.relatedTargetMeasures, dataLoader)
    }


    @SchemaMapping(typeName = "MonitoringMethodology", field = "targetMeasures")
    CompletableFuture<List<TargetMeasure>> targetMeasures(MonitoringMethodology monitoringMethodology, DataLoader<String, TargetMeasure> dataLoader) {
        targetMeasuresFromScoreIds(monitoringMethodology.relatedTargetMeasures, dataLoader)
    }

    @SchemaMapping(typeName = "MeriPlan", field = "statusChangeHistory")
    List<StatusChange> statusChangeHistory(MeriPlan meriPlan) {
        projectService.getMeriPlanApprovalHistory(meriPlan.projectId, false)
    }


    @SchemaMapping(typeName = "OutputTarget", field = "targetMeasure")
    CompletableFuture<TargetMeasure> targetMeasure(OutputTarget outputTarget, DataLoader<String, TargetMeasure> dataLoader) {
        dataLoader.load(outputTarget.scoreId)
    }

    @SchemaMapping(typeName = "DeliveredAgainstTarget", field = "targetMeasure")
    CompletableFuture<TargetMeasure> targetMeasure(DeliveredAgainstTarget deliveredAgainstTarget, DataLoader<String, TargetMeasure> dataLoader) {
        dataLoader.load(deliveredAgainstTarget.scoreId)
    }

    @SchemaMapping(typeName = "Report", field = "deliveredAgainstTargets")
    List<DeliveredAgainstTarget> deliveredAgainstTargets(Report report,
                                                         @LocalContextValue Project project,
                                                         @LocalContextValue Map<String, List> deliveredByActivityId) {

        List<DeliveredAgainstTarget> deliveredAgainstTargets = null
        if (project?.projectId != report.projectId) {
            throw new IllegalStateException("Project in context does not match project for report")
        }
        if (deliveredByActivityId) {
            List<Map> deliveredAgainstTargetsData = deliveredByActivityId[report.activityId]
            if (deliveredAgainstTargetsData) {
                deliveredAgainstTargets = deliveredAgainstTargetsData.collect { new DeliveredAgainstTarget(it) }
            }
        }
        deliveredAgainstTargets?.findAll{it.amountDelivered > 0}

    }

    @SchemaMapping(typeName = "Project", field = "deliveredAgainstTargets")
    List<DeliveredAgainstTarget> deliveredAgainstTargets(Project project) {

        boolean approvedOnly = true
        List scoreIds = []
        project.outputTargets?.each {
            scoreIds << it.scoreId
        }
        if (!scoreIds) {
            return null
        }
        List<Map> metrics = (List<Map>)projectService.projectMetrics(project.projectId, false, approvedOnly, scoreIds, null, true, true)
        metrics.collect { new DeliveredAgainstTarget(it) }

    }

    private static CompletableFuture targetMeasuresFromScoreIds(List<String> scoreIds, DataLoader<String, TargetMeasure> targetMeasureDataLoader) {
        List<CompletableFuture> futures = scoreIds?.collect {
            targetMeasureDataLoader.load(it)
        } ?: []

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply{futures.collect{it.join()}}
    }

}
