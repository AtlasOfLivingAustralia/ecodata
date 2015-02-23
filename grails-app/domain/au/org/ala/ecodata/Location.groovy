package au.org.ala.ecodata

import org.bson.types.ObjectId

class Location {

    static mapping = { version false }

    ObjectId id
    String locationId
    String userId
    String locality
    Double decimalLatitude
    Double decimalLongitude
    String geodeticDatum
    Date dateCreated

    def beforeValidate() {
        if(locationId == null){
            //mint an UUID
            locationId = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        geodeticDatum nullable:true
    }
}
