
import au.org.ala.ecodata.converter.ISODateBindingConverter
import au.org.ala.ecodata.SecurityConfig
import au.org.ala.ws.security.AlaRoleMapper
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.filter.OrderedFilter

// Place your Spring DSL code here
beans = {
    alaSecurityConfig(SecurityConfig)
    alaRoleMapper(AlaRoleMapper)
    securityFilterChainRegistration(FilterRegistrationBean) {
        filter = ref("springSecurityFilterChain")
        order = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 25 // This needs to be before the GrailsWebRequestFilter which is +30
    }
    formattedStringConverter ISODateBindingConverter
}
