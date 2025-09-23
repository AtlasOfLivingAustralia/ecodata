import au.org.ala.ecodata.caching.EcodataCacheKeyGenerator
import au.org.ala.ecodata.converter.ISODateBindingConverter
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.config.GraphQLConfig
import au.org.ala.ecodata.graphql.controller.AssociatedOrgQueryController
import au.org.ala.ecodata.graphql.controller.ProjectQueryController
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher

// Place your Spring DSL code here
beans = {
    ecodataGraphQlContextBuilder(EcodataGraphQLContextBuilder)
    projectsFetcher(ProjectsFetcher)
    formattedStringConverter ISODateBindingConverter
    customCacheKeyGenerator(EcodataCacheKeyGenerator)

    graphqlConfig(GraphQLConfig)

    projectQueryController(ProjectQueryController)
    associatedOrgQueryController(AssociatedOrgQueryController)

    projectsFetcher(ProjectsFetcher)

}
