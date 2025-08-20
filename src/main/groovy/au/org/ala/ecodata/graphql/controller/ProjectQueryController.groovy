package au.org.ala.ecodata.graphql.controller

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder.EcodataGraphQLContext
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import au.org.ala.ecodata.graphql.models.TargetMeasure
import grails.compiler.GrailsCompileStatic
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.ContextValue
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


    @QueryMapping
    Map<Integer, List<Project>> searchMeritProjects(DataFetchingEnvironment env) {
        Map hub = (Map)env.graphQlContext.get("hub")
        if (hub.urlPath != "merit") {
            throw new IllegalArgumentException("The searchMeritProjects query is only available via the MERIT hub path")
        }
        projectsFetcher.searchMeritProject(env)
    }

    @SchemaMapping(typeName = "Project", field = "reports")
    List<Report> reports(Project project, GraphQLContext context) {

        context.put("project", project)

        List<Report> resultList = (List<Report>)reportingService.search(projectId:project.projectId, [max:100, offset:0, sort:'dateCreated', order:'desc'])
        resultList
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
    List<Site> sites(Project project, @ContextValue EcodataGraphQLContext securityContext) {

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


    @SchemaMapping(typeName = "OutputTarget", field = "targetMeasure")
    CompletableFuture<TargetMeasure> targetMeasure(OutputTarget outputTarget, DataLoader<String, TargetMeasure> dataLoader) {
        dataLoader.load(outputTarget.scoreId)
    }

    private static CompletableFuture targetMeasuresFromScoreIds(List<String> scoreIds, DataLoader<String, TargetMeasure> targetMeasureDataLoader) {
        List<CompletableFuture> futures = scoreIds?.collect {
            targetMeasureDataLoader.load(it)
        } ?: []

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply{futures.collect{it.join()}}
    }

}
