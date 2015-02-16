package au.org.ala.ecodata

import org.bson.types.ObjectId

class Project {

    /*
    Associations:
        projects may have 0..n Sites - these are mapped from the Site side
        Activities - not implemented yet
    */
    static mapping = {
        name index: true
        projectId index: true
		promoteOnHomepage index: true
        version false
    }

    ObjectId id
    String projectId      // same as collectory dataProvider id
    String dataResourceId // one collectory dataResource stores all sightings
    String status = 'active'
    String externalId
    String name  // required
    String description
    String manager
    String grantId
    String groupId
    String groupName
    String organisationName
    String serviceProviderName
    String organisationId
    Date plannedStartDate
    Date plannedEndDate
    Date serviceProviderAgreementDate
    Date actualStartDate
    Date actualEndDate
    String fundingSource
    String fundingSourceProjectPercent
    String plannedCost
    String reportingMeasuresAddressed
    String projectPlannedOutputType
    String projectPlannedOutputValue
	Map custom
	Map risks
	Date dateCreated
    Date lastUpdated
	String promoteOnHomepage = 'no'
	
    boolean isCitizenScience
    String projectType    // survey, works
    String aim, keywords, urlAndroid, urlITunes, urlWeb
    String getInvolved, scienceType, projectSiteId
    double funding
    String orgIdGrantee, orgIdSponsor, orgIdSvcProvider
    String userCreated, userLastModified

    static constraints = {
        externalId nullable:true
        description nullable:true, maxSize: 40000
        manager nullable:true
        groupId nullable:true
        groupName nullable:true
        organisationName nullable:true
        serviceProviderName nullable:true
        plannedStartDate nullable:true
        plannedEndDate nullable:true
        serviceProviderAgreementDate nullable:true
        actualStartDate nullable:true
        actualEndDate nullable:true
        fundingSource nullable:true
        fundingSourceProjectPercent nullable:true
        plannedCost nullable:true
        reportingMeasuresAddressed nullable:true
        projectPlannedOutputType nullable:true
        projectPlannedOutputValue nullable:true
        grantId nullable:true
		custom nullable:true
		risks nullable:true
        promoteOnHomepage nullable:true
        organisationId nullable:true
        projectType nullable:true    // nullable for backward compatibility; survey, works
        dataResourceId nullable:true // nullable for backward compatibility
        aim nullable:true
        keywords nullable:true
        urlAndroid nullable:true
        urlITunes nullable:true
        urlWeb nullable:true
        getInvolved nullable:true
        scienceType nullable:true
        orgIdGrantee nullable:true
        orgIdSponsor nullable:true
        orgIdSvcProvider nullable:true
        projectSiteId nullable:true // nullable for backward compatibility
        userCreated nullable:true
        userLastModified nullable:true
    }
}
