package au.org.ala.ecodata

import grails.converters.JSON
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent

class AuditService {

    static transactional = false

    def logGormEvent(AbstractPersistenceEvent event) {
        log.debug("Loggin audit event: ${event.eventType} ObjectType: ${event.entity.javaClass.name} ObjectIdentity: ${event.entity.getPropertyByName("id")}")
        println event.entityObject as JSON
    }

}
