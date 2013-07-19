package au.org.ala.ecodata

import org.bson.types.ObjectId

class Project {
    def projectService
    def elasticSearchService
    /*
    Associations:
        projects may have 0..n Sites - these are mapped from the Site side
        Activities - not implemented yet
    */
    static mapping = {
        name index: true
        projectId index: true
        version false
    }

    ObjectId id
    String projectId  // required
    String status = 'active'
    String externalProjectId
    String name  // required
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
    Date dateCreated
    Date lastUpdated

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

    def afterInsert() {
        indexDoc()
    }

    def afterUpdate() {
        indexDoc()
    }

    def indexDoc() {
        def thisMap = projectService.toMap(this, "flat")
        thisMap["class"] = this.getClass().name
        elasticSearchService.indexDoc(thisMap)
    }
}
