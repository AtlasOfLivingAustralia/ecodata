package au.org.ala.ecodata

class UserService {

    private static ThreadLocal<UserDetails> _currentUser = new ThreadLocal<UserDetails>()

    def getCurrentUserDisplayName() {
        UserDetails currentUser = _currentUser.get()
        return currentUser ? currentUser.displayName : '<anonymous>'
    }

    def UserDetails lookupUserDetails(String userId) {
        // TODO: lookup user details from AUTH
        def userDetails = new UserDetails(userId: userId, userName: 'mark.woolston@csiro.au', displayName: 'Mark Woolston')
        return userDetails
    }

    /**
     * This method gets called by a filter at the beginning of the request (if a userId paramter is on the URL)
     * It sets the user details in a thread local for extraction by the audit service.
     * @param userId
     */
    def setCurrentUser(String userId) {

        def userDetails = lookupUserDetails(userId)
        if (userDetails) {
            log.debug("Setting user details for id ${userId} on thread local.")
            _currentUser.set(userDetails)
            return userDetails
        } else {
            log.warn("Failed to lookup user details for user id ${userId}! No details set on thread local.")
        }
    }

    def clearCurrentUser() {
        if (_currentUser) {
            log.debug("Clearing user details from thread local.")
            _currentUser.remove()
        }
    }

}
