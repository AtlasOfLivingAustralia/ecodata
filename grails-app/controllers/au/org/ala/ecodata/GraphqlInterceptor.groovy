package au.org.ala.ecodata

import au.org.ala.ws.security.AuthenticatedUser
import grails.converters.JSON

class GraphqlInterceptor {

    UserService userService
    PermissionService permissionService

    GraphqlInterceptor() {
        match uri: '/graphql/**'
    }

    boolean before() {
        //JWT
        String userid = request.getUserPrincipal()?.principal?.attributes?.userid

        if(!userid){
            accessDeniedError('No userId')
            return false
        }
        else{
            return  true
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
