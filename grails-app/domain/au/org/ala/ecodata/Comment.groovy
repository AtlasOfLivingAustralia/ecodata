package au.org.ala.ecodata

import org.bson.types.ObjectId

class Comment {

    ObjectId id
    String text
    String entityType
    String entityId
    String userId
    Comment parent
    Date dateCreated
    Date lastUpdated

    static hasMany = [children: Comment]

    static constraints = {
        parent nullable: true
    }

    static mapping = {
        children cascade: "all-delete-orphan"
    }
}