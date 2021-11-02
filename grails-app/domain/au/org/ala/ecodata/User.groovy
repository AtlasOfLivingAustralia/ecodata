package au.org.ala.ecodata

import org.bson.types.ObjectId
import org.springframework.validation.Errors


/**
 * The main purpose of this class is to record the most recent login time to various hubs.
 * This information is used to automatically manage access (expire access that is no longer
 * required) and to generate user permission reports on a per hub basis.
 */
class User {

    ObjectId id

    String userId

    String status = 'active'

    static embedded = ['userHubs']
    static hasMany = [userHubs:UserHub]

    static constraints = {
        userId unique: true

        // This ensures each UserHub has a distinct hubId
        userHubs validator: { Set<UserHub> hubs, User user, Errors errors ->
            if (!hubs) {
                return true
            }
            List hubIds = hubs.collect{it.hubId}
            boolean valid = hubIds == hubIds.unique(false)
            if (!valid) {
                errors.rejectValue('userHubs', 'user.userHubs.hubId.unique', "The hubId field must be unique")
            }
            valid
        }
    }

    /** Finds the UserHub with the supplied id */
    UserHub getUserHub(String hubId) {
        userHubs?.find{it.hubId == hubId}
    }

    /**
     * Records a login against a hubId
     * @param hubId the hubId the user has logged into
     * @param loginTime (optional) the time the user logged in (defaults to current time)
     */
    void loginToHub(String hubId, Date loginTime = new Date()) {
        if (!userHubs) {
            userHubs = new HashSet()
        }
        UserHub userHub = getUserHub(hubId)
        if (!userHub) {
            userHub = new UserHub(hubId)
            userHubs << userHub
        }
        userHub.lastLoginTime = loginTime
    }

    /** Helper method to find all Users with an entry for a particular hub */
    static List<User> findAllByLoginHub(String aHubId) {
        User.where {
            userHubs {
                hubId == aHubId
            }
        }.list()
    }
}
