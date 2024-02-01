package au.org.ala.ecodata

@au.ala.org.ws.security.RequireApiKey(scopes=["profile", "email", "openid"])
/**
 * This class exists to allow the RequireApiKey annotation to be applied to the path around the GraphQL endpoint
 * so we can decode the JWT before forwarding the request on.
 */
class GraphqlWsController {
    def index() {
        forward(uri:'/graphql/index')
    }
}
