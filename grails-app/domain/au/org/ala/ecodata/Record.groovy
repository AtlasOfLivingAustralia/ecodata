package au.org.ala.ecodata

import org.bson.types.ObjectId

class Record {

    static mapping = { version false }

    ObjectId id
    String projectId //ID of the project within ecodata
    String occurrenceID
    String userId
    String eventDate //should be a date in "yyyy-MM-dd" or "2014-11-24T04:55:48+11:00" format
    Double decimalLatitude
    Double decimalLongitude
    Date dateCreated
    Date lastUpdated

    def beforeValidate() {
        if(occurrenceID == null){
            //mint an UUID
            occurrenceID = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        projectId nullable:true
        eventDate nullable:true
        decimalLatitude nullable:true
        decimalLongitude nullable:true
        userId nullable: true
    }
}
