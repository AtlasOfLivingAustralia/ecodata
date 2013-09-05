package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * Currently this holds both activities and assessments.
 */
class Activity {

    /**
     * Values for activity progress.  An enum would probably be better but doesn't seem to
     * work out of the box with mongo/GORM
     */
    public static final String PLANNED = 'planned'
    public static final String STARTED = 'started'
    /*
    Note:
        activities and assessments are both described by this domain - 'activities' can be used to mean both
    Associations:
        activities must belong to 1 Site or 1 project - this is mapped by the siteId or projectId in this domain
        activities may have 0..n Outputs - these are mapped from the Output side
    */

    static mapping = {
        activityId index: true
        siteId index: true
        projectId index: true
        version false
    }

    ObjectId id
    String activityId
    String status = 'active'
    String progress = PLANNED
    Boolean assessment = false
    String siteId
    String projectId
    String description
    String type
    Date startDate
    Date endDate
    Date plannedStartDate
    Date plannedEndDate


    String collector
    String censusMethod
    String methodAccuracy
    String fieldNotes
    String notes
    Date dateCreated
    Date lastUpdated

    static constraints = {
        siteId nullable: true
        projectId nullable: true
        description nullable: true
        startDate nullable: true
        endDate nullable: true
        type nullable: true
        collector nullable: true
        censusMethod nullable: true
        methodAccuracy nullable: true
        fieldNotes nullable: true, maxSize: 4000
        notes nullable: true, maxSize: 4000
        plannedStartDate nullable: true
        plannedEndDate nullable: true
        progress nullable: true
    }

}
