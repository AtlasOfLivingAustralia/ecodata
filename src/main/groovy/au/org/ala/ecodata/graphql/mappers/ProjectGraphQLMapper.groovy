package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.Document
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.Site
import au.org.ala.ecodata.Status
import au.org.ala.ecodata.graphql.enums.DateRange
import au.org.ala.ecodata.graphql.enums.YesNo
import au.org.ala.ecodata.graphql.fetchers.ActivityFetcher
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import au.org.ala.ecodata.graphql.models.MeriPlan
import grails.gorm.DetachedCriteria
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.ClosureDataFetchingEnvironment
import org.grails.gorm.graphql.fetcher.impl.SingleEntityDataFetcher

class ProjectGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled true
            operations.list.paginate(true)
            operations.count.enabled false
            operations.create.enabled true
            operations.update.enabled false
            operations.delete.enabled false

            exclude("custom")

            List<String> restrictedProperties = []
            restrictedProperties.each { String prop ->
                property(prop) {
                    dataFetcher { Project project, ClosureDataFetchingEnvironment env ->
                        boolean canRead = env.environment.context.acl.canRead(env.source, project)
                        if (canRead) {
                            return project[prop]
                        }
                        return null
                    }
                }
            }

            add('meriPlan', MeriPlan) {
                dataFetcher { Project project ->
                    project.getMeriPlan()
                }
            }

            add('documents', [Document]) {
                dataFetcher { Project project, ClosureDataFetchingEnvironment env ->
                    Document.findAllByProjectIdAndStatusNotEqual(project.projectId, Status.DELETED)
                }
            }
            add('reports', [Report]) {
                dataFetcher { Project project ->
                    Report.findAllByProjectIdAndStatusNotEqual(project.projectId, Status.DELETED)
                }
            }

            add('sites', [Site]) {
                dataFetcher { Project project ->
                    Site.findAllByProjectsAndStatusNotEqual(project.projectId, Status.DELETED)
                }
            }

            add('activities', [Activity]) {
                dataFetcher { Project project ->
                    new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                            Holders.applicationContext.messageSource, Holders.grailsApplication).getFilteredActivities(project.tempArgs, project.projectId)
                }
            }

            // get project by ID
            query('project', Project) {
                argument('projectId', String)
                dataFetcher(new SingleEntityDataFetcher<Project>(Project.gormPersistentEntity) {
                    @Override
                    protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                        Project.where { projectId == environment.getArgument('projectId') }
                    }
                })
            }

            query('projects', [Project]) {
                argument('term', String)
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().get(environment)
                    }
                })
            }

            query('searchMeritProject', [Project]) {
                argument('projectId', String) { nullable true }
                argument('fromDate', String){ nullable true description "yyyy-mm-dd"  }
                argument('toDate', String){ nullable true description "yyyy-mm-dd" }
                argument('dateRange', DateRange){ nullable true }
                argument('status', [String]){ nullable true }
                argument('organisation', [String]){ nullable true }
                argument('associatedProgram', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('mainTheme', [String]){ nullable true }
                argument('state', [String]){ nullable true }
                argument('lga', [String]){ nullable true }
                argument('cmz', [String]){ nullable true }
                argument('partnerOrganisationType', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('primaryOutcome', [String]){ nullable true }
                argument('secondaryOutcomes', [String]){ nullable true }
                argument('tags', [String]){ nullable true }

                argument('managementArea', [String]){ nullable true }
                argument('majorVegetationGroup', [String]){ nullable true  }
                argument('biogeographicRegion', [String]){ nullable true }
                argument('marineRegion', [String]){ nullable true }
                argument('otherRegion', [String]){ nullable true }
                argument('grantManagerNominatedProject', [YesNo]){ nullable true }
                argument('federalElectorate', [String]){ nullable true }
                argument('assetsAddressed', [String]){ nullable true }
                argument('userNominatedProject', [String]){ nullable true }
                argument('managementUnit', [String]){ nullable true }

                //activities filter
                argument('activities', 'activities') {
                    accepts {
                        field('activityType', String) {nullable true}
                        field('output', 'output') {
                            field('outputType', String) {nullable false}
                            field('fields', [String]) {nullable true}
                            nullable true
                            //one activity can have zero or more output
                            collection true
                        }
                        //one project can have many activities
                        collection true
                    }
                    nullable true
                }

                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().searchMeritProject(environment)
                    }
                })
            }

        }
    }


    static ProjectsFetcher buildTestFetcher() {

        new ProjectsFetcher(Holders.applicationContext.projectService, Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService)

    }
}
