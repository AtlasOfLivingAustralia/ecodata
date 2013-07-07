package au.org.ala.ecodata

import org.bson.types.ObjectId

class Site {

    /*
    Associations:
        sites may belong to 0..n Projects - a list of projectIds are stored in each site
        sites may have 0..n Activities/Assessments - mapped from the Activity side
    */

    static mapping = {
        name index: true
        siteId index: true
        projects index: true
        version false
    }

    ObjectId id
    String siteId
    String status = 'active'
    String externalSiteId
    List projects = []
    String name
    String type
    String description
    String habitat
    List location = []
    String area
    String recordingMethod
    String landTenure
    String protectionMechanism
    String notes
    Date dateCreated
    Date lastUpdated

    static constraints = {
        name nullable: true
        externalSiteId nullable:true
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
