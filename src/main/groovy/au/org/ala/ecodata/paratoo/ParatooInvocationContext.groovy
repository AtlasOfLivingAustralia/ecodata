package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.ParatooService
import au.org.ala.ecodata.Permission

class ParatooInvocationContext {

    static ThreadLocal<ParatooInvocationContext> current = new ThreadLocal<>()
    static ParatooInvocationContext getCurrent() {
        return current.get()
    }
    static void setCurrent(ParatooInvocationContext context) {
        current.set(context)
    }
    static void removeCurrent() {
        current.remove()
    }

    static final String API_VERSION_1 = "v1"
    String userId
    String apiVersion
    /**
     * The operationType in the context is not necessarily the type of operation being called.
     * For example, a GET request to /user-project is a read operation, however it returns projects based
     * on the requested operationType.  e.g. the Monitor client is asking "give me all the projects this
     * user has write access to" and the ParatooService will filter the projects based on the operationType in the context.
     */
    Permission operationType = Permission.WRITE

    private boolean isApiVersion1() {
        return !apiVersion || apiVersion == API_VERSION_1
    }

    void filterRoles(List roles) {
        // The Determiner role was only introduced in v2 of the API, so if we're running in v1 mode we need to remove it from the list of roles
        if (isApiVersion1()) {
            roles.remove(ParatooService.DETERMINER)
        }
    }

    boolean supportsMultipleRoles() {
        return !isApiVersion1()
    }

    boolean supportsClientMeta() {
        return !isApiVersion1() && operationType == Permission.WRITE
    }

    boolean requiresSurveyDetailsInSubmission() {
        return !isApiVersion1()
    }

}
