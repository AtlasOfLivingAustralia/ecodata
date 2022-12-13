package au.org.ala.ecodata


import au.org.ala.ecodata.graphql.models.MeriPlan
import au.org.ala.ecodata.graphql.mappers.ProjectGraphQLMapper
import org.springframework.validation.Errors

import static au.org.ala.ecodata.Status.COMPLETED
import au.org.ala.ecodata.graphql.models.MeriPlan
import au.org.ala.ecodata.graphql.mappers.ProjectGraphQLMapper

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Interval

import static au.org.ala.ecodata.Status.COMPLETED

class Project {

    static graphql = ProjectGraphQLMapper.graphqlMapping()

    /*
    Associations:
        projects may have 0..n Sites - these are mapped from the Site side
        Activities - not implemented yet
    */
    static mapping = {
        name index: true
        projectId index: true
		promoteOnHomepage index: true
        externalId index: true
        dataResourceId index: true
        version false
    }

    ObjectId id
    String projectId
    /** The id of the hub in which this project was created */
    String hubId
    String dataProviderId // collectory dataProvider id
    String dataResourceId // one collectory dataResource stores all sightings
    String status = 'active'
    String terminationReason
    String externalId
    String name  // required
    String description
    String manager
    String grantId
    Date contractStartDate
    Date contractEndDate
    String organisationName
    String serviceProviderName
    String organisationId
    Date plannedStartDate
    Date plannedEndDate
    Date serviceProviderAgreementDate
    Date actualStartDate
    Date actualEndDate
    String managementUnitId
	Map custom
	Risks risks
	Date dateCreated
    Date lastUpdated
	String promoteOnHomepage = 'no'
    List activities
    boolean isCitizenScience, isMERIT
    String difficulty, gear, task
    String projectType    // survey, works
    // TODO urlAndroid and urlITunes need to be phased out; replaced by link-type documente
    String aim, keywords, urlAndroid, urlITunes, urlWeb
    String getInvolved, projectSiteId
    List <String> scienceType = []
    List <String> ecoScienceType = []
    List <String> tags = []
    double funding
    String orgIdGrantee, orgIdSponsor, orgIdSvcProvider
    String userCreated, userLastModified
    boolean isExternal = false // An external project only has a listing with the ALA and is not using data capture capabilities
    boolean isSciStarter = false
    List<String> uNRegions = []
    List<String> countries = []
    List<String> industries = []
    List<String> bushfireCategories = []
    boolean isBushfire
    String projLifecycleStatus

    /** The system in which this project was created, eg. MERIT / SciStarter / BioCollect / Grants Hub / etc */
    String origin = 'atlasoflivingaustralia'
    String baseLayer
    MapLayersConfiguration mapLayersConfig
    /** configure how activity is displayed on map for example point, heatmap or cluster. */
    List mapDisplays
    List tempArgs = []

    boolean alaHarvest = false
    //For embedded table, needs to conversion in controller
    List<Funding> fundings

    List<AssociatedOrg> associatedOrgs

    /** Associates a list of ids from external systems with this project */
    List<ExternalId> externalIds

    /** The program of work this project is a part of, if any */
    String programId

    /** Grant/procurement etc */
    String fundingType

    /** If this project represents an election commitment, the year of the commitment (String typed to allow financial years) */
    String electionCommitmentYear

    /** Records geographic information about the project that isn't derived from the project Sites */
    GeographicInfo geographicInfo

    /** Information about the organisation/department overseeing the project */
    String portfolio

    /** Electorate Reporting Comment */
    String comment

    List<OutputTarget> outputTargets

    static embedded = ['associatedOrgs', 'fundings', 'mapLayersConfig', 'risks', 'geographicInfo', 'externalIds', 'outputTargets']

    static transients = ['activities', 'plannedDurationInWeeks', 'actualDurationInWeeks', 'tempArgs']

    Date getActualStartDate() {
        if (actualStartDate) {
            return actualStartDate
        }
        if (activities) {
            return activities.min{it.startDate}?.startDate ?: null
        }
        return null;
    }

    Date getActualEndDate() {
        if (actualEndDate) {
            return actualEndDate
        }
        if (status == COMPLETED && activities) {
            return activities.max{it.endDate}?.endDate ?: null
        }
        return null;
    }

    Integer getActualDurationInWeeks() {
        return intervalInWeeks(getActualStartDate(), getActualEndDate())
    }

    Integer getPlannedDurationInWeeks() {
        return intervalInWeeks(plannedStartDate, plannedEndDate)
    }

    Integer getContractDurationInWeeks() {
        return intervalInWeeks(contractStartDate, contractEndDate)
    }

    /**
     * Compatibility method to extract the workOrderId from the embedded list.  Returns the first
     * ExternalId with type WORK_ORDER from the externalIds field
     */
    String getWorkOrderId() {
        externalIds.find{it.idType == ExternalId.IdType.WORK_ORDER}?.externalId
    }

    /**
     * Compatibility method to extract the internalOrderId from the embedded list.  Returns the first
     * ExternalId with type INTERNAL_ORDER from the externalIds field
     */
    String getInternalOrderId() {
        externalIds.find{it.idType == ExternalId.IdType.INTERNAL_ORDER_NUMBER}?.externalId
    }

    private Integer intervalInWeeks(Date startDate, Date endDate) {
        if (!startDate || !endDate) {
            return null
        }
        DateTime start = new DateTime(startDate)
        DateTime end = new DateTime(endDate);

        Interval interval = new Interval(start, end)
        int numDays = Days.daysIn(interval).days
        double numWeeks = numDays / 7.0
        return (int)Math.ceil(numWeeks)
    }

    static constraints = {
        externalId nullable:true
        description nullable:true, maxSize: 40000
        contractStartDate nullable: true
        contractEndDate nullable: true
        manager nullable:true
        organisationName nullable:true
        serviceProviderName nullable:true
        plannedStartDate nullable:true
        plannedEndDate nullable:true
        serviceProviderAgreementDate nullable:true
        actualStartDate nullable:true
        actualEndDate nullable:true
        grantId nullable:true
		custom nullable:true
		risks nullable:true
        promoteOnHomepage nullable:true
        organisationId nullable:true
        projectType nullable:true    // nullable for backward compatibility; survey, works
        dataProviderId nullable:true // nullable for backward compatibility
        dataResourceId nullable:true // nullable for backward compatibility
        aim nullable:true
        keywords nullable:true
        urlAndroid nullable:true, url:true // TODO phased out
        urlITunes nullable:true, url:true // TODO phased out
        urlWeb nullable:true, url:true
        getInvolved nullable:true
        scienceType nullable:true
        ecoScienceType nullable:true
        orgIdGrantee nullable:true
        orgIdSponsor nullable:true
        orgIdSvcProvider nullable:true
        projectSiteId nullable:true // nullable for backward compatibility
        difficulty nullable:true, inList: ['Easy','Medium','Hard']
        gear nullable:true
        task nullable:true
        userCreated nullable:true
        userLastModified nullable:true
        origin nullable: true
        uNRegions nullable: true
        countries nullable: true
        tags nullable: true
        alaHarvest nullable: true
        industries nullable: true
        programId nullable: true
        baseLayer nullable: true
        isBushfire nullable: true
        bushfireCategories nullable: true
        mapLayersConfig nullable: true
        managementUnitId nullable: true
        mapDisplays nullable: true
        terminationReason nullable: true
        fundingType nullable: true
        electionCommitmentYear nullable: true
        geographicInfo nullable:true
        portfolio nullable: true
        comment nullable: true
        projLifecycleStatus nullable: true, inList: [PublicationStatus.PUBLISHED, PublicationStatus.DRAFT]
        hubId nullable: true, validator: { String hubId, Project project, Errors errors ->
            GormMongoUtil.validateWriteOnceProperty(project, 'projectId', 'hubId', errors)
        }

        externalIds nullable: true, validator: { List<ExternalId> externalIds, Project project, Errors errors ->
            if (externalIds?.size() != externalIds?.toUnique()?.size()) {
                errors.rejectValue('externalIds', 'Each ExternalId in externalIds must be unique')
            }
        }
    }

    MeriPlan getMeriPlan() {
        if(!custom) {
            return null
        }

        MeriPlan meriPlan = new MeriPlan()
        meriPlan.details = custom.get("details")
        meriPlan.outputTargets = this.outputTargets
        return meriPlan
    }
}

