package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.Document
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.Site
import au.org.ala.ecodata.Status
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

        }
    }


    static ProjectsFetcher buildTestFetcher() {

        new ProjectsFetcher(Holders.applicationContext.projectService, Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService)

    }
}
