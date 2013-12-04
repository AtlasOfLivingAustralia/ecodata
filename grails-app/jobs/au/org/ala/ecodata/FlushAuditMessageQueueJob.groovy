package au.org.ala.ecodata

class FlushAuditMessageQueueJob {

    def auditService, elasticSearchService

    static triggers = {
        simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute() {

        auditService.flushMessageQueue()
        Project.withNewSession {
            elasticSearchService.flushIndexMessageQueue()
        }
    }
}
