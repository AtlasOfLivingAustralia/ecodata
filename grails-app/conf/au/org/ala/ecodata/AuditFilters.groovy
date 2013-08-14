package au.org.ala.ecodata

class AuditFilters {

    def userService

    def filters = {

        all(controller: '*', action: '*') {

            before = {
                def userDetails = userService.setCurrentUser(params.userId)
                if (userDetails) {
                    // We set the current user details in the request scope because
                    // the 'afterView' hook can be called prior to the actual rendering (despite the name)
                    // and the thread local can get clobbered before it is actually required.
                    // Consumers who have access to the request can simply extract current user details
                    // from there rather than use the service.
                    request.setAttribute(UserDetails.REQUEST_USER_DETAILS_KEY, userDetails)
                }
            }
            after = { Map model ->

            }
            afterView = { Exception e ->
                userService.clearCurrentUser()
            }
        }
    }
}
