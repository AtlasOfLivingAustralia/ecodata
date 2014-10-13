package au.org.ala.ecodata

class UserService {

    static transactional = false
    def authService, cacheService

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

        def um = cacheService.get('userNameMap' , {
            def userMap = authService.getAllUserNameMap()
            userMap.values().collectEntries { [(it.userId): it] }

        })

        if (!um || um.containsKey('error')) {
            cacheService.clear('userNameMap')
        }

        def ud = null;

        if (um && um.containsKey(userId)) {
            ud = um.get(userId)
        }

        return ud
    }

    /**
     * This method gets called by a filter at the beginning of the request (if a userId paramter is on the URL)
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

}
