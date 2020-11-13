package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.ACTIVE

import org.bson.types.ObjectId

/**
 * Domain class to store permissions settings on a User/Project
 * level.
 * @see AccessLevel
 */
class UserPermission {
    ObjectId id
    String userId
    String entityId
    AccessLevel accessLevel
    String entityType
    String status = ACTIVE
    List<String> permissions = []

    static constraints = {
        userId(unique: ['accessLevel', 'entityId']) // prevent duplicate entries
        status nullable: true
    }

    static mapping = {
        userId index: true
        entityId index: true
        entityType index: true
        status index: true
        accessLevel index: true
        version false
    }

    boolean hasPermission(String permission) {
        boolean hasPermission = false
        if (permissions) {
            hasPermission = permissions.contains(permission)
        }
        else {
            // fallback to role definitions
            hasPermission = accessLevel.includes(permission)
        }
        hasPermission
    }
}
