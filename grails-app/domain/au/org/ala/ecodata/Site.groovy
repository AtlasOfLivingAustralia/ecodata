package au.org.ala.ecodata

import org.bson.types.ObjectId

class Site {

    static mapping = {
        name index: true
        siteId index: true
        version false
    }

    ObjectId id
    String siteId
    String externalSiteId
    String projectId
    String projectName
    String name
    String type
    String description
    String habitat
    List location = []
    String area
    String recordingMethod
    String landTenure
    String protectionMechanism
    Map activities = [:] // activityId, activity
    String notes

    Date dateCreated
    Date lastUpdated

    static embedded = ['location']

    static constraints = {
        externalSiteId nullable:true
        projectId nullable:true
        projectName nullable:true
        type nullable:true
        description nullable:true, maxSize: 40000
        habitat nullable:true
        area nullable:true
        recordingMethod nullable:true
        landTenure nullable:true
        protectionMechanism nullable:true
        notes nullable:true, maxSize: 40000
    }
}
