package au.org.ala.ecodata.graphql

import au.org.ala.ecodata.UserService
import au.org.ala.ecodata.graphql.converters.DateFormatting
import au.org.ala.ecodata.graphql.converters.MeriPlanConverter
import au.org.ala.ecodata.graphql.converters.ObjectConverter
import au.org.ala.ecodata.graphql.converters.ObjectIdConverter
import au.org.ala.ecodata.graphql.converters.OutputDataConverter
import au.org.ala.ecodata.graphql.converters.SchemaConverter
import au.org.ala.ecodata.graphql.converters.SectionTemplateConverter
import au.org.ala.ecodata.graphql.converters.SummaryConverter
import au.org.ala.ecodata.graphql.models.MeriPlan
import au.org.ala.ecodata.graphql.models.OutputData
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.SectionTemplate
import au.org.ala.ecodata.graphql.models.Summary
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import org.apache.log4j.Logger
import org.bson.types.ObjectId
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.gorm.graphql.fetcher.GraphQLDataFetcherType
import org.grails.gorm.graphql.interceptor.GraphQLFetcherInterceptor
import org.grails.gorm.graphql.interceptor.GraphQLSchemaInterceptor
import org.grails.gorm.graphql.interceptor.manager.GraphQLInterceptorManager
import org.grails.gorm.graphql.plugin.GraphQLPostProcessor
import org.grails.gorm.graphql.types.GraphQLTypeManager

class EcodataGraphQLCustomiser extends GraphQLPostProcessor {

    static Logger log = Logger.getLogger(EcodataGraphQLCustomiser.class)

    @Override
    void doWith(GraphQLTypeManager graphQLTypeManager) {
        // GraphQL type conversion for Mongo ObjectIds
        graphQLTypeManager.registerType(ObjectId, new GraphQLScalarType("ObjectId", "Hex representation of a Mongo object id", new ObjectIdConverter()))
        graphQLTypeManager.registerType(Object, new GraphQLScalarType("object", "", new ObjectConverter()))
        //graphQLTypeManager.registerType(Map, GraphQLInterfaceType.newInterface().)
        graphQLTypeManager.registerType(MeriPlan, new GraphQLScalarType("MeriPlan", "", new MeriPlanConverter()))
        graphQLTypeManager.registerType(OutputData, new GraphQLScalarType("OutputData", "", new OutputDataConverter()))
        graphQLTypeManager.registerType(SectionTemplate, new GraphQLScalarType("SectionTemplate", "", new SectionTemplateConverter()))
        graphQLTypeManager.registerType(Summary, new GraphQLScalarType("Summary", "", new SummaryConverter()))
        graphQLTypeManager.registerType(Schema, new GraphQLScalarType("Schema", "", new SchemaConverter()))
        graphQLTypeManager.registerType(Date, new GraphQLScalarType("Date", "", new DateFormatting()))
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
                Map query = [:]
                query.name = environment.selectionSet.fields.name
                query.arguments = environment.selectionSet.fields.arguments
                query.selections = environment.selectionSet.fields.selectionSet.selections
                log.info ('GrapqhQl API request, UserId: ' + UserService.currentUser().userName + ", Query: " + query)
                return true
            }

            @Override
            boolean onCustomMutation(String name, DataFetchingEnvironment environment) {
                return false
            }
        })
    }


}
