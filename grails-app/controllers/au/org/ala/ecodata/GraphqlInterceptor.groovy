package au.org.ala.ecodata

import grails.converters.JSON
import au.org.ala.web.UserDetails

import javax.inject.Inject

class GraphqlInterceptor {

    UserService userService
    PermissionService permissionService

    GraphqlInterceptor() {
        match uri: '/graphql/**'
    }

    boolean before() {
        String userName = request.getUserPrincipal()?.principal?.attributes?.id

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            if (authorizationHeader.startsWith("Bearer")) {
                return true
            }
            else {
                accessDeniedError('No Authorization Bearer token')
                return false
            }
        }
        else {
            accessDeniedError('No Authorization header')
            return false
        }

//        if (userName) {
//            //test to see that the user is valid
//            UserDetails user = userService.getUserForUserId(userName)
//
//            if(!user){
//                accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + userName)
//                return false
//            }
//            else{
//                //TODO add Biocollect hub owners roles
//                if(permissionService.isUserAlaAdmin(userName) || userService.getRolesForUser(userName)?.contains("ROLE_FC_ADMIN")) {
//                    return true
//                }
//                else {
//                    accessDeniedError('Invalid GrapqhQl API usage: Access denied, userId: ' + userName)
//                    return false
//                }
//            }
//        }
//        else{
//            accessDeniedError('Invalid GrapqhQl API usage: No user Id')
//            return false
//        }
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
