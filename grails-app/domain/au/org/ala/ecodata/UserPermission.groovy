package au.org.ala.ecodata

import org.bson.types.ObjectId

class UserPermission {
    ObjectId id
    String userId
    AccessLevel accessLevel
    Project project

    static constraints = {
        userId(unique: ['accessLevel', 'project']) // prevent duplicate entries
    }
}
