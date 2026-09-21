package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooInvocationContext
import au.org.ala.web.AuthService
import grails.core.GrailsApplication
import org.apache.http.HttpStatus

class ParatooInterceptor {

    AuthService authService
    GrailsApplication grailsApplication

    int order = 110 // Runs after the AuditInterceptor which sets the user in the UserService
    ParatooInterceptor() {
        match(controller:'paratoo')
    }

    boolean before() {
        String apiVersion = params.apiVersion ?: "v1"
        Permission operationType = null
        if (params.operationType) {
            operationType = Permission.fromString(params.operationType)
            if (!operationType) {
                log.warn "Invalid operationType ${params.operationType} specified in request"
                response.status = HttpStatus.SC_BAD_REQUEST
                return false
            }
        }
        else {
            // Default to read for GET requests and write for all others
            operationType = request.method == "GET" ? Permission.READ : Permission.WRITE
        }

        // The Monitor/Paratoo application has a use case where it needs to call the API on behalf of a user,
        // but the user is not actually logged in.  This is implemented by a JWT with a scope allowing
        // access to write to the paratoo API without a direct user context.
        String scope = grailsApplication.config.getProperty('paratoo.api.writeScope')
        boolean isSystemUser = request.isUserInRole(scope)
        ParatooInvocationContext.setCurrent(new ParatooInvocationContext(userId: authService.userId, isSystemUser: isSystemUser, operationType: operationType, apiVersion: apiVersion))
        true
    }

    void afterView() {
        ParatooInvocationContext.removeCurrent()
    }


}
