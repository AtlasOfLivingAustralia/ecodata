package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.ACTIVE

import org.bson.types.ObjectId

class Comment {

    ObjectId id
    String text
    String entityType
    String entityId
    String userId
    Comment parent
    String status = ACTIVE
    Date dateCreated
    Date lastUpdated

    static hasMany = [children: Comment]

    static constraints = {
        parent nullable: true
        status nullable: true
    }

    static mapping = {
        children cascade: "all-delete-orphan"
    }
}