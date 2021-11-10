package au.org.ala.ecodata

import au.org.ala.ecodata.command.HubLoginTime
import grails.converters.JSON
import org.springframework.validation.Errors

class UserController {

    static allowedMethods = [recordLoginTime: 'POST']

    UserService userService
    WebService webService

    /**
     * Get user auth key for the given username and password
     */
    @PreAuthorise(basicAuth = false)
    def getKey() {
        Map result = [:]
        String username = request.getHeader('userName')
        String password = request.getHeader('password')

        if (username && password) {
            def ret = userService.getUserKey(username, password)
            if (ret.error) {
                String error = "Failed to get key for user: "+username
                log.error(error)
                result = [status: 'error', statusCode: ret.statusCode, error: error]

            } else if (ret.resp) {
                result = ret.resp
                String userDetailsUrl = grailsApplication.config.userDetails.url + "getUserDetails"
                def userDetailsResult = webService.doPostWithParams(userDetailsUrl, [userName:username])
                if (!userDetailsResult?.resp?.statusCode && userDetailsResult.resp) {
                    result.userId = userDetailsResult.resp.userId
                    result.firstName = userDetailsResult.resp.firstName
                    result.lastName = userDetailsResult.resp.lastName
                }
            }
        } else {
            result = [status: 'error', statusCode: 400, error: "Missing username or password"]
        }

        result.statusCode ? response.setStatus(result.statusCode) : ''
        render result as JSON
    }

    /**
     * Records the time a User has logged into a hub.
     * {@link au.org.ala.ecodata.UserService#recordLoginTime(java.lang.String, java.lang.String)} for details.
     *
     * @param hubId The hubId of the Hub that was logged into
     * @param userId The userId of the user that logged in.  If not supplied the current user will be used.
     * @param loginTime The time the user logged in.  If not supplied, the current time will be used by the service.
     */
    def recordLoginTime(HubLoginTime hubLoginTime) {
        if (hubLoginTime.hasErrors()) {
            respond hubLoginTime.errors
        }
        else {
            String userId = hubLoginTime.userId ?: userService.getCurrentUserDetails()?.userId
            respond userService.recordLoginTime(hubLoginTime.hubId, userId, hubLoginTime.loginTime)
        }

    }

}
