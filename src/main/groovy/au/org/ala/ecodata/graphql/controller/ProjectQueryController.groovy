package au.org.ala.ecodata.graphql.controller

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder.EcodataGraphQLContext
import au.org.ala.ecodata.graphql.fetchers.PagedList
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import grails.compiler.GrailsCompileStatic
import grails.gorm.PagedResultList
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
}
