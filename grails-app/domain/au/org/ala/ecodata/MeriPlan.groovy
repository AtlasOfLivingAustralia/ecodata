package au.org.ala.ecodata


import au.org.ala.ecodata.graphql.models.TargetMeasure
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class KeyEvaluationQuestion {
    String question
    String evaluationApproach

    KeyEvaluationQuestion(Map details) {
        this.question = details.data1 ?: ''
        this.evaluationApproach = details.data2 ?: ''
    }
}

class DeliveredAgainstTarget {
    String scoreId
    double amountDelivered
    Double target

    DeliveredAgainstTarget(Map aggregationResult) {
        this.scoreId = aggregationResult.scoreId
        this.target = aggregationResult.target ?: null
        this.amountDelivered = aggregationResult.result?.result ?: 0
    }
}

class KeyThreat {
    List<String> relatedOutcomes
    String threat
    String threatCode
    String intervention
    String evidence
    List<String> relatedTargetMeasures

    List<String> getTargetMeasures() {
        relatedTargetMeasures ?: []
    }

    String getDescription() {
        threat
    }

}

class Partnership {
    String partnerName
    String description
    String partnerOrganisationType
}

class MonitoringMethodology {
    String relatedBaseline
    List<String> protocols
    String data1
    List<String> relatedTargetMeasures
    String data2
    String evidence
    List<String> getEmsaModules() {
        protocols
    }

    String getDescription() {
        data1
    }

    String getMethod() {
        data2
    }

    List<String> getTargetMeasures() {
        relatedTargetMeasures ?: []
    }


}

class Baseline {
    String code
    String baseline
    String monitoringDataStatus
    List<String> relatedTargetMeasures
    List<String> protocols
    String method
    String evidence
    List<String> relatedOutcomes

    String getDescription() {
        baseline
    }

    List<String> getEmsaModules() {
        protocols
    }

    String existsOrToBeEstablished() {
        monitoringDataStatus
    }

}

class ManagementPlan {
    String data1
    String data2
    String data3
    String documentUrl

    String getDocumentName() {
        data1
    }

    String getStrategicAlignment() {
        data3
    }

    String getDocumentSection() {
        data2
    }
}

/**
 * A MERI Plan stands for Monitoring, Evaluation, Reporting and Improvement Plan.
 * It is a document that outlines the expected outcomes of a project, what natural assets are being
 * protected or enhanced and how the outcomes will be delivered, measured and reported.
 */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class MeriPlan {

    Map details
    List<OutputTarget> outputTargets
    String publicationStatus = PublicationStatus.DRAFT
    Date lastUpdated
    Project project

    List<String> supportedPriorityPlaces
    String firstNationsPeopleInvolvement
    String evaluationApproach
    String implementationOrDeliveryAssumptions

    MeriPlan(Project project) {

        this.project = project
        String lastUpdated = project?.custom?.details?.lastUpdated
        this.lastUpdated = lastUpdated ? DateUtil.parseDateWithAndWithoutMilliSeconds(lastUpdated) : null

        this.details = project?.custom?.details ?: [:]
        this.supportedPriorityPlaces = this.details.supportedPriorityPlaces ?: null
        this.firstNationsPeopleInvolvement = this.details.indigenousInvolvementType ?: null
        this.publicationStatus = getPublicationStatus()
        this.outputTargets = project?.outputTargets ?: []
        this.implementationOrDeliveryAssumptions = details.implementation?.description ?: null
        this.evaluationApproach = details.projectEvaluationApproach

    }

    String getProjectId() {
        project?.projectId
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

    List<MonitoringMethodology> getMonitoringMethodology() {
        details?.monitoring?.rows?.collect { Map monitoringDetails ->
            if (monitoringDetails && monitoringDetails.data1) {
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

    List<KeyEvaluationQuestion> getKeyEvaluationQuestions() {
        details?.keq?.rows?.collect { Map keq ->
            if (keq?.data1 || keq?.data2) {
                new KeyEvaluationQuestion(keq)
            }
        }?.findAll { it }
    }

    List<ManagementPlan> getConservationAndManagementPlans() {
        details?.priorities?.rows?.collect { Map managementPlanDetails ->
            if (managementPlanDetails && managementPlanDetails.data1) {
                new ManagementPlan(managementPlanDetails)
            }
        }?.findAll { it }
    }

}