package au.org.ala.ecodata

import grails.core.GrailsApplication

class ImportSciStarterJob {

    GrailsApplication grailsApplication
    ProjectService projectService

    static triggers = {
        Boolean enabled = grailsApplication.config.getProperty("sciStarter.importEnabled", Boolean, true)
        if (enabled) {
            cron name: "every sunday", cronExpression: "0 0 * * 0"
        }
    }

    def execute() {
        projectService.importProjectsFromSciStarter()
    }
}
