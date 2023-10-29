package au.org.ala.ecodata
import groovy.util.logging.Slf4j
import grails.core.GrailsApplication

@Slf4j
class UpdateProjectActivityStatsJob {
    GrailsApplication grailsApplication
    ProjectActivityService projectActivityService
    CacheService cacheService

    static triggers = {
        Boolean enabled = grailsApplication.config.getProperty("projectActivity.stats.enabled", Boolean, true)
        if (enabled) {
//            updates at 11PM
            cron name: "11PM", cronExpression: "0 0 23 * * ? *"
        }
    }

    def execute () {
        log.info("Starting cron job - project activity statistics update")
        ProjectActivity.withNewSession {
            iterateOverProjectActivities()
        }
        log.info("Finished cron job - project activity statistics update")
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
