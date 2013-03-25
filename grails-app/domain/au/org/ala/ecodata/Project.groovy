package au.org.ala.ecodata

import org.bson.types.ObjectId

class Project {

    static mapping = {
        name index: true
        projectId index: true
        version false
    }

    ObjectId id
    String projectId
    String externalProjectId
    String name
    String description
    String manager
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

    static hasMany = [sites:Site]

    static constraints = {
        externalProjectId nullable:true
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
    }
}
