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
    }

    ObjectId id

    String reportId

    String name
    String description

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

    public void approve(String userId) {
        StatusChange change = changeStatus(userId, 'approved')

        publicationStatus = 'published'
        approvedBy = change.changedBy
        dateApproved = change.dateChanged
    }

    public void submit(String userId) {
        StatusChange change = changeStatus(userId, 'submitted')

        publicationStatus = 'pendingApproval'
        submittedBy = change.changedBy
        dateSubmitted = change.dateChanged
    }

    public void returnForRework(String userId) {
        StatusChange change = changeStatus(userId, 'returned')

        publicationStatus = 'not published'
        returnedBy = change.changedBy
        dateReturned = change.dateChanged
    }

    private StatusChange changeStatus(String userId, String status) {
        Date now = new Date()
        StatusChange change = new StatusChange(changedBy:userId, dateChanged: now, status: publicationStatus)
        statusChangeHistory << change

        return change
    }

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

    }

    static embedded = ['statusChangeHistory']


}
