package au.org.ala.ecodata.graphql

import au.org.ala.ecodata.*
import graphql.schema.DataFetchingEnvironment
import org.elasticsearch.action.search.SearchResponse

class ActivityFetcher implements graphql.schema.DataFetcher<List<Site>> {

    public ActivityFetcher(ElasticSearchService elasticSearchService, PermissionService permissionService) {

        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService
    ActivityService activityService


    @Override
    List<Activity> get(DataFetchingEnvironment environment) throws Exception {

        String userId = environment.context.user?.userId
        String query = environment.arguments.term ?:"*:*"
        SearchResponse searchResponse = elasticSearchService.searchWithSecurity(null, query, [include:'projectId', max:65000], ElasticIndex.HOMEPAGE_INDEX)

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}


        Activity.findAllByProjectIdInList(projectIds, [max:10])

        // Do we want to restrict API use based on hubs?
//        if (!userId || !environment.context.permissionService.checkUserPermission(userId, environment.fieldDefinition.name, "API", "read")) {
//            throw new GraphQLException("No permission")
//        }

        // Search ES, applying the user role in the process...


        // What should happen if we get a "show me all" type query?

        // Should we return the public view for all public projects (is that all projects?) we have data for?

        // e.g. should the role check only apply during the mapping phase?  In which case we need a bulk query of permissions to determine a list of project ids we can get full resolution data for?
        // Or do we do two queries, one for full resolution, one for the rest (how do we sort/page if we do two queries?)




    }

}
