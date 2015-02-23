package au.org.ala.ecodata

class UserService {

    static transactional = false
    def authService, grailsApplication, webService

    private static ThreadLocal<UserDetails> _currentUser = new ThreadLocal<UserDetails>()

    def getCurrentUserDisplayName() {
        UserDetails currentUser = _currentUser.get()
        return currentUser ? currentUser.displayName : ""
    }

    def getCurrentUserDetails() {
        return _currentUser.get();
    }

    def lookupUserDetails(String userId) {
        // DONE: lookup user details from AUTH
        def userDetails = new UserDetails(userId: '9999', userName: 'mark.woolston@csiro.au', displayName: 'Mark Woolston')
        getUserForUserId(userId)?:userDetails
    }

    synchronized def getUserForUserId(String userId) {
        if (!userId) {
            return null
        }
        return authService.getUserForUserId(userId)
    }

    /**
     * This method gets called by a filter at the beginning of the request (if a userId parameter is on the URL)
     * It sets the user details in a thread local for extraction by the audit service.
     * @param userId
     */
    def setCurrentUser(String userId) {

        def userDetails = lookupUserDetails(userId)
        if (userDetails) {
            _currentUser.set(userDetails)
            return userDetails
        } else {
            log.warn("Failed to lookup user details for user id ${userId}! No details set on thread local.")
        }
    }

    def clearCurrentUser() {
        if (_currentUser) {
            _currentUser.remove()
        }
    }

    /**
     * Returns null if lookup fails
     * @param email
     * @return
     */
    def syncUserIdLookup(email){
        def result = webService.doPost(grailsApplication.config.userDetailsSingleUrl, ["userName": email])
        if (!result.error){
            result.resp.userId
        } else {
            null
        }
    }
}
