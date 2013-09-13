package au.org.ala.ecodata

import org.bson.types.ObjectId

class Output {

    /*
    Associations:
        outputs must belong to 1 Activity - this is mapped by the activityId in this domain
    */

    static mapWith="mongo"

    static mapping = {
        activityId index: true
        outputId index: true
        version false
    }

    ObjectId id
    String outputId
    String status = 'active'
    String activityId
    Date assessmentDate
    String name
    Date dateCreated
    Date lastUpdated

    static constraints = {
        assessmentDate nullable: true
        name nullable: true
    }
}
