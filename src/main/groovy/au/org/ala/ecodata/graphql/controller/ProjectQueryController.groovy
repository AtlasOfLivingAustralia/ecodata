package au.org.ala.ecodata.graphql.controller

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder.EcodataGraphQLContext
import au.org.ala.ecodata.graphql.fetchers.PagedList
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import grails.compiler.GrailsCompileStatic
import grails.gorm.PagedResultList
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
    ProgramService programService

    @Autowired
    ManagementUnitService managementUnitService

    @Autowired
    SiteService siteService


    @QueryMapping
    Map<Integer, List<Project>> searchMeritProjects(DataFetchingEnvironment env) {
        projectsFetcher.searchMeritProject(env)
    }

    @SchemaMapping(typeName = "Project", field = "reports")
    PagedList reports(Project project, @ContextValue EcodataGraphQLContext securityContext) {
        if (!securityContext.hasPermission(project)) {
            return null
        }
        PagedResultList resultList = (PagedResultList)reportingService.search(projectId:project.projectId, [max:100, offset:0, sort:'dateCreated', order:'desc'])
        new PagedList(resultList)
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
    CompletableFuture<Activity> activity(Report report, DataLoader<String, Activity> activityDataLoader, @ContextValue EcodataGraphQLContext securityContext) {
//        if (!securityContext.hasPermission(report)) {
//            return null
//        }
        activityDataLoader.load(report.activityId)

    }


    @SchemaMapping(typeName = "Project", field = "sites")
    List<Site> sites(Project project, @ContextValue EcodataGraphQLContext securityContext) {
        if (!securityContext.hasPermission(project)) {
            return null
        }
        Site.findAllByProjectsAndStatusNotEqual(project.projectId, Status.DELETED, [sort: 'dateCreated', order: 'asc'])

    }

    @SchemaMapping(typeName = "Site", field = "geoJson")
    Site geoJson(Site site) {
        // The scalar type converter will handle the conversion to GeoJSON
        site
    }

    @SchemaMapping(typeName = "Activity", field = "outputs")
    CompletableFuture<List<Output>> outputs(Activity activity, DataLoader<String, List<Output>> outputDataLoader, GraphQLContext context, DataFetchingEnvironment dfe) {
//        if (!securityContext.hasPermission(activity)) {
//            return null
//        }
        dfe.graphQlContext.put("activity", activity)
        outputDataLoader.load(activity.activityId)

    }

    @SchemaMapping(typeName = "Output", field = "service")
    Service service(Output output, GraphQLContext context ) {
//        if (!securityContext.hasPermission(output)) {
//            return null
//        }

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

}
