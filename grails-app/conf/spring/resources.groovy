import au.org.ala.ecodata.graphql.ActivityFetcher
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.EcodataGraphQLCustomiser
import au.org.ala.ecodata.graphql.ProjectsFetcher
import au.org.ala.ecodata.graphql.SitesFetcher

import au.org.ala.ecodata.converter.ISODateBindingConverter

// Place your Spring DSL code here
beans = {
    ecodataGraphQLCustomiser(EcodataGraphQLCustomiser)
    projectsFetcher(ProjectsFetcher)
    sitesFetcher(SitesFetcher)
    activitiesFetcher(ActivityFetcher)
    graphQLContextBuilder(EcodataGraphQLContextBuilder)
    formattedStringConverter ISODateBindingConverter
}
