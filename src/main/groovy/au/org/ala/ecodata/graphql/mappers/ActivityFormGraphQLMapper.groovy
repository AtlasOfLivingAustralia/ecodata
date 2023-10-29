package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.*
import grails.gorm.DetachedCriteria
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.SingleEntityDataFetcher

class ActivityFormGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled true
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

            exclude("sections")

            add("formSections", [FormSection]) {
                dataFetcher { ActivityForm activityForm ->
                    List<FormSection> sectionList = activityForm.sections.each {
                        new FormSection(
                                title: it.title,
                                template: it.template,
                                name: it.name,
                                modelName: it.modelName,
                                templateName: it.templateName,
                                optionalQuestionText: it.optionalQuestionText,
                                optional: it.optional,
                                collapsedByDefault: it.collapsedByDefault
                        )
                    }
                    return sectionList
                }
            }

            //get the activity form schema by activity form name
            query('activityForm', ActivityForm) {
                argument('activityFormName', String)
                dataFetcher(new SingleEntityDataFetcher<ActivityForm>(ActivityForm.gormPersistentEntity) {
                    @Override
                    protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                        //need to get the latest activity form based on the formVersion
                        ActivityForm.where { name == environment.getArgument('activityFormName') }.sort('formVersion', 'desc')
                    }
                })
            }
        }
    }
}
