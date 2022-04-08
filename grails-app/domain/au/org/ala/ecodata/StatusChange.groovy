package au.org.ala.ecodata

/**
 * Tracks lifecycle events for a Report (submitted/approved/returned)
 */
class StatusChange {

    Date dateChanged
    String changedBy
    String status
    String comment
    String category
    static constraints = {
        comment nullable: true
        category nullable: true
    }
}
