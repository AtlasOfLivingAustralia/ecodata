package au.org.ala.ecodata

import grails.core.GrailsApplication

class UpdateProjectActivityStatsJob {
    GrailsApplication grailsApplication
    ProjectActivityService projectActivityService
    CacheService cacheService

    static triggers = {
        Boolean enabled = grailsApplication.config.getProperty("projectActivity.stats.enabled", Boolean, true)
        if (enabled) {
//            updates every 24 hours
            simple(name: "updatePAStats", startDelay: 60 * 1000, repeatInterval: 24 * 60 * 60 * 1000)
        }
    }

    def execute () {
        ProjectActivity.withNewSession {
            iterateOverProjectActivities()
        }
    }

    def iterateOverProjectActivities() {
        def offset = 0
        List<ProjectActivity> projectActivities = projectActivityService.list(offset)
        while (projectActivities) {
            projectActivities.each {
                def key = ProjectActivityService.PA_STATS_CACHE_KEY_PREFIX + it.projectActivityId
                cacheService.clear(key)
                projectActivityService.addProjectActivityStats([projectActivityId: it.projectActivityId])
            }

            offset += ProjectActivityService.MAX_QUERY_RESULT_SIZE
            projectActivities = projectActivityService.list(offset)
        }
    }
}
