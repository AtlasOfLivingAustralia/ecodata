package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.MeriPlan
import au.org.ala.ecodata.ProjectOutcome
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class MeriPlanGraphQLMapper {
    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
                operations.get.enabled false
                operations.list.enabled false
                operations.count.enabled false
                operations.create.enabled false
                operations.update.enabled false
                operations.delete.enabled false


            exclude('details', 'errors', 'id')
            add('primaryOutcome', ProjectOutcome) {
                dataFetcher { MeriPlan meriPlan ->
                    meriPlan.getPrimaryOutcome()
                }
            }
            add('secondaryOutcomes', [ProjectOutcome]) {
                dataFetcher { MeriPlan meriPlan ->
                    meriPlan.getSecondaryOutcomes()
                }
            }

            add('investmentPriorities', [String]) {
                dataFetcher { MeriPlan meriPlan ->
                    meriPlan.getInvestmentPriorities()
                }
            }

            add('shortTermOutcomes', [ProjectOutcome]) {
                dataFetcher { MeriPlan meriPlan ->
                    meriPlan.getShortTermOutcomes()
                }
            }

            add('midTermOutcomes', [ProjectOutcome]) {
                dataFetcher { MeriPlan meriPlan ->
                    meriPlan.getMidTermOutcomes()
                }
            }

        }

    }
}
