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
        version false
		state defaultValue:'Active'
    }

    ObjectId id
    String projectId  // required
    String status = 'active'
    String externalId
    String name  // required
    String description
    String manager
    String grantId
    String groupId
    String groupName
    String organisationName
    Date plannedStartDate
    Date plannedEndDate
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
	String state
	List variations = []
	
    static constraints = {
        externalId nullable:true
        description nullable:true, maxSize: 40000
        manager nullable:true
        groupId nullable:true
        groupName nullable:true
        organisationName nullable:true
        plannedStartDate nullable:true
        plannedEndDate nullable:true
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
		state nullable:true
    }
}
