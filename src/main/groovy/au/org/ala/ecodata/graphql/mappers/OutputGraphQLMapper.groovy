package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.fetchers.OutputFetcher
import au.org.ala.ecodata.graphql.models.OutputData
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.Summary
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class OutputGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled false
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false


            add('data', OutputData) {
                dataFetcher { Output output ->
                    output.getData(output.data)
                }
                input false
            }

            //get the list of output types
            query('outputs', [Summary]) {
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        new OutputFetcher(Holders.applicationContext.metadataService, Holders.applicationContext.messageSource, Holders.grailsApplication).getOutputSummaryList(environment)
                    }
                })
            }

            //get output schema by output type name
            query('outputTypeByName', Schema) {
                argument('outputTypeName', String)
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        new OutputFetcher(Holders.applicationContext.metadataService, Holders.applicationContext.messageSource, Holders.grailsApplication).getOutputByName(environment.getArgument('outputTypeName'))
                    }
                })
            }
        }
    }
}
