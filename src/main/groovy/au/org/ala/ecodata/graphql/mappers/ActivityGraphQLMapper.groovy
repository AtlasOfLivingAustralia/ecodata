package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.fetchers.ActivityFetcher
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.Summary
import grails.gorm.DetachedCriteria
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.SingleEntityDataFetcher

class ActivityGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled true
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

            add('outputs', [Output]) {
                dataFetcher { Activity activity ->
                    Output.findAllByActivityId(activity.activityId)
                }
                input false
            }

            //get activity by activity id
            query('activity', Activity) {
                argument('activityId', String)
                dataFetcher(new SingleEntityDataFetcher<Activity>(Activity.gormPersistentEntity) {
                    @Override
                    protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                        Activity.where { activityId == environment.getArgument('activityId') }
                    }
                })
            }

            //get a list of activities by project id
            query('activityByProjectId', [Activity]) {
                argument('projectId', String)
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        Activity.where { projectId == environment.getArgument('projectId') }
                    }
                })
            }

            //get activity schema by activity name
            query('activitySchemaByName', Schema) {
                argument('activityName', String)
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                                Holders.applicationContext.messageSource, Holders.grailsApplication).getActivityByName(environment.getArgument('activityName'))
                    }
                })
            }

            //get the list of activities
            query('activities', [Summary]) {
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                                Holders.applicationContext.messageSource, Holders.grailsApplication).getActivitySummaryList(environment)
                    }
                })
            }
        }

    }
}
