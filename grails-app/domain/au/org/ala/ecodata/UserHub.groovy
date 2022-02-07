package au.org.ala.ecodata

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Records any Hub specific settings/information about/for a User.
 * Importantly for the automatic access removal feature, it also records the most recent
 * time a user logged into a hub
 *
 * This object is embedded in the User collection and hence has no id field.
 */
@EqualsAndHashCode
@ToString
class UserHub {

    static belongsTo = [user : User]

    String hubId

    /** the most recent login time to the hub */
    Date lastLoginTime

    /**
     * Records the Date the user was lest sent a warning that their access is due to expire.
     * This is used to prevent users being sent more than one warning
     */
    Date inactiveAccessWarningSentDate

    /**
     * Records the Date the user had their access removed due to inactivity.
     * This is used to prevent this user being processed in future jobs.
     */
    Date accessExpiredDate

    /**
     * Records the Date the use was last sent a warning that their permission is expiring 1 month from now
     * This is used to prevent users being sent more than one warning
     */
    Date permissionWarningSentDate

    UserHub(String hubId) {
        this.hubId = hubId
    }

    /** Returns true if the user has been sent a warning about their access being due to expire */
    boolean sentAccessRemovalDueToInactivityWarning() {
        inactiveAccessWarningSentDate && (!lastLoginTime || (inactiveAccessWarningSentDate > lastLoginTime))
    }

    /** Returns true if the user has had their access expired */
    boolean accessExpired() {
        accessExpiredDate && (!lastLoginTime || (accessExpiredDate > lastLoginTime))
    }

    static constraints = {
        hubId unique: true
        lastLoginTime nullable: true
        inactiveAccessWarningSentDate nullable: true
        accessExpiredDate nullable: true
        permissionWarningSentDate nullable: true
    }
}
