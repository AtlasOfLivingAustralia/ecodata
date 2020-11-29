package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import graphql.schema.DataFetchingEnvironment
import org.elasticsearch.action.search.SearchResponse

class ProjectsFetcher implements graphql.schema.DataFetcher<List<Project>> {

    public ProjectsFetcher(ProjectService projectService, ElasticSearchService elasticSearchService, PermissionService permissionService) {
        this.projectService = projectService
        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService

    ProjectService projectService

    @Override
    List<Project> get(DataFetchingEnvironment environment) throws Exception {


        // Search ES, applying the user role in the process...


        // What should happen if we get a "show me all" type query?

        // Should we return the public view for all public projects (is that all projects?) we have data for?

        // e.g. should the role check only apply during the mapping phase?  In which case we need a bulk query of permissions to determine a list of project ids we can get full resolution data for?
        // Or do we do two queries, one for full resolution, one for the rest (how do we sort/page if we do two queries?)



        return queryElasticSearch(environment)
    }

    private List<Project> queryElasticSearch(DataFetchingEnvironment environment) {
        // Retrieve projectIds only from elasticsearch.


        // add pagination results.
        String userId = environment.context.userId ?: '1493'
        String query = environment.arguments.term ?:"*:*"
        SearchResponse searchResponse = elasticSearchService.searchWithSecurity(userId, query, [include:'projectId'], ElasticIndex.HOMEPAGE_INDEX)

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}

        // Split projects into those the user has full read permission & those they don't
        List publicProjectIds = []
        List fullProjectIds = []


        // Alternative here is to also return the userIds from the ES query and see if the user is in the result.
        // we could also map directly from the ES, but this would require a different approach (maybe creating
        // and binding the domain objects from the ES data?)
        projectIds.each {
            boolean readable = permissionService.checkUserPermission(userId, it, Project.name, "read")
            readable ? publicProjectIds << it : publicProjectIds << it
        }

        Map publicView = [name:true, description:true, projectId:true]

        Map publicProjects = [:]
        FindIterable findIterable = Project.find(Filters.in("projectId", publicProjectIds))
        findIterable.projection(publicView).each { Project project ->
            publicProjects.put(project.projectId, project)
        }

        Map fullProjects = [:]
        findIterable = Project.find(Filters.in("projectId", fullProjectIds))
        findIterable.each { Project project ->
            fullProjects.put(project.projectId, project)
        }

        List projects = projectIds.collect {
            publicProjects.containsKey(it) ? publicProjects[it] : fullProjects[it]
        }

        projects
    }
}
