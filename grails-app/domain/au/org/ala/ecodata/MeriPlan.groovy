package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.MeriPlanGraphQLMapper
import au.org.ala.ecodata.graphql.models.TargetMeasure
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class AmountDelivered {
    TargetMeasure targetMeasure
    double amountDelivered
}

class KeyThreat {
    List<String> relatedOutcomes
    String threat
    String threatCode
    String intervention
    List<String> relatedTargetMeasures

}

class Partnership {
    String partnerName
    String description
    String partnerOrganisationType
}

class MonitoringMethodology {
    String baselineCode
    List<String> method
    String description
    List<String> relatedTargetMeasures
    String otherMethod
    String evidenceToRetain

}

class Baseline {
    String code
    String description
    String existsOrToBeEstablished
    List<String> relatedTargetMeasures
    List<String> methods
    String otherMethod
    String evidenceToRetain
    List<String> relatedOutcomes
}

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
    String publicationStatus = PublicationStatus.DRAFT
    Date lastUpdated
    Project project

    MeriPlan(Project project) {

        this.project = project
        String lastUpdated = project?.custom?.details?.lastUpdated
        this.lastUpdated = lastUpdated ? DateUtil.parseDateWithAndWithoutMilliSeconds(lastUpdated) : null

        this.details = project?.custom?.details ?: [:]
        this.outputTargets = project?.outputTargets ?: []
    }

    String getPublicationStatus() {
        String planStatus = project.planStatus

        if (!planStatus) {
            // If the project does not have a plan status, default to DRAFT
            return project.custom?.details ? PublicationStatus.DRAFT : null
        }
        // Convert from MERI plan status to publication status
        Map statusMap = [
            'not approved': PublicationStatus.DRAFT,
            'approved': PublicationStatus.PUBLISHED,
            'submitted': PublicationStatus.SUBMITTED_FOR_REVIEW
        ]

        statusMap[planStatus]
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

    List<KeyThreat> getKeyThreats() {
        details.threats?.rows?.collect { Map threatDetails ->
            if (threatDetails && threatDetails.threat) {
                new KeyThreat(threatDetails)
            }
        }?.findAll { it }
    }

    String deliveryAssumptions() {
        details?.implementation?.description
    }

    List<Partnership> getPartnerships() {
        details?.partnership?.rows?.collect { Map partnershipDetails ->
            if (partnershipDetails && partnershipDetails.data1) {
                new Partnership(partnerName: partnershipDetails.data1,
                                description: partnershipDetails.data2,
                                partnerOrganisationType: partnershipDetails.data3)
            }
        }?.findAll { it }
    }

    List<MonitoringMethodology> getMonitoringMethodologies() {
        details?.monitoring?.rows?.collect { Map monitoringDetails ->
            if (monitoringDetails && monitoringDetails.methodology) {
                new MonitoringMethodology(monitoringDetails)
            }
        }?.findAll { it }
    }

    List<MonitoringMethodology> getMonitoringMethodology() {
        details?.monitoring?.rows?.collect { Map monitoringDetails ->
            if (monitoringDetails && monitoringDetails.methodology) {
                new MonitoringMethodology(monitoringDetails)
            }
        }?.findAll { it }
    }

    List<Baseline> getBaselines() {
        details?.baseline?.rows?.collect { Map baselineDetails ->
            if (baselineDetails && baselineDetails.code) {
                new Baseline(baselineDetails)
            }
        }?.findAll { it }
    }

}