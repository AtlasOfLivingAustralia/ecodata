package au.org.ala.ecodata

import au.org.ala.web.AuthService

class UserService {

    static transactional = false
    AuthService authService
    WebService webService
    def grailsApplication

    private static ThreadLocal<UserDetails> _currentUser = new ThreadLocal<UserDetails>()

    def getCurrentUserDisplayName() {
        def currentUser = _currentUser.get()
        return currentUser ? currentUser.displayName : ""
    }

    /**
     * Static equivalent of getCurrentUserDetails for use by GORM objects when dependency injection
     * is disabled in grails 3.
     */
    static def currentUser() {
        return _currentUser.get()
    }

    def getCurrentUserDetails() {
        return _currentUser.get();
    }

    def lookupUserDetails(String userId) {

        def userDetails = getUserForUserId(userId)
        if (!userDetails) {
            if (log.debugEnabled) {
                log.debug("Unable to lookup user details for userId: ${userId}")
            }
            userDetails = new UserDetails(userId: userId, userName: 'unknown', displayName: 'Unknown')
        }

        userDetails
    }

    /**
     * Gets the CAS roles for the specified user. If no id is provided, then the currently authenticated user will be used
     *
     * @param userId The ID of the user whose roles you want to retrieve. Optional - if not provided, will return the roles for the currently authenticated user (if there is one)
     * @return List of {@link au.org.ala.web.CASRoles} names
     */
    def getRolesForUser(String userId = null) {
        userId = userId ?: getCurrentUserDetails().userId
        authService.getUserForUserId(userId, true)?.roles ?: []
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
     * Check username against the auth key.
     *
     * @param username
     * @param authKey
     */
    String authorize(userName, authKey) {
        String userId = ""

        if (authKey && userName) {
            String key = new String(authKey)
            String username = new String(userName)

            def url = grailsApplication.config.authCheckKeyUrl
            def params = [userName: username, authKey: key]
            def result = webService.doPostWithParams(url, params, true)
            if (!result?.resp?.statusCode && result.resp?.status == 'success') {
                params = [userName: username]
                url = grailsApplication.config.userDetails.url + "getUserDetails"
                result = webService.doPostWithParams(url, params)
                if (!result?.resp?.statusCode && result.resp) {
                    userId = result.resp.userId
                }
            }
        }

        return userId
    }

    /**
     * Get auth key for the given username and password
     *
     * @param username
     * @param password
     */
    def getUserKey(String username, String password) {
        webService.doPostWithParams(grailsApplication.config.authGetKeyUrl, [userName: username, password: password], true)
    }

    def getAllUsers() {
        return authService.getAllUserNameList()
    }

}
