import au.org.ala.ecodata.graphql.fetchers.ActivityFetcher
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.EcodataGraphQLCustomiser
import au.org.ala.ecodata.graphql.fetchers.OutputFetcher
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import au.org.ala.ecodata.graphql.fetchers.SitesFetcher
import au.org.ala.ecodata.converter.ISODateBindingConverter

import au.org.ala.ws.security.AlaRoleMapper
import au.org.ala.ecodata.SecurityConfig
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.filter.OrderedFilter

import au.org.ala.ws.security.JwtService
import au.org.ala.ws.security.LegacyApiKeyService
import au.org.ala.ws.security.AlaWebServiceAuthFilter
import org.springframework.web.client.RestTemplate

// Place your Spring DSL code here
beans = {
    ecodataGraphQLCustomiser(EcodataGraphQLCustomiser)
    projectsFetcher(ProjectsFetcher)
    sitesFetcher(SitesFetcher)
    activitiesFetcher(ActivityFetcher)
    outputFetcher(OutputFetcher)
    graphQLContextBuilder(EcodataGraphQLContextBuilder)

    formattedStringConverter ISODateBindingConverter

    restService(RestTemplate)
    jwtService(JwtService)
    legacyApiKeyService(LegacyApiKeyService)
    alaWebServiceAuthFilter(AlaWebServiceAuthFilter)
    alaRoleMapper(AlaRoleMapper)
    alaSecurityConfig(SecurityConfig)
    securityFilterChainRegistration(FilterRegistrationBean) {
        filter = ref("springSecurityFilterChain")
        order = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 25 // This needs to be before the GrailsWebRequestFilter which is +30
    }
}
