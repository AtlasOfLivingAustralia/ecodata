package au.org.ala.ecodata

import grails.converters.JSON

class UserController {
    UserService userService

    /**
     * Get user auth key for the given username and password
     */
    @PreAuthorise(basicAuth = false)
    def getKey() {
        String username = params.username?.encodeAsURL()
        String password = params.password?.encodeAsURL()
        def result = userService.getUserKey(username, password)
        if (result?.statusCode) {
            response.setStatus(result.statusCode)
        }

        render result as JSON
    }
}
