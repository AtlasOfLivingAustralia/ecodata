package au.org.ala.ecodata

import au.org.ala.ecodata.command.HubLoginTime
@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class UserController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [recordUserLogin: 'POST']

    UserService userService
    WebService webService

    /**
     * Records the time a User has logged into a hub.
     * {@link au.org.ala.ecodata.UserService#recordUserLogin(java.lang.String, java.lang.String)} for details.
     *
     * @param hubId The hubId of the Hub that was logged into
     * @param userId The userId of the user that logged in.  If not supplied the current user will be used.
     * @param loginTime The time the user logged in.  If not supplied, the current time will be used by the service.
     */
    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def recordUserLogin(HubLoginTime hubLoginTime) {
        if (hubLoginTime.hasErrors()) {
            respond hubLoginTime.errors
        }
        else {
            String userId = hubLoginTime.userId ?: userService.getCurrentUserDetails()?.userId
            respond userService.recordUserLogin(hubLoginTime.hubId, userId, hubLoginTime.loginTime)
        }

    }

}
