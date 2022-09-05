package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.ManagementUnit
import au.org.ala.ecodata.graphql.fetchers.ManamgementUnitFetcher
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class ManagementUnitGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled false
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

            exclude("outcomes", "priorities", "config")

            add("outcomes", "outcomes") {
                type {
                    field("outcome", String)
                    field("priorities", "priorities"){
                        field("category", String)
                        collection(true)
                    }
                    field("category", String)
                    field("shortDescription", String)
                    collection true
                }
                dataFetcher { ManagementUnit mu ->
                    mu.outcomes
                }
            }

            add("reportConfig", "reportConfig") {
                type {
                    field("report", String)
                    field("category", String)
                    field("reportType", String)
                    field("activityType", String)
                    field("reportNameFormat", String)
                    field("reportDescriptionFormat", String)
                    field("firstReportingPeriodEnd", String)
                    field("reportingPeriodInMonths", String)
                    field("multiple", boolean)
                    field("minimumPeriodInMonths", String)
                    field("reportsAlignedToCalendar", boolean)
                    collection true
                }
                dataFetcher { ManagementUnit mu ->
                    mu.getReportConfig()
                }
            }

            add("data", [Activity]) {
                dataFetcher { ManagementUnit mu ->
                    mu.getActivityData(mu.managementUnitId)
                }
            }

            add("muPriorities", "muPriorities") {
                type {
                    field("category", String)
                    field("priority", String)
                    collection(true)
                }
                dataFetcher { ManagementUnit mu ->
                    mu.priorities
                }
            }

            query('searchManagementUnits', [ManagementUnit]) {
                argument('managementUnitId', String) { nullable true }
                argument('name', String) { nullable true }
                argument('startDate', String){ nullable true description "yyyy-mm-dd"  }
                argument('endDate', String){ nullable true description "yyyy-mm-dd" }
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        new ManamgementUnitFetcher().get(environment)
                    }
                })
            }
        }
    }
}
