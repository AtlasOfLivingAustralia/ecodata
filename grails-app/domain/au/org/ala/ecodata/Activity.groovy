package au.org.ala.ecodata

import org.bson.types.ObjectId

class Activity {

    static mapping = {
        activityId index: true
        siteId index: true
        version false
    }

    ObjectId id
    String activityId
    String siteId
    String description
    String type
    Date startDate
    Date endDate
    String collector
    String censusMethod
    String methodAccuracy
    String fieldNotes
    String notes
    Date dateCreated
    Date lastUpdated

    static hasMany = [outputs: Output]

    static constraints = {
        description nullable: true
        startDate nullable: true
        endDate nullable: true
        type nullable: true
        collector nullable: true
        censusMethod nullable: true
        methodAccuracy nullable: true
        fieldNotes nullable: true, maxSize: 4000
        notes nullable: true, maxSize: 4000
    }
}
