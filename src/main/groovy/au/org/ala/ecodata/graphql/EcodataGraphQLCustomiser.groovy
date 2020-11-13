package au.org.ala.ecodata.graphql

import graphql.schema.Coercing
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import org.bson.types.ObjectId
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.gorm.graphql.fetcher.GraphQLDataFetcherType
import org.grails.gorm.graphql.fetcher.manager.GraphQLDataFetcherManager
import org.grails.gorm.graphql.interceptor.GraphQLFetcherInterceptor
import org.grails.gorm.graphql.interceptor.GraphQLSchemaInterceptor
import org.grails.gorm.graphql.interceptor.manager.GraphQLInterceptorManager
import org.grails.gorm.graphql.plugin.GraphQLPostProcessor
import org.grails.gorm.graphql.types.GraphQLTypeManager

class EcodataGraphQLCustomiser extends GraphQLPostProcessor {

    @Override
    void doWith(GraphQLTypeManager graphQLTypeManager) {
        // GraphQL type conversion for Mongo ObjectIds
        graphQLTypeManager.registerType(ObjectId, new GraphQLScalarType("ObjectId", "Hex representation of a Mongo object id", new ObjectIdConverter()))
        graphQLTypeManager.registerType(Object, new GraphQLScalarType("object", "", new ObjectConverter()))
        //graphQLTypeManager.registerType(Map, GraphQLInterfaceType.newInterface().)
    }

    @Override
    void doWith(GraphQLInterceptorManager graphQLInterceptorManager) {
        Set entities = new HashSet()
        graphQLInterceptorManager.registerInterceptor(new GraphQLSchemaInterceptor() {
            @Override
            void interceptEntity(PersistentEntity entity, List<GraphQLFieldDefinition.Builder> queryFields, List<GraphQLFieldDefinition.Builder> mutationFields) {
                if (entities.contains(entity.name)) {
                    queryFields.clear()
                    mutationFields.clear()
                }
                else {
                    entities.add(entity.name)
                }
            }

            @Override
            void interceptSchema(GraphQLObjectType.Builder queryType, GraphQLObjectType.Builder mutationType, Set<GraphQLType> additionalTypes) {
                println queryType


            }
        })

        graphQLInterceptorManager.registerInterceptor(Object, new GraphQLFetcherInterceptor() {
            @Override
            boolean onQuery(DataFetchingEnvironment environment, GraphQLDataFetcherType type) {
                println "Running query $type"

                return true
            }

            @Override
            boolean onMutation(DataFetchingEnvironment environment, GraphQLDataFetcherType type) {
                return false
            }

            @Override
            boolean onCustomQuery(String name, DataFetchingEnvironment environment) {
                println name
                return true
            }

            @Override
            boolean onCustomMutation(String name, DataFetchingEnvironment environment) {
                return false
            }
        })
    }


}
