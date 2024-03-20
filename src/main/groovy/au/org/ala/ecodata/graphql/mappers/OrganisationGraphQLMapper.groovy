package au.org.ala.ecodata.graphql.mappers


import au.org.ala.ecodata.Program
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class OrganisationGraphQLMapper {
    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled false
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

        }
    }
}
