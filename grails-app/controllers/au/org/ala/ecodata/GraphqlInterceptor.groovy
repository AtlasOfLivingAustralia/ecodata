

package au.org.ala.ecodata

import grails.converters.JSON
import org.pac4j.core.config.Config
import org.pac4j.core.context.JEEContextFactory
import org.pac4j.core.context.WebContext
import org.pac4j.core.util.FindBest
import org.pac4j.http.client.direct.DirectBearerAuthClient
import org.springframework.beans.factory.annotation.Autowired

class GraphqlInterceptor {

    UserService userService
    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    DirectBearerAuthClient directBearerAuthClient

    GraphqlInterceptor() {
        match uri: '/graphql/**'
    }

    boolean before() {
        // allow ALA admins to access GraphQL browser
        if (request.isUserInRole("ROLE_ADMIN")) {
            return true
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            if (authorizationHeader.startsWith("Bearer")) {
                final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
                def credentials = directBearerAuthClient.getCredentials(context, config.sessionStore)
                if (credentials.isPresent()) {
                    def profile = directBearerAuthClient.getUserProfile(credentials.get(), context, config.sessionStore)
                    if (profile.isPresent()) {
                        def userProfile = profile.get()
                        def result =  userProfile.roles.contains("ROLE_ADMIN") || userProfile.roles.contains("ROLE_FC_ADMIN")

                        if(result){
                            return true
                        }
                        else{
                            accessDeniedError('No required user roles')
                            return false
                        }
                    }
                    else {
                        accessDeniedError('Invalid token')
                        return false
                    }
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
