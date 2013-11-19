package au.org.ala.ecodata

import org.bson.types.ObjectId

class Setting {

    ObjectId id
    String key
    String value
    String description

    static constraints = {
        key nullable: false
        value nullable: false
        description nullable: true
    }

}
