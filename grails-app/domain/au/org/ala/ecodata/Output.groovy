package au.org.ala.ecodata

import org.bson.types.ObjectId

class Output {

    static mapWith="mongo"

    static mapping = {
        activityId index: true
        outputId index: true
        version false
    }

    ObjectId id
    String outputId
    String activityId
    Date assessmentDate
    Date dateCreated
    Date lastUpdated

    static constraints = {
        assessmentDate nullable: true
    }
}
