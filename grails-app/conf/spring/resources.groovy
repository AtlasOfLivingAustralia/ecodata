import au.org.ala.ecodata.NoRequestPropagationTaskDecorator
import au.org.ala.ecodata.caching.EcodataCacheKeyGenerator
import au.org.ala.ecodata.converter.ISODateBindingConverter
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.controller.AssociatedOrgQueryController
import au.org.ala.ecodata.graphql.controller.ProjectQueryController
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher

// Place your Spring DSL code here
beans = {
    ecodataGraphQlContextBuilder(EcodataGraphQLContextBuilder)
    projectsFetcher(ProjectsFetcher)
    formattedStringConverter ISODateBindingConverter
    customCacheKeyGenerator(EcodataCacheKeyGenerator)

    projectQueryController(ProjectQueryController)
    associatedOrgQueryController(AssociatedOrgQueryController)

    projectsFetcher(ProjectsFetcher)

    // Overrides the Grails 8 controllers-async bean so Promise tasks don't hold onto a request that may be
    // recycled before the task completes.
    grailsWebRequestTaskDecorator(NoRequestPropagationTaskDecorator)
}
