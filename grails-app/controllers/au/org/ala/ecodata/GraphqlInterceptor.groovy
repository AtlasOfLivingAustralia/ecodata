package au.org.ala.ecodata

import grails.converters.JSON

class GraphqlInterceptor {

    UserService userService
    PermissionService permissionService

    GraphqlInterceptor() {
        match uri: '/graphql/index/**'
    }

    boolean before() {
        //validate the username and given authKey
        String userName = request.getHeader('userName')
        String authKey = request.getHeader('authKey')

        if(authKey && userName) {

            def result = userService.validateKey(userName, authKey)
            if (!result?.resp?.statusCode && result.resp?.status == 'success') {
                //and also, only the ala admins are permitted to do graphql searches
                if (permissionService.isUserAlaAdmin(userName)) {
                    true
                }
                else{
                    Map map = [error: 'Access denied', status: 401]
                    response.status = 401
                    render map as JSON

                    false
                }
            } else {
                Map map = [error: 'Access denied', status: 401]
                response.status = 401
                render map as JSON

                false
            }
        }
        else{
            Map map = [error: 'userName or authKey is invalid', status: 400]
            response.status = 400
            render map as JSON

            false
        }
    }

    boolean after = { }

    void afterView() { }

}
