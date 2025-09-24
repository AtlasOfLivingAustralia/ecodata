package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A DataSetSummary represents metadata about a dataset that has been
 * or is being collected as part of a project. It is intended to support
 * reporting of datasets.
 *
 * A DataSetSummary will be created when a /mintCollectionId request
 * is received from the Monitor app via the ParatooController/ParatooService.
 * DataSetsSummaries created in this way contain extra information in the
 * surveyId and orgMintedIdentifier fields.
 */
class DataSetSummary {

    static mapWith = "mongo"

    ObjectId id
    Date dateCreated
    Date lastUpdated
    String dataSetId
    String projectId
    String reportId
    String siteId
    String name

    String status = Status.ACTIVE
    String publicationStatus
    String progress

    Date startDate
    Date endDate
    List<String> methods
    String type
    String collectorType
    String qa
    List<String> measurementTypes
    String term
    String programOutcome
    List<String> projectOutcomes
    List<String> baselines
    Integer serviceId
    String addition
    String owner
    String methodDescription
    String custodian
    Boolean dataCollectionOngoing
    String format
    String published
    List<String> sensitivities
    List<String> investmentPriorities
    String storageType
    String publicationUrl
    String threatenedSpeciesIndex
    Date threatenedSpeciesIndexUploadDate
    Boolean sizeUnknown

    String protocol
    String collectionApp
    Map surveyId
    String orgMintedIdentifier


    static mapping = {
        dataSetId index: true
        projectId index: true
        version false
    }

    static constraints = {
        dataSetId nullable: false, unique: true
        projectId nullable: false
        name nullable: false, blank: false
        status nullable: true
        publicationStatus nullable: true
        progress nullable: true
        startDate nullable: true
        endDate nullable: true
        methods nullable: true
        type nullable: true
        collectorType nullable: true
        qa nullable: true
        measurementTypes nullable: true
        term nullable: true
        programOutcome nullable: true
        addition nullable: true
        owner nullable: true
        methodDescription nullable: true
        custodian nullable: true
        dataCollectionOngoing nullable: true
        format nullable: true
        published nullable: true
        sensitivities nullable: true
        investmentPriorities nullable: true
        storageType nullable: true
        publicationUrl nullable: true
        threatenedSpeciesIndex nullable: true
        threatenedSpeciesIndexUploadDate nullable: true
        siteId nullable: true
        baselines nullable: true
        serviceId nullable: true
        reportId nullable: true
        protocol nullable: true
        collectionApp nullable: true
        surveyId nullable: true
        orgMintedIdentifier nullable: true
        sizeUnknown nullable: true
    }

}
