package au.org.ala.ecodata

import grails.core.support.GrailsConfigurationAware
import grails.config.Config

class AuditInterceptor implements GrailsConfigurationAware {

    String httpRequestHeaderForUserId
    UserService userService

    public AuditInterceptor() {
        matchAll()
    }

    boolean before() {
        // userId is set from either the request param userId or failing that it tries to get it from
        // the UserPrincipal (assumes ecodata is being accessed directly via admin page)
        def userId = request.getHeader(httpRequestHeaderForUserId)?: request.getUserPrincipal()?.principal?.attributes?.id
        if (userId) {
            def userDetails = userService.setCurrentUser(userId)
            if (userDetails) {
                // We set the current user details in the request scope because
                // the 'afterView' hook can be called prior to the actual rendering (despite the name)
                // and the thread local can get clobbered before it is actually required.
                // Consumers who have access to the request can simply extract current user details
                // from there rather than use the service.
                request.setAttribute(UserDetails.REQUEST_USER_DETAILS_KEY, userDetails)
            }
        }

        true
    }

    boolean after() { true }

    void afterView() {
        userService.clearCurrentUser()
    }

    @Override
    void setConfiguration(Config co) {
        httpRequestHeaderForUserId = co.getProperty('app.http.header.userId', String)
    }

}
