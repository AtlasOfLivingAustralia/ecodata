package au.org.ala.ecodata

import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value


@Slf4j
class FlushAuditMessageQueueJob {

    AuditService auditService
    ElasticSearchService elasticSearchService

    @Value('${audit.thread.max.messages:100}')
    int maxMessagesToFlush

    static triggers = {
        long repeatInterval = Holders.config.getProperty("audit.thread.schedule.interval", Long, 5000l)
        simple repeatInterval: repeatInterval // execute job once in 5 seconds
    }

    @CompileStatic
    def execute() {

        if (log.isDebugEnabled()) {
            log.debug("Flushing message queues at: "+DateUtil.formatWithMilliseconds(new Date()))
        }
        // This method has internal session management so doesn't need to be wrapped like the call to the
        // elasticSearchService below.
        auditService.flushMessageQueue(maxMessagesToFlush)
        // Because this is run from a Quartz thread, it needs a session explicitly setup otherwise the mongo
        // connection is not released leading to eventual thread pool starvation.
        Project.withNewSession {
            elasticSearchService.flushIndexMessageQueue()
        }
    }
}
