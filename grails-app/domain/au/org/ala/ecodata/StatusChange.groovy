package au.org.ala.ecodata

/**
 * Tracks lifecycle events for a Report and Project/MERI plan (submitted/approved/returned)
 */
class StatusChange {

    static final String APPROVED = "approved"
    static final String SUBMITTED = "submitted"
    static final String RETURNED = "returned"
    static final String CANCELLED = "cancelled"
    static final String ADJUSTED = "adjusted"

    Date dateChanged
    String changedBy
    String status
    String comment
    String reference
    List categories

    /** MERI plan changes are stored in a document */
    String documentId

    static constraints = {
        comment nullable: true
        categories nullable: true
        reference nullable: true
        documentId nullable: true
    }

    Map toMap() {
        [dateChanged: DateUtil.format(dateChanged), changedBy: changedBy, status: status, comment: comment, reference: reference, categories: categories, documentId: documentId]
    }
}
