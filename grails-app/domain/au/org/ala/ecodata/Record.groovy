package au.org.ala.ecodata

import org.bson.types.ObjectId

class Record {

    static mapping = { version false }

    ObjectId id
    String projectId //ID of the project within ecodata
    String projectActivityId
    String activityId
    String occurrenceID
    String userId
    String eventDate //should be a date in "yyyy-MM-dd" or "2014-11-24T04:55:48+11:00" format
    Double decimalLatitude
    Double decimalLongitude
    Integer coordinateUncertaintyInMeters
    Integer individualCount
    Integer numberOfOrganisms
    Date dateCreated
    Date lastUpdated
    String outputId
    String json
    Integer outputItemId

    def beforeValidate() {
        if (occurrenceID == null) {
            //mint an UUID
            occurrenceID = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        projectId nullable: true
        projectActivityId nullable: true
        activityId nullable: true
        eventDate nullable: true
        decimalLatitude nullable: true
        decimalLongitude nullable: true
        userId nullable: true
        coordinateUncertaintyInMeters nullable: true
        individualCount nullable: true
        numberOfOrganisms nullable: true
        outputId nullable: true
        json nullable: true
        outputItemId nullable: true
    }
}
