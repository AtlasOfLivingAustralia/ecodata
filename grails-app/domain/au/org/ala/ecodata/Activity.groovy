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
    public static final String FINISHED = 'finished'
    public static final String DEFERRED = 'deferred'
    public static final String CANCELLED = 'cancelled'

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
        projectActivityId index: true
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
    /**
     * Allows grouping of project activities into stages or milestones for planning and reporting purposes.
     * Biodiversity & CFOC projects plan activities in six monthly groups (Stage 1, Stage 2...)
     */
    String projectStage
    Date plannedStartDate
    Date plannedEndDate
    /** The program sponsoring or funding this Activity (e.g. Biodiversity Fund Round 1) */
    String associatedProgram
    /** Allows for breakdown of a program into separately reportable units (e.g. Biodiversity Fund Round 1 has three themes) */
    String associatedSubProgram
    String collector
    String censusMethod
    String methodAccuracy
    String fieldNotes
    String notes
    Date dateCreated
    Date lastUpdated

    String projectActivityId

    /** An activity is considered complete if it's progress attribute is finished, deferred or cancelled. */
    public boolean isComplete() {
        def completedStates = [FINISHED, DEFERRED, CANCELLED]
        return progress in completedStates
    }

    static transients = ['complete']

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
        associatedProgram nullable: true
        associatedSubProgram nullable: true
        projectStage nullable: true
        projectActivityId nullable: true
    }

}
