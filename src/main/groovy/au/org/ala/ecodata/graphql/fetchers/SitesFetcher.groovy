package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import graphql.schema.DataFetchingEnvironment
import org.elasticsearch.action.search.SearchResponse

class SitesFetcher implements graphql.schema.DataFetcher<List<Site>> {

    public SitesFetcher(ProjectService projectService, ElasticSearchService elasticSearchService, PermissionService permissionService) {
        this.projectService = projectService
        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService

    ProjectService projectService

    @Override
    List<Site> get(DataFetchingEnvironment environment) throws Exception {

        String userId = environment.context.user?.userId

        // Do we want to restrict API use based on hubs?
//        if (!userId || !environment.context.permissionService.checkUserPermission(userId, environment.fieldDefinition.name, "API", "read")) {
//            throw new GraphQLException("No permission")
//        }

        // Search ES, applying the user role in the process...


        // What should happen if we get a "show me all" type query?

        // Should we return the public view for all public projects (is that all projects?) we have data for?

        // e.g. should the role check only apply during the mapping phase?  In which case we need a bulk query of permissions to determine a list of project ids we can get full resolution data for?
        // Or do we do two queries, one for full resolution, one for the rest (how do we sort/page if we do two queries?)



        return queryElasticSearch(environment)
    }

    private List<Site> queryElasticSearch(DataFetchingEnvironment environment) {
        // Retrieve projectIds only from elasticsearch.

        // Need to only retrieve sites for which we actually have access to.  It's a bit tricky as pagination can mean we
        // can't post process data.  e.g. we want from 100-200 and are post processing we have to query from 0, and post filter,
        // throwing away the first 100.

        // Might have to include an ACL in ES or the database to make it work properly.  Or use projects as ACL - this may not work, as
        // someone with access to all MERIT projects could result in a large project list going to the sites query (e.g. 3500...)
        // I assume we want to be able to query sites based on projects anyway though - e.g. programs.

        // ES limits terms query to ~65,000 by default, which may eventually cause problems that would require building a
        // new index.

        // Another way to deal with this would be to limit site / activity queries to hub based ones and include hub in the
        // index?

        // Another way is to build the query with hub information in the query?
        // e.g. hub in <xyz> or <userId> in ACL?   Do we need a way to give access to all Hubs explicity?

        // Otherwise we need to be adding hub permissions to the ACL, which will require re-indexing a lot of projects when
        // hub permissions change? (maybe that's OK)?

        SearchResponse searchResponse = elasticSearchService.searchWithSecurity(null, "*:*", [include:'projectId', max:65000], ElasticIndex.HOMEPAGE_INDEX)

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}

        Site.findAllByProjectsInList(projectIds, [max:10])
    }
}
