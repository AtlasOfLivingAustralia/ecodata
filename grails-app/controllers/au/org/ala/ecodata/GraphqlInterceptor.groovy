package au.org.ala.ecodata

import au.org.ala.web.AuthService
import au.org.ala.web.CASRoles
import grails.converters.JSON
import au.org.ala.web.UserDetails

class GraphqlInterceptor {

    UserService userService
    PermissionService permissionService
    AuthService authService

    GraphqlInterceptor() {
        match uri: '/graphql/**'
    }

    boolean before() {
        String userName = request.getHeader(grailsApplication.config.app.http.header.userId) ?:
                request.cookies.find { it.name == 'ALA-Auth' }.value

        if (userName) {
            //test to see that the user is valid
            UserDetails user = authService.getUserForEmailAddress(userName)

            if(!user){
                accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + userName)
                return false
            }
            else{
                //TODO add Biocollect hub owners roles
                if(permissionService.isUserAlaAdmin(userName) || userService.getRolesForUser(userName)?.contains("ROLE_FC_ADMIN")) {
                    return true
                }
                else {
                    accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + userName)
                    return false
                }
            }
        }
        else{
            accessDeniedError('Invalid GrapqhQl API usage: No user Id')
            return false
        }
}

    boolean after = { }

    void afterView() { }

    def accessDeniedError(String error) {
        Map map = [error: 'Access denied', status: 401]
        response.status = 401
        log.warn (error)
        render map as JSON
    }

}
