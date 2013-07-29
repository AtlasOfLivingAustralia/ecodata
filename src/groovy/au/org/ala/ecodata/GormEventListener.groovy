package au.org.ala.ecodata

import groovy.util.logging.Log4j
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.springframework.context.ApplicationEvent
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA



import javax.persistence.PostUpdate

/**
 * GORM event listener to trigger ElasticSearch updates when domain classes change
 *
 * see http://grails.org/doc/latest/guide/single.html#eventsAutoTimestamping
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Log4j
class GormEventListener extends AbstractPersistenceEventListener {

    public GormEventListener(final Datastore datastore) {
        super(datastore)
    }
    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
        def ctx = SCH.servletContext.getAttribute(GA.APPLICATION_CONTEXT)

        ElasticSearchService elasticSearchService = ctx.elasticSearchService

        switch(event.eventType) {
            case org.grails.datastore.mapping.engine.event.EventType.PostInsert:
                log.debug "POST INSERT ${event.entityObject}"
                elasticSearchService.indexDocType(event.entityObject)
                break
            case org.grails.datastore.mapping.engine.event.EventType.PostUpdate:
                log.debug "POST UPDATE ${event.entityObject}"
                elasticSearchService.indexDocType(event.entityObject)
                break;
            case org.grails.datastore.mapping.engine.event.EventType.PostDelete:
                log.debug "POST DELETE ${event.entityObject}"
                elasticSearchService.indexDocType(event.entityObject)
                break;
        }
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return true
    }
}
