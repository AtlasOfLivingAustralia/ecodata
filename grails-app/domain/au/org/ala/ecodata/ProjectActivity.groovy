package au.org.ala.ecodata

import org.bson.types.ObjectId

class ProjectActivity {

    ObjectId id
    String projectActivityId
    String projectId
    String name
    String description
    String status
    String pActivityFormName
    boolean commentsAllowed
    Date startDate
    Date endDate
    Map alerts
    List sites = [] // list of sites associated to the survey
    boolean restrictRecordToSites
    boolean allowAdditionalSurveySites
    String baseLayersName
    boolean publicAccess // only editors/admins can add data to a project activity unless publicAccess = true
    VisibilityConstraint visibility = new VisibilityConstraint(embargoOption: EmbargoOption.NONE)
    List<SubmissionRecord> submissionRecords

    static embedded = ['visibility']

    static hasMany = [submissionRecords: SubmissionRecord]

    static constraints = {
        endDate nullable: true
        pActivityFormName nullable: true
        alerts nullable: true
        sites nullable: true
        restrictRecordToSites nullable: true
        allowAdditionalSurveySites nullable: true
        baseLayersName nullable: true
        publicAccess nullable: true
        visibility nullable: true
        submissionRecords nullable: true
    }

    static mapping = {
        projectActivityId index: true
    }
}
