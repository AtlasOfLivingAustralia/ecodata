package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.ProjectActivity
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.ClosureDataFetchingEnvironment

class ProjectActivityGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled true
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

            exclude("sites")

            add("sites", [String]){
                dataFetcher { ProjectActivity projectActivity, ClosureDataFetchingEnvironment env ->
                    projectActivity.sites
                }
            }

            add('surveyForms', [ActivityForm]) {
                dataFetcher { ProjectActivity projectActivity, ClosureDataFetchingEnvironment env ->
                    ActivityForm.findAllByName(projectActivity.pActivityFormName)
                }
            }


        }

    }
}
