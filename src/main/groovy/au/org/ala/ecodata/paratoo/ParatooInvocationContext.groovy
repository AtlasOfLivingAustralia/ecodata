package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.ParatooService
import au.org.ala.ecodata.Permission

class ParatooInvocationContext {

    static ThreadLocal<ParatooInvocationContext> current = new ThreadLocal<>()

    String userId
    String apiVersion
    /**
     * The operationType in the context is not necessarily the type of operation being called.
     * For example, a GET request to /user-project is a read operation, however it returns projects based
     * on the requested operationType.  e.g. the Monitor client is asking "give me all the projects this
     * user has write access to" and the ParatooService will filter the projects based on the operationType in the context.
     */
    Permission operationType = Permission.WRITE


    void filterRoles(List roles) {
        // The Determiner role was only introduced in v2 of the API, so if we're running in v1 mode we need to remove it from the list of roles
        if (apiVersion != "v2") {
            roles.remove(ParatooService.DETERMINER)
        }
    }

    boolean supportsMultipleRoles() {
        return apiVersion && apiVersaion != "v1"
    }

}
