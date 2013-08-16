package au.org.ala.ecodata

import grails.converters.JSON
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.EventType

class AuditService {

    def userService

    static transactional = false

    // Do not log GORM events for the AuditMessage class, otherwise we'll recurse off into the sunset...
    private static List<Class> EXCLUDED_OBJECT_TYPES = [ AuditMessage.class ]

    def logGormEvent(AbstractPersistenceEvent event) {

        def entity = event?.entityObject

        if (!entity) {
            return
        }


        if (EXCLUDED_OBJECT_TYPES.contains(entity.class)) {
            return
        }

        def user = userService.getCurrentUserDetails()
        def userId = user?.userId ?: '<anon>'   // if, for some reason, we don't have a user, probably should log anyway
        def auditEventType = getAuditEventTypeFromGormEventType(event.eventType)
        def entityId = IdentifierHelper.getEntityIdentifier(entity)
        def projectId = entity.projectId?.toString()  // not all objects have a direct projectId, so it may be null

        try {
            def message = new AuditMessage(date: new Date(), userId: userId, eventType: auditEventType, entityType: entity.class.name, entityId: entityId, projectId: projectId)
            // TODO: When the MongoDB plugin supports the dynamic isDirty() and/or dirtyProperties() methods, we could
            // optimize what gets stored during an 'update' by only logging the dirty properties.
            // At the moment we log all the properties
            message.entity = entity.properties
            message.save(failOnError: true)
        } catch (Exception ex) {
            log.error("Failed to create audit event message. UserId: ${userId} EventType: ${event.eventType} ObjectType: ${entity.class.name} ObjectIdentity: ${event.entityObject.id}", ex)
        }
    }

    private static AuditEventType getAuditEventTypeFromGormEventType(EventType eventType) {
        AuditEventType auditEventType
        switch (eventType) {
            case EventType.PostInsert:
            case EventType.PreInsert:
                auditEventType = AuditEventType.Insert
                break;
            case EventType.PostDelete:
            case EventType.PreDelete:
                auditEventType = AuditEventType.Delete
                break;
            case EventType.PreUpdate:
            case EventType.PostUpdate:
                auditEventType = AuditEventType.Update
                break;
            default:
                auditEventType = AuditEventType.Unknown
        }

        return auditEventType
    }

}
