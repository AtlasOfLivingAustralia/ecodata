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
        switch (event.eventType) {
            case EventType.PostInsert:
            case EventType.PostUpdate:
            case EventType.PostDelete:
                elasticSearchService.queueGormEvent(event)
                auditService.logGormEvent(event)
                break
            case EventType.PreUpdate:
                elasticSearchService.queueGormEvent(event)
                break
        }

//        log.debug "onPersistenceEvent END"
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return true
    }
}
