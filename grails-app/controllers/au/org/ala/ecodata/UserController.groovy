package au.org.ala.ecodata

import grails.converters.JSON

class UserController {
    UserService userService

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
                result = [status: 'error', statusCode: ret.statusCode, error: ret.error]
            } else if (ret.resp) {
                result = ret.resp
            }
        } else {
            result = [status: 'error', statusCode: 400, error: "Missing username or password"]
        }

        result.statusCode ? response.setStatus(result.statusCode) : ''
        render result as JSON
    }
}
