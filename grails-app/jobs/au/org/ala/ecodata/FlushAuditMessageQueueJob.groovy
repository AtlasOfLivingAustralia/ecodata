package au.org.ala.ecodata

class FlushAuditMessageQueueJob {

    def auditService, elasticSearchService

    static triggers = {
        simple repeatInterval: 5000l // execute job once in 5 seconds
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
