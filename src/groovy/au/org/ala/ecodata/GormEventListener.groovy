package au.org.ala.ecodata

import groovy.util.logging.Log4j
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.engine.event.PostDeleteEvent
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
import org.springframework.context.ApplicationEvent

/**
 * GORM event listener to trigger ElasticSearch updates when domain classes change
 *
 * see http://grails.org/doc/latest/guide/single.html#eventsAutoTimestamping
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Log4j
class GormEventListener extends AbstractPersistenceEventListener {

    ElasticSearchService elasticSearchService
    AuditService auditService

    public GormEventListener(final Datastore datastore, ElasticSearchService serviceClass, AuditService auditService) {
        super(datastore)
        elasticSearchService = serviceClass
        this.auditService = auditService
    }

    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
//        log.debug "onPersistenceEvent START || elasticSearchService = ${elasticSearchService}"

        if (event.eventType == EventType.PostInsert) {
            log.debug "POST INSERT ${event.entityObject}"
            elasticSearchService.indexDocType(event.entityObject)
            auditService.logGormEvent(event)
        } else if (event.eventType == EventType.PostUpdate) {
            log.debug "POST UPDATE "
            elasticSearchService.indexDocType(event.entityObject)
            auditService.logGormEvent(event)
        } else if (event.eventType == EventType.PostDelete) {
            log.debug "POST DELETE ${event.entityObject}"
            elasticSearchService.deleteDocType(event.entityObject)
            auditService.logGormEvent(event)
        }

//        log.debug "onPersistenceEvent END"
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return true
    }
}
