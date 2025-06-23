package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.MeriPlanGraphQLMapper
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * A MERI Plan stands for Monitoring, Evaluation, Reporting and Improvement Plan.
 * It is a document that outlines the expected outcomes of a project, what natural assets are being
 * protected or enhanced and how the outcomes will be delivered, measured and reported.
 */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class MeriPlan {

    static graphql = MeriPlanGraphQLMapper.graphqlMapping()

    Map details
    List<OutputTarget> outputTargets


    MeriPlan(Map meriPlanDetails, List<OutputTarget> outputTargets) {
        details = meriPlanDetails
        this.outputTargets = outputTargets
    }


    ProjectOutcome getPrimaryOutcome() {
        details.outcomes?.primaryOutcome ? new ProjectOutcome(details.outcomes.primaryOutcome) : null
    }
    List<ProjectOutcome> getSecondaryOutcomes() {
        List<ProjectOutcome> outcomes = []
        details.outcomes?.secondaryOutcomes?.each { Map outcome ->
            if (outcome && outcome.description) {
                outcomes.add(new ProjectOutcome(outcome))
            }
        }
        outcomes
    }


    List<ProjectOutcome> getShortTermOutcomes() {
        details.outcomes?.shortTermOutcomes?.collect { new ProjectOutcome(it) }
    }

    List<ProjectOutcome> getMidTermOutcomes() {
        List outcomes = details.outcomes?.midTermOutcomes?.collect { new ProjectOutcome(it) } ?: []
        outcomes
    }

    List <String> getInvestmentPriorities() {
        List assets = []
        assets.addAll(details.objectives.rows1?.collect{it.assets}?:[])
        assets.addAll(details.assets?.collect{it.description}?:[])
        assets.addAll(details.outcomes?.primaryOutcome?.assets?:[])
        assets.addAll(details.outcomes?.secondaryOutcomes?.collect({it.assets})?:[])

        assets.flatten()?.findAll{it}
    }

}