package au.org.ala.ecodata

import au.org.ala.web.AuthService
import grails.core.support.GrailsConfigurationAware
import grails.config.Config

class AuditInterceptor implements GrailsConfigurationAware {

    int order = 100 // This needs to be after the @RequireApiKey interceptor which makes the userId available via the authService
    static String httpRequestHeaderForUserId
    UserService userService
    AuthService authService

    public AuditInterceptor() {
        matchAll()
    }

    boolean before() {
        userService.setUser()
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
