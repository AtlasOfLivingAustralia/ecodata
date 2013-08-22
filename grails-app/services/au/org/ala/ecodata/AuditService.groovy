package au.org.ala.ecodata

import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.mongo.MongoSession

import java.util.concurrent.ConcurrentLinkedQueue

class AuditService {

    def userService

    static transactional = false

    // AuditMessages are queued so that they can be persisted asynchronously. The reasons for doing this are twofold:
    // 1. It avoids the problems caused by creating/saving domain objects during the flush phase of the session (i.e. when the GORM
    //    event listener is being triggered (recursive flushing manifesting as duplicate id errors)
    // 2. It lowers the overhead of logging the audit message on a request thread
    private static Queue<AuditMessage> _messageQueue = new ConcurrentLinkedQueue<AuditMessage>()

    // Do not log GORM events for the AuditMessage class, otherwise we'll recurse off into the sunset...
    private static List<Class> EXCLUDED_OBJECT_TYPES = [ AuditMessage.class ]

    // If any particular properties are not to be logged, their names should be added to this list
    private static List<String> EXCLUDED_ENTITY_PROPERTIES = []

    /**
     * Logs a GORM event Audit Message to the persistant store. Audit Messages contain information about insert, updates or deletes of
     * domain objects in the system. Any changes made to a domain object, therefore, should be traceable through the collection of AuditEvents tied
     * to that object via its unique id.
     *
     * This method is called by the GormEventListener interface on a request thread.
     *
     * @param event This GORM supplied object contains key information about individual updates,inserts or deletes.
     */
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

            def props = [:]
            // Exclude any properties that should not be logged
            // dbo is a magical bag of all the properties as far as Mongo is concerned, including dynamically added ones
            def map = entity.dbo ?: entity.properties
            map.keySet().each { key ->
                if (!EXCLUDED_ENTITY_PROPERTIES.contains(key)) {
                    props[key] = map[key]
                }
            }
            message.entity = props

            // push the audit message onto the queue
            _messageQueue.offer(message)

        } catch (Exception ex) {
            log.error("Failed to create audit event message. UserId: ${userId} EventType: ${event.eventType} ObjectType: ${entity.class.name} ObjectIdentity: ${event.entityObject.id}", ex)
        }
    }

    /**
     * This method polls the Audit Message queue, and attempts to persist any messages to the database.
     * If the queue exceeds the value of 'maxMessagesToFlush', then only that number of messages will be saved, thus
     * preventing the loop from spinning endlessly.
     *
     * Note: This is important as the a new sessions is created outside of the polling loop, and is only flused
     * once either the queue is empty, or the max number of messages has been flushed.
     *
     * This method is called on a background thread scheduled by the Quartz job scheduler
     */
    public int flushMessageQueue(int maxMessagesToFlush = 1000) {
        int messageCount = 0
        AuditMessage.withNewSession { MongoSession session ->
            try {
                AuditMessage message = null;
                while (messageCount < maxMessagesToFlush && (message = _messageQueue.poll()) != null) {
                    // need to attach the message object to the GORM (Mongo) session
                    session.attach(message)
                    message.save(failOnError: true)
                    messageCount++
                }
                session.flush()
            } catch (Exception ex) {
                log.error(ex)
            }
            return messageCount
        }
    }

    /**
     * Converts a GORM Event Type into an AuditEventType
     *
     * @param eventType
     * @return
     */
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
