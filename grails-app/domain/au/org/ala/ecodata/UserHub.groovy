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

    UserHub(String hubId) {
        this.hubId = hubId
    }

    static constraints = {
        hubId unique: true
        lastLoginTime nullable: true
    }
}
