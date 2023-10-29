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
import org.grails.gorm.graphql.fetcher.impl.ClosureDataFetchingEnvironment

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
                dataFetcher { Output output, ClosureDataFetchingEnvironment env ->
                    List fieldList = []
                    if(output.tempArgs) {
                        output.tempArgs["output"].each {
                            it.each { x ->
                                x.each { y ->
                                    if (y["outputType"] == output.name && y["fields"]) {
                                        fieldList = y["fields"] as List
                                    }
                                }
                            }
                        }
                    }
                    //get output data with requested fields
                    output.getData(fieldList)
                }
                input false
            }

            query('outputs', [Output]) {
                argument('output', 'output') {
                    accepts {
                        field('outputType', String)
                        field('fields', [String]) { nullable true }
                        collection true
                    }
                    nullable true
                }
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        new OutputFetcher(Holders.applicationContext.metadataService, Holders.applicationContext.messageSource, Holders.grailsApplication).getFilteredOutput(environment.arguments['output'] as List)
                    }
                })
            }

            //get the list of output types
            query('outputList', [Summary]) {
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
