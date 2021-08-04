package au.org.ala.ecodata

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants

/**
 * A Report is a container for a set of Activities that must be "Finished" and submitted for approval.
 * Different programmes can require certain types of report as a part of the Programme reporting design, but
 * generally all MERIT programmes will require a stage report, which is a report on works activities during a period of time.
 */
class Report {

    public static final String REPORT_APPROVED = 'published'
    public static final String REPORT_SUBMITTED = 'pendingApproval'
    public static final String REPORT_NOT_APPROVED = 'unpublished'

    /**
     *  An activity report is the one that is applied to all activities performed during the
     *  reporting period.
     */
    public static final String TYPE_ACTIVITY = 'Activity'

    /**
     * An adjustment report is created to amend the values entered into a submitted report without requiring the
     * report to have approvals withdrawn and the report edited.
     */
    public static final String TYPE_ADJUSTMENT = 'Adjustment'

    ObjectId id

    /** UUID for this report */
    String reportId

    /** If this report is for a project, this identifies which project */
    String projectId
    /** If this report is for a organisation, this identifies which organisation */
    String organisationId
    /** If this report is for a program, this identifies which program */
    String programId
    /** If this report is for a management unit, this identifies which program */
    String managementUnitId
    String name
    String description
    String type // "Activity" for stage/activity progress reporting, "Performance", "Administrative" for organisation performance self assessments
    String category // Client classification for reports
    /** Name of the report configuration that generated this report */
    String generatedBy

    /**
     * For reports with an activityType specified, this field holds the id of the activity that contains the data for this report.
     * It is unused for other report types.
     */
    String activityId
    /**
     * For type == REPORT_TYPE_SINGLE this field holds the type of activity that needs to be
     * completed as a requirement for this report.
     * It is unused for other report types.
     */
    String activityType

    Date fromDate
    Date toDate
    Date dueDate
    /* The date from which the report can be submitted */
    Date submissionDate

    String progress // For reports that have data (e.g self assessment)
    Map data // report type specific data for this report.

    /** The number of activities associated with this report. */
    Integer activityCount

    /** Should we record history in this object or rely on the Audit trail? */
    List<StatusChange> statusChangeHistory = []

    /** The Date the report was submitted for approval by a project admin */
    Date dateSubmitted
    /** The user ID of the user who submitted this Report for approval */
    String submittedBy
    /** The Date the report was approved by a grant manager */
    Date dateApproved
    /** The user ID of the grant manager who approved this Report */
    String approvedBy
    /** The Date the report was returned to the admin for rework */
    Date dateReturned
    /** The user ID of the grant manager who returned this Report */
    String returnedBy
    /** The Date the report adjustment was initiated */
    Date dateAdjusted
    /** The user ID of the grant manager who initiated the adjustment for this Report */
    String adjustedBy
    /** Number of days before (-ve) or after the due date the report was submitted.  Calculated at submit time to make reporting easier. */
    Integer submissionDeltaInWeekdays
    /** Number of days after a report is submitted that it's approved.  Calculated at approval time to make reporting easier. */
    Integer approvalDeltaInWeekdays

    /** REPORT_NOT_APPROVED, REPORT_SUBMITTED, REPORT_APPROVED */
    String publicationStatus = REPORT_NOT_APPROVED

    /** Only non-null for reports of type 'Adjustment' - references the id of the Report that the adjustment applies to */
    String adjustedReportId

    /** active, deleted */
    String status = 'active'

    Date dateCreated
    Date lastUpdated

    public Date getSubmissionDate() {
        return submissionDate ?: toDate
    }

    public boolean isCurrent() {
        def now = new Date()
        return  !isSubmittedOrApproved() &&
                fromDate < now && toDate >= now
    }

    public boolean isDue() {
        def now = new Date()
        return  !isSubmittedOrApproved() &&
                toDate < now && (dueDate == null || dueDate >= now)
    }

    public boolean isOverdue() {
        def now = new Date()
        return  !isSubmittedOrApproved() &&
                dueDate && dueDate < now
    }

    public boolean isSubmittedOrApproved() {
        return publicationStatus == REPORT_SUBMITTED ||
                publicationStatus == REPORT_APPROVED
    }

    public boolean isApproved() {
        return publicationStatus == REPORT_APPROVED
    }

    public boolean isAdjusted() {
        return dateAdjusted != null
    }

    public boolean isActivityReport() {
        return type == TYPE_ACTIVITY
    }


    public void approve(String userId, String comment = '' , Date changeDate = new Date()) {
        if (publicationStatus != REPORT_SUBMITTED) {
            throw new IllegalArgumentException("Only submitted reports can be approved.")
        }
        if (!approvalDeltaInWeekdays) {
            approvalDeltaInWeekdays = weekDaysBetween(dateSubmitted, changeDate)
        }
        StatusChange change = changeStatus(userId, 'approved', changeDate, comment)
        markDirty("approvedBy")
        markDirty("dateApproved")
        markDirty("publicationStatus")
        publicationStatus = REPORT_APPROVED
        approvedBy = change.changedBy
        dateApproved = change.dateChanged
    }

    public void submit(String userId, String comment = '', Date changeDate = new Date()) {
        if (isSubmittedOrApproved()) {
            throw new IllegalArgumentException("An approved or submitted report cannot be resubmitted")
        }
        StatusChange change = changeStatus(userId, 'submitted', changeDate, comment)

        if (dueDate && !submissionDeltaInWeekdays) {
            submissionDeltaInWeekdays = weekDaysBetween(dueDate, changeDate)
        }
        markDirty("submittedBy")
        markDirty("dateSubmitted")
        markDirty("publicationStatus")
        publicationStatus = REPORT_SUBMITTED
        submittedBy = change.changedBy
        dateSubmitted = change.dateChanged
    }

    public void returnForRework(String userId, String comment = '', String category = '', Date changeDate = new Date()) {
        StatusChange change = changeStatus(userId, 'returned', changeDate, comment, category)
        markDirty("returnedBy")
        markDirty("dateReturned")
        markDirty("publicationStatus")
        publicationStatus = REPORT_NOT_APPROVED
        returnedBy = change.changedBy
        dateReturned = change.dateChanged
    }

    public void adjust(String userId, String comment, Date changeDate = new Date()) {

        if (!isApproved() || isAdjusted()) {
            throw new IllegalArgumentException("Only approved reports can be adjusted")
        }
        StatusChange change = changeStatus(userId, 'adjusted', changeDate, comment)

        markDirty("adjustedBy")
        markDirty("dateAdjusted")
        markDirty("publicationStatus")

        publicationStatus = REPORT_APPROVED
        adjustedBy = change.changedBy
        dateAdjusted = change.dateChanged
    }

    private StatusChange changeStatus(String userId, String status, Date changeDate = new Date(), String comment = '', String category = '') {
        StatusChange change = new StatusChange(changedBy:userId, dateChanged: changeDate, status: status, comment: comment, category:category)
        statusChangeHistory << change

        return change
    }

    public boolean isSingleActivityReport() {
        return activityType != null
    }

    static transients = ['due', 'overdue', 'current']

    static constraints = {
        reportId index:true
        version:false
        description nullable:true
        dateSubmitted nullable:true
        submittedBy nullable:true
        dateApproved nullable:true
        approvedBy nullable:true
        dateReturned nullable:true
        returnedBy nullable:true
        adjustedBy nullable:true
        dateAdjusted nullable:true
        projectId nullable:true
        dueDate nullable:true
        organisationId nullable:true
        programId nullable:true
        managementUnitId nullable: true
        approvalDeltaInWeekdays nullable: true
        submissionDeltaInWeekdays nullable: true
        activityCount nullable: true
        data nullable: true
        progress nullable: true
        submissionDate nullable: true
        activityId nullable: true
        activityType nullable:true
        type nullable:false
        category nullable:true
        generatedBy nullable:true
        adjustedReportId nullable:true, validator: { value, report ->
            // Adjustment reports must reference another report
            if (report.type == TYPE_ADJUSTMENT) {
                if (value == null) {
                    return 'nullable'
                }
            }
            else {
                if (value != null) {
                    return 'nullable'
                }
            }
        }
    }

    static embedded = ['statusChangeHistory']
    static mapping = {
        reportId index: true
        projectId index: true
        adjustedReportId index: true
        programId index: true

        version false
    }


    static int weekDaysBetween(Date date1, Date date2) {
        DateTime d1
        DateTime d2

        def direction
        if (date1.before(date2)) {
            d1 = new DateTime(date1)
            d2 = new DateTime(date2)
            direction = 1
        }
        else {
            d1 = new DateTime(date2)
            d2 = new DateTime(date1)
            direction = -1
        }
        def daysDifference = 0
        while (d1.isBefore(d2)) {
            d1 = d1.plusDays(1)
            if (d1.getDayOfWeek() != DateTimeConstants.SATURDAY && d1.getDayOfWeek() != DateTimeConstants.SUNDAY) {
                daysDifference++
            }
        }

        return daysDifference*direction
    }

}
