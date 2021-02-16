package au.org.ala.ecodata

import au.org.ala.web.AuthService
import grails.converters.JSON
import au.org.ala.web.UserDetails

class GraphqlInterceptor {

    UserService userService
    PermissionService permissionService
    AuthService authService

    def LOCALHOST_IP = '127.0.0.1'

    GraphqlInterceptor() {
        match uri: '/graphql/index/**'
    }

    boolean before() {
        //validate the username and given authKey
        String userName = request.getHeader(grailsApplication.config.app.http.header.userId)
        String authKey = request.getHeader('authKey')

        if(authKey && userName) {

            def result = userService.validateKey(userName, authKey)
            if (!result?.resp?.statusCode && result.resp?.status == 'success') {
                //and also, only the ala admins are permitted to do graphql searches
                if (permissionService.isUserAlaAdmin(userName)) {
                    return true
                }
                else{
                    accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + userName)
                    return false
                }
            } else {
                accessDeniedError('Invalid GrapqhQl API usage: Invalid username or api key, userId: ' + userName)
                return false
            }
        }
        // This is to validate graphql browser user
        else{
            def userCookie = null

            if(request.cookies) {
                userCookie = request.cookies.find { it.name == 'ALA-Auth' }
            }
            UserDetails user = null

            if (userCookie) {
                String username = URLDecoder.decode(userCookie.getValue(), 'utf-8')
                //test to see that the user is valid
                user = authService.getUserForEmailAddress(username)

                if(!user){
                    accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + username)
                    return false
                }
                else{
                    def whiteList = buildWhiteList()
                    def clientIp = request.getHeader("X-Forwarded-For") ?: request.getRemoteHost()
                    def ipOk = whiteList.contains(clientIp) || (whiteList.size() == 1 && whiteList[0] == LOCALHOST_IP)

                    if(ipOk) {
                        userService.setCurrentUser(username)
                        return true
                    }
                    else {
                        accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + username)
                        return false
                    }
                }
            }
            else{
                accessDeniedError('Invalid GrapqhQl API usage: Access denied')
                return false
            }

            false
        }
    }

    boolean after = { }

    void afterView() { }

    def buildWhiteList() {
        def whiteList = [LOCALHOST_IP]
        def config = grailsApplication.config.app.api.whiteList as String
        if (config) {
            whiteList.addAll(config.split(',').collect({it.trim()}))
        }
        whiteList
    }

    def accessDeniedError(String error) {
        Map map = [error: 'Access denied', status: 401]
        response.status = 401
        log.warn (error)
        render map as JSON
    }

}
