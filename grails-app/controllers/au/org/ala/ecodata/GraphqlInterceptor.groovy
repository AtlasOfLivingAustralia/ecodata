

package au.org.ala.ecodata

import grails.converters.JSON
import org.apache.http.HttpStatus

/** Secures the GraphQL endpoint and browser behind a role that grants permission to use the API */
class GraphqlInterceptor {

    GraphqlInterceptor() {
        match uri: '/ws/graphql/**' // Web services - uses the supplied JWT bearer token to authorize
        match uri: '/graphql/**'  // Admin UI - uses the jee session state to authorize
    }

    boolean before() {
        boolean allowed = request.isUserInRole("ROLE_ECODATA_API")
        if (!allowed) {
            accessDeniedError("You do not have permissions to use this API")
        }
        allowed
    }

    boolean after = { }

    void afterView() { }

    def accessDeniedError(String error) {
        Map map = [error: error, status: HttpStatus.SC_UNAUTHORIZED]
        response.status = HttpStatus.SC_UNAUTHORIZED
        log.warn (error)
        render map as JSON
    }

}
