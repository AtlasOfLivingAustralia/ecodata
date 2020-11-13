import au.org.ala.ecodata.converter.ISODateBindingConverter

import au.org.ala.ecodata.graphql.ActivityFetcher
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.EcodataGraphQLCustomiser
import au.org.ala.ecodata.graphql.ProjectsFetcher
import au.org.ala.ecodata.graphql.SitesFetcher

// Place your Spring DSL code here
beans = {
    formattedStringConverter ISODateBindingConverter
    ecodataGraphQLCustomiser(EcodataGraphQLCustomiser)
    projectsFetcher(ProjectsFetcher)
    sitesFetcher(SitesFetcher)
    activitiesFetcher(ActivityFetcher)
    graphQLContextBuilder(EcodataGraphQLContextBuilder)
}
