package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.models.SectionTemplate
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class FormSectionGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            operations.get.enabled false
            operations.list.enabled false
            operations.list.paginate(false)
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

            exclude("template")

            add("formTemplate", SectionTemplate) {
                dataFetcher { FormSection formSection ->
                    formSection.getSectionTemplate()
                }
            }
        }
    }
}
