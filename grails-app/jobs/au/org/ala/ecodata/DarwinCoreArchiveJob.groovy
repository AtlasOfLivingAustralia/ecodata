package au.org.ala.ecodata

import grails.core.GrailsApplication

class DarwinCoreArchiveJob {

    GrailsApplication grailsApplication
    RecordService recordService

    static triggers = {
        Boolean enabled = grailsApplication.config.getProperty("export.darwinCoreArchive.enabled", Boolean, true)
        if (enabled) {
            cron name: "Darwin Core Archive: At 9 PM on Tuesday, Thursday and Saturday",
                    cronExpression: grailsApplication.config.getProperty("export.darwinCoreArchive.cronSchedule",
                            "0 0 22 ? * TUE,THU,SAT *")
        }
    }

    def execute() {
        // set the document host URL prefix so that the correct URLs are generated for documents in the Darwin Core Archive.
        String biocollectURL = grailsApplication.config.getProperty("biocollect.baseURL")
        try {
            DocumentHostInterceptor.documentHostUrlPrefix.set(biocollectURL)
            recordService.saveToDiskDarwinCoreArchiveForAllProjects()
        } finally {
            DocumentHostInterceptor.documentHostUrlPrefix.remove()
        }
    }
}
