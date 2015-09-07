package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A Report is a container for a set of Activities that must be "Finished" and submitted for approval.
 * Different programmes can require certain types of report as a part of the Programme reporting design, but
 * generally all MERIT programmes will require a stage report, which is a report on works activities during a period of time.
 */
class Report {

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

    /** Not Approved, Submitted, Approved */
    String publicationStatus = 'not approved'

    /** active, deleted */
    String status = 'active'

    Date dateCreated
    Date lastUpdated

    public boolean isCurrent() {
        def now = new Date()
        return  publicationStatus != 'pendingApproval' &&
                publicationStatus != 'published' &&
                fromDate < now && toDate >= now
    }

    public boolean isDue() {
        def now = new Date()
        return  publicationStatus != 'pendingApproval' &&
                publicationStatus != 'published' &&
                toDate < now && dueDate >= now
    }

    public boolean isOverdue() {
        def now = new Date()
        return  publicationStatus != 'pendingApproval' &&
                publicationStatus != 'published' &&
                dueDate < now
    }


    public void approve(String userId, Date changeDate = new Date()) {
        StatusChange change = changeStatus(userId, 'approved', changeDate)

        publicationStatus = 'published'
        approvedBy = change.changedBy
        dateApproved = change.dateChanged
    }

    public void submit(String userId,  Date changeDate = new Date()) {
        StatusChange change = changeStatus(userId, 'submitted', changeDate)

        publicationStatus = 'pendingApproval'
        submittedBy = change.changedBy
        dateSubmitted = change.dateChanged
    }

    public void returnForRework(String userId, Date changeDate = new Date()) {
        StatusChange change = changeStatus(userId, 'returned', changeDate)

        publicationStatus = 'unpublished'
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
        organisationId nullable:true

    }

    static embedded = ['statusChangeHistory']


}
