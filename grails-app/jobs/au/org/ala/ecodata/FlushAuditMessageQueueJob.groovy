package au.org.ala.ecodata

import grails.util.Holders

class FlushAuditMessageQueueJob {

    def auditService, elasticSearchService

    static triggers = {
        long repeatInterval = Holders.config.getProperty("audit.thread.schedule.interval", Long, 5000l)
        simple repeatInterval: repeatInterval // execute job once in 5 seconds
    }

    def execute() {

        // This method has internal session management so doesn't need to be wrapped like the call to the
        // elasticSearchService below.
        auditService.flushMessageQueue()
        // Because this is run from a Quartz thread, it needs a session explicitly setup otherwise the mongo
        // connection is not released leading to eventual thread pool starvation.
        Project.withNewSession {
            elasticSearchService.flushIndexMessageQueue()
        }
    }
}
