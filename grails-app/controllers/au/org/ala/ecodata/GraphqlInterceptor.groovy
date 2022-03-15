package au.org.ala.ecodata

import org.pac4j.core.config.Config
import org.pac4j.core.context.JEEContextFactory
import grails.converters.JSON
import au.org.ala.web.UserDetails
import org.pac4j.core.context.WebContext
import org.pac4j.http.client.direct.DirectBearerAuthClient
import org.springframework.beans.factory.annotation.Autowired
import org.pac4j.core.util.FindBest

import javax.inject.Inject

class GraphqlInterceptor {

    UserService userService
    PermissionService permissionService
    @Autowired
    Config config
    @Autowired
    DirectBearerAuthClient directBearerAuthClient

    GraphqlInterceptor() {
        match uri: '/graphql/**'
    }

    boolean before() {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            if (authorizationHeader.startsWith("Bearer")) {
                final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
                def credentials = directBearerAuthClient.getCredentials(context, config.sessionStore)
                if (credentials.isPresent()) {
                    return true
                }
                else {
                    accessDeniedError('Invalid token')
                    return false
                }
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
        Map map = [error: error, status: 401]
        response.status = 401
        log.warn (error)
        render map as JSON
    }

}
