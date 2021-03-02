package au.org.ala.ecodata

import org.grails.gorm.graphql.plugin.testing.GraphQLSpec
import spock.lang.Specification

class GraphqlSpecHelper extends Specification implements GraphQLSpec{

    def graphqlRequest(String requestBody, String userName){
        return rest.post("http://localhost:${serverPort}/graphql/index") {
            contentType("application/graphql")
            header("Cookie", "ALA-Auth=" + userName);
            body(requestBody)
        }
    }
}