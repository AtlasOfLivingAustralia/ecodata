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
                            "0 0 21 ? * TUE,THU,SAT *")
        }
    }

    def execute() {
        recordService.saveToDiskDarwinCoreArchiveForAllProjects()
    }
}
