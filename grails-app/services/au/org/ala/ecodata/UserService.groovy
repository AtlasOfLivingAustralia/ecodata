package au.org.ala.ecodata

import au.org.ala.userdetails.UserDetailsClient
import au.org.ala.web.AuthService
import au.org.ala.ws.security.client.AlaOidcClient
import grails.core.GrailsApplication
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.util.FindBest
import org.pac4j.jee.context.JEEContextFactory
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UserService {

    static transactional = false
    static String AUTHORIZATION_HEADER_FIELD = "Authorization"
    AuthService authService
    WebService webService
    GrailsApplication grailsApplication
    UserDetailsClient userDetailsClient
    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    AlaOidcClient alaOidcClient

    /** Limit to the maximum number of Users returned by queries */
    static final int MAX_QUERY_RESULT_SIZE = 1000

    private static ThreadLocal<UserDetails> _currentUser = new ThreadLocal<UserDetails>()

    def getCurrentUserDisplayName() {
        String displayName = authService.displayName
        if (!displayName) {
            def currentUser = _currentUser.get()
            displayName = currentUser ? currentUser.displayName : ""
        }

        displayName
    }

    /**
     * Static equivalent of getCurrentUserDetails for use by GORM objects when dependency injection
     * is disabled in grails 3.
     */
    static def currentUser() {
        return _currentUser.get()
    }

    def getCurrentUserDetails() {
        return _currentUser.get()
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

    def userInRole(Object role){
        return authService.userInRole(role)
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
     * Convenience method to record the most recent time a user has logged into a hub.
     * If no User exists, one will be created.  If no login record exists for a hub, one
     * will be added.  If an existing login time exists, the date will be updated.
     */
    User recordUserLogin(String hubId, String userId, Date loginTime = new Date()) {

        if (!hubId || !userId || !Hub.findByHubId(hubId)) {
            throw new IllegalArgumentException()
        }

        User user = User.findByUserIdAndStatusNotEqual(userId, Status.DELETED)
        if (!user) {
            user = new User(userId:userId)
        }
        user.loginToHub(hubId, loginTime)
        user.save()

        user
    }

    /**
     * Returns a list of Users who last logged into the specified hub before the supplied date.
     * Users who have never logged into the hub will not be returned.
     * @param hubId The hubId of the hub of interest
     * @param date The cutoff date for logins
     * @param offset (optional, default 0) offset into query results, used for batching
     * @param max (optional, maximum 1000) maximum number of results to return from the query
     * @return List<User>
     */
    List<User> findUsersNotLoggedInToHubSince(String hubId, Date date, int offset = 0, int max = MAX_QUERY_RESULT_SIZE) {
        Map options = [offset:offset, max: Math.min(max, MAX_QUERY_RESULT_SIZE), sort:'userId']

        User.where {
            userHubs {
                hubId == hubId && lastLoginTime < date
            }
        }.list(options)
    }

    /**
     * Returns a list of Users who last logged into the specified between two specified dates.
     * Users who have never logged into the hub will not be returned.
     * @param hubId The hubId of the hub of interest
     * @param fromDate The start date for finding logins
     * @param toDate The end date for finding logins
     * @param offset (optional, default 0) offset into query results, used for batching
     * @param max (optional, maximum 1000) maximum number of results to return from the query
     * @return List<User> The users who need to be sent a warning.
     */
    List<User> findUsersWhoLastLoggedInToHubBetween(String hubId, Date fromDate, Date toDate, int offset = 0, int max = MAX_QUERY_RESULT_SIZE) {
        Map options = [offset:offset, max: Math.min(max, MAX_QUERY_RESULT_SIZE), sort:'userId']
        User.where {
            userHubs {
                hubId == hubId && lastLoginTime < toDate && lastLoginTime >= fromDate
            }
        }.list(options)
    }

    /**
     * This will return the User entity
     */
    User findByUserId(String userId) {
        User.findByUserId(userId)
    }


    /**
     * Get user from JWT.
     * @param authorizationHeader
     * @return
     */
    au.org.ala.web.UserDetails getUserFromJWT(String authorizationHeader = null) {
        if((config == null) || (alaOidcClient == null))
            return
        try {
            GrailsWebRequest grailsWebRequest = GrailsWebRequest.lookup()
            HttpServletRequest request = grailsWebRequest.getCurrentRequest()
            HttpServletResponse response = grailsWebRequest.getCurrentResponse()
            if (!authorizationHeader)
                authorizationHeader = request?.getHeader(AUTHORIZATION_HEADER_FIELD)
            if (authorizationHeader?.startsWith("Bearer")) {
                final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
                def optCredentials = alaOidcClient.getCredentials(context, config.sessionStore)
                if (optCredentials.isPresent()) {
                    Credentials credentials = optCredentials.get()
                    def optUserProfile = alaOidcClient.getUserProfile(credentials, context, config.sessionStore)
                    if (optUserProfile.isPresent()) {
                        def userProfile = optUserProfile.get()
                        String userId = userProfile?.userId ?: userProfile?.getAttribute(grailsApplication.config.getProperty('userProfile.userIdAttribute'))
                        if (userId) {
                            return authService.getUserForUserId(userId)
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Failed to get user details from JWT", e)
            return null
        }
    }

    def setUser() {
        String userId
        GrailsWebRequest grailsWebRequest = GrailsWebRequest.lookup()
        HttpServletRequest request = grailsWebRequest.getCurrentRequest()
        def userDetails = request.getAttribute(UserDetails.REQUEST_USER_DETAILS_KEY)

        if (userDetails)
            return userDetails

        // userId is set from either the request param userId or failing that it tries to get it from
        // the UserPrincipal (assumes ecodata is being accessed directly via admin page)
        userId = getUserFromJWT() ?: authService.getUserId() ?: request.getHeader(AuditInterceptor.httpRequestHeaderForUserId)

        if (userId) {
            userDetails = setCurrentUser(userId)
            if (userDetails) {
                // We set the current user details in the request scope because
                // the 'afterView' hook can be called prior to the actual rendering (despite the name)
                // and the thread local can get clobbered before it is actually required.
                // Consumers who have access to the request can simply extract current user details
                // from there rather than use the service.
                request.setAttribute(UserDetails.REQUEST_USER_DETAILS_KEY, userDetails)
                return userDetails
            }
        }
    }
}
