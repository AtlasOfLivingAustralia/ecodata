package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * Currently this holds both activities and assessments.
 */
class Activity {

    /*
    Note:
        activities and assessments are both described by this domain - 'activities' can be used to mean both
    Associations:
        activities must belong to 1 Site - this is mapped by the siteId in this domain
        activities may have 0..n Outputs - a list of outputIds is held in this class
    */

    static mapping = {
        activityId index: true
        siteId index: true
        version false
    }

    ObjectId id
    String activityId
    String status = 'active'
    Boolean assessment = false
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
    List outputs = []

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

    def addToOutputs(output) {
        outputs << output.outputId
    }

    def removeFromOutputs(output) {
        outputs >> output.outputId
    }
}
