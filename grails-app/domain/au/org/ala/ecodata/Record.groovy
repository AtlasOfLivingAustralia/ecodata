package au.org.ala.ecodata

import org.bson.types.ObjectId

class Record {

    static mapping = { version false }

    ObjectId id
    String eventDate //should be a date in "yyyy-MM-dd" format
    String eventTime //should be a date in "HH:mm" format
    String decimalLatitude
    String decimalLongitude
    String userId
    Date dateCreated
    Date lastUpdated

    static constraints = {
        eventDate nullable:true
        eventTime nullable:true
        decimalLatitude nullable:true
        decimalLongitude nullable:true
        userId nullable: true
    }
}
