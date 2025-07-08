import au.org.ala.ecodata.converter.ISODateBindingConverter
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.EcodataGraphQLCustomiser
import au.org.ala.ecodata.graphql.GraphQLDomainPropertyManager
import au.org.ala.ecodata.graphql.config.GraphQLConfig
import au.org.ala.ecodata.graphql.fetchers.ActivityFetcher
import au.org.ala.ecodata.graphql.fetchers.OutputFetcher
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import au.org.ala.ecodata.graphql.fetchers.SitesFetcher
import au.org.ala.ecodata.graphql.controller.AssociatedOrgQueryController
import au.org.ala.ecodata.graphql.controller.ProjectQueryController

// Place your Spring DSL code here
beans = {
    ecodataGraphQLCustomiser(EcodataGraphQLCustomiser)
    projectsFetcher(ProjectsFetcher)
    sitesFetcher(SitesFetcher)
    activitiesFetcher(ActivityFetcher)
    outputFetcher(OutputFetcher)
    graphQLContextBuilder(EcodataGraphQLContextBuilder)

    formattedStringConverter ISODateBindingConverter
    graphQLDomainPropertyManager(GraphQLDomainPropertyManager)

    graphqlConfig(GraphQLConfig)

    projectQueryController(ProjectQueryController)
    associatedOrgQueryController(AssociatedOrgQueryController)
    ecodataGraphQLContextBuilder(EcodataGraphQLContextBuilder)
    projectsFetcher(ProjectsFetcher)

}
