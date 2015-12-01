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

    public static class StatusChange {
        Date dateChanged
        String changedBy
        String status
        static constraints = {
            version false
        }


    }

    ObjectId id

    String reportId
    String projectId
    String organisationId

    String name
    String description
    String type // What do we want here...? (Stage, Green Army Monthly, Green Army 3 Monthly)

    Date fromDate
    Date toDate
    Date dueDate

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
    /** Number of days before (-ve) or after the due date the report was submitted.  Calculated at submit time to make reporting easier. */
    Integer submissionDeltaInWeekdays
    /** Number of days after a report is submitted that it's approved.  Calculated at approval time to make reporting easier. */
    Integer approvalDeltaInWeekdays

    /** REPORT_NOT_APPROVED, REPORT_SUBMITTED, REPORT_APPROVED */
    String publicationStatus = REPORT_NOT_APPROVED

    /** active, deleted */
    String status = 'active'

    Date dateCreated
    Date lastUpdated

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
        return  publicationStatus == REPORT_SUBMITTED ||
                publicationStatus == REPORT_APPROVED
    }


    public void approve(String userId, Date changeDate = new Date()) {
        if (publicationStatus != REPORT_SUBMITTED) {
            throw new IllegalArgumentException("Only submitted reports can be approved.")
        }
        if (!approvalDeltaInWeekdays) {
            approvalDeltaInWeekdays = weekDaysBetween(dateSubmitted, changeDate)
        }
        StatusChange change = changeStatus(userId, 'approved', changeDate)

        publicationStatus = REPORT_APPROVED
        approvedBy = change.changedBy
        dateApproved = change.dateChanged
    }

    public void submit(String userId,  Date changeDate = new Date()) {
        if (isSubmittedOrApproved()) {
            throw new IllegalArgumentException("An approved or submitted report cannot be resubmitted")
        }
        StatusChange change = changeStatus(userId, 'submitted', changeDate)

        if (dueDate && !submissionDeltaInWeekdays) {
            submissionDeltaInWeekdays = weekDaysBetween(dueDate, changeDate)
        }
        publicationStatus = REPORT_SUBMITTED
        submittedBy = change.changedBy
        dateSubmitted = change.dateChanged
    }

    public void returnForRework(String userId, Date changeDate = new Date()) {
        StatusChange change = changeStatus(userId, 'returned', changeDate)

        publicationStatus = REPORT_NOT_APPROVED
        returnedBy = change.changedBy
        dateReturned = change.dateChanged
    }

    private StatusChange changeStatus(String userId, String status, Date changeDate = new Date()) {
        StatusChange change = new StatusChange(changedBy:userId, dateChanged: changeDate, status: status)
        statusChangeHistory << change

        return change
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
        projectId nullable:true
        dueDate nullable:true
        organisationId nullable:true
        approvalDeltaInWeekdays nullable: true
        submissionDeltaInWeekdays nullable: true

    }

    static embedded = ['statusChangeHistory']
    static mapping = {
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
