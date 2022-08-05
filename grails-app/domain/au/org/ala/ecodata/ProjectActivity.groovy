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
    String methodName
    boolean commentsAllowed
    Date startDate
    Date endDate
    Map alerts
    List sites = [] // list of sites associated to the survey
    boolean restrictRecordToSites
    boolean allowAdditionalSurveySites
    /**
     * Removed selectFromSitesOnly
     */
    String baseLayersName
    String excludeProjectSite // Exclude projectsite from site selection dropdown
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
    List<String> dataAccessMethods
    String dataAccessExternalURL
    boolean isDataManagementPolicyDocumented
    String dataManagementPolicyDescription
    String dataManagementPolicyURL
    String dataManagementPolicyDocument
    String dataSharingLicense
    MapLayersConfiguration mapLayersConfig
    String surveySiteOption
    boolean canEditAdminSelectedSites
    boolean published
    Date dateCreated
    Date lastUpdated

    static embedded = ['visibility', 'mapLayersConfig']

    static hasMany = [submissionRecords: SubmissionRecord]

    static constraints = {
        endDate nullable: true
        methodType inList: ['opportunistic', 'systematic']
        spatialAccuracy inList: ['low', 'moderate', 'high']
        speciesIdentification inList: ['low', 'moderate', 'high', 'na']
        temporalAccuracy inList: ['low', 'moderate', 'high']
        nonTaxonomicAccuracy inList: ['low', 'moderate', 'high']
        dataQualityAssuranceMethods validator: { values ->
            [ "dataownercurated", "subjectexpertverification", "crowdsourcedverification", "recordannotation", "systemsupported", "nodqmethodsused", "na" ].containsAll(values)
        }
        dataAccessMethods validator: { values ->
            ["oasrdfs", "oaordfs", "lsrds", "ordfsvr", "oasrdes", "casrdes", "rdna", "odidpa", "na"].containsAll(values)
        }
        pActivityFormName nullable: true
        alerts nullable: true
        sites nullable: true
        restrictRecordToSites nullable: true
        allowAdditionalSurveySites nullable: true
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
        excludeProjectSite nullable: true
        mapLayersConfig nullable: true
        surveySiteOption nullable: true, inList: ['sitepick','sitecreate', 'sitepickcreate']
        canEditAdminSelectedSites nullable: true
        published nullable: true
    }

    static mapping = {
        projectActivityId index: true
    }
}
