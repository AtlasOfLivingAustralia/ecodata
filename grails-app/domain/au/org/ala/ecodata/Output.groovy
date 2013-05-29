package au.org.ala.ecodata

import org.bson.types.ObjectId

class Output {

    /*
    Associations:
        outputs must belong to 1 Activity - a list of outputIds is held in the Activity domain
        this domain currently holds the activityId but it is not used for associations and may be removed
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
    Date dateCreated
    Date lastUpdated

    static constraints = {
        assessmentDate nullable: true
    }
}
