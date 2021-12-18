package au.org.ala.ecodata

import org.bson.types.ObjectId

class Setting {

    ObjectId id
    String key
    String value
    String description
    Date lastUpdated
    Date dateCreated

    static constraints = {
        key nullable: false
        value nullable: false
        description nullable: true
        lastUpdated nullable: true
        dateCreated nullable: true
    }

}
