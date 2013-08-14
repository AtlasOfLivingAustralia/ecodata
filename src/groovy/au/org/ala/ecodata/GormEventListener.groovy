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
    //CommonService commonService

    public GormEventListener(final Datastore datastore, serviceClass) {
        super(datastore)
        elasticSearchService = serviceClass
    }

    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
        log.debug "onPersistenceEvent START || elasticSearchService = ${elasticSearchService}"

        if (event.eventType == EventType.PostInsert) {
            log.debug "POST INSERT ${event.entityObject}"
            elasticSearchService.indexDocType(event.entityObject)
            //commonService.dummyMethod(null, true)
        } else if (event.eventType == EventType.PostUpdate) {
            log.debug "POST UPDATE "
            //commonService.dummyMethod(null, true)
            elasticSearchService.indexDocType(event.entityObject)
        } else if (vent.eventType == EventType.PostDelete) {
            log.debug "POST DELETE ${event.entityObject}"
            elasticSearchService.indexDocType(event.entityObject)
        }

        log.debug "onPersistenceEvent END"
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return (eventType in  [PostInsertEvent, PostUpdateEvent, PostDeleteEvent])
    }
}
