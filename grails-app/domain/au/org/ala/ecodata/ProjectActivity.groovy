package au.org.ala.ecodata

import org.bson.types.ObjectId

class ProjectActivity {

    ObjectId id
    String projectActivityId
    String projectId
    String name
    String description
    String status
    String pActivityFormName
    String methodType
    boolean commentsAllowed
    Date startDate
    Date endDate
    Map alerts
    List sites = [] // list of sites associated to the survey
    boolean restrictRecordToSites
    boolean allowAdditionalSurveySites
    boolean selectFromSitesOnly
    String baseLayersName
    boolean publicAccess // only editors/admins can add data to a project activity unless publicAccess = true
    VisibilityConstraint visibility = new VisibilityConstraint(embargoOption: EmbargoOption.NONE)
    List<SubmissionRecord> submissionRecords
    String legalCustodianOrganisation
    String spatialAccuracy
    String speciesIdentification
    String temporalAccuracy
    String nonTaxonomicAccuracy
    List<String> dataQualityAssuranceMethods
    String dataQualityAssuranceDescription
    String dataAccessMethod
    String dataAccessExternalURL
    boolean isDataManagementPolicyDocumented
    String dataManagementPolicyDescription
    String dataManagementPolicyURL
    String dataManagementPolicyDocument
    Date dateCreated
    Date lastUpdated

    static embedded = ['visibility']

    static hasMany = [submissionRecords: SubmissionRecord]

    static constraints = {
        endDate nullable: true
        methodType inList: ['opportunistic', 'systematic']
        dataQualityAssuranceMethods validator: { values ->
            [ "dataownercurated", "subjectexpertverification", "crowdsourcedverification", "recordannotation", "systemsupported", "nodqmethodsused", "na" ].containsAll(values)
        }
        pActivityFormName nullable: true
        alerts nullable: true
        sites nullable: true
        restrictRecordToSites nullable: true
        allowAdditionalSurveySites nullable: true
        selectFromSitesOnly nullable: true
        baseLayersName nullable: true
        publicAccess nullable: true
        visibility nullable: true
        submissionRecords nullable: true
        legalCustodianOrganisation nullable: true
        dataAccessExternalURL nullable: true
        dataQualityAssuranceDescription nullable: true
        dataManagementPolicyDescription nullable: true
        dataManagementPolicyURL nullable: true
        dataManagementPolicyDocument nullable: true
    }

    static mapping = {
        projectActivityId index: true
    }
}
