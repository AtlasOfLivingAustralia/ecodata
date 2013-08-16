package au.org.ala.ecodata

import org.bson.types.ObjectId

class AuditMessage {

    ObjectId id
    Date date
    String userId
    AuditEventType eventType
    String entityType
    String projectId
    String entityId
    Map entity

    static constraints = {
        projectId nullable: true
        entityId nullable: true
    }

}
