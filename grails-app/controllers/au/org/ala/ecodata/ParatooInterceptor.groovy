package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooInvocationContext
import au.org.ala.web.AuthService

class ParatooInterceptor {

    AuthService authService

    int order = 110 // Runs after the AuditInterceptor which sets the user in the UserService
    ParatooInterceptor() {
        match(controller:'paratoo')
    }

    boolean before() {
        String apiVersion = params.apiVersion ?: "v1"
        Permission operationType = null
        if (params.operationType) {
            operationType = Permission.fromString(params.operationType)
        }
        else {
            // Default to read for GET requests and write for all others
            operationType = request.method == "GET" ? Permission.READ : Permission.WRITE
        }

        ParatooInvocationContext.setCurrent(new ParatooInvocationContext(userId: authService.userId, operationType: operationType, apiVersion: apiVersion))
        true
    }

    void afterView() {
        ParatooInvocationContext.removeCurrent()
    }


}
