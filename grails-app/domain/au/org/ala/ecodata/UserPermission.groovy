package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * Domain class to store permissions settings on a User/Project
 * level.
 * @see AccessLevel
 */
class UserPermission {
    ObjectId id
    String userId
    String projectId
    AccessLevel accessLevel

    static constraints = {
        userId(unique: ['accessLevel', 'projectId']) // prevent duplicate entries
    }
}
