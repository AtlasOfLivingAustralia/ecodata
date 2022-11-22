package au.org.ala.ecodata.job


import au.org.ala.ecodata.CacheService
import au.org.ala.ecodata.ProjectActivity
import au.org.ala.ecodata.ProjectActivityService
import au.org.ala.ecodata.UpdateProjectActivityStatsJob
import grails.test.mongodb.MongoSpec
import org.grails.testing.GrailsUnitTest

class UpdateProjectActivityStatsJobSpec extends MongoSpec implements GrailsUnitTest {
    UpdateProjectActivityStatsJob job = new UpdateProjectActivityStatsJob()

    def setup () {
        job.grailsApplication = grailsApplication
        job.projectActivityService = new TestProjectActivityService()
        job.projectActivityService.cacheService = job.cacheService = new CacheService()
    }

    def "should load project activity stats to cache" () {
        def pa = new ProjectActivity(projectActivityId: 'abc')
        def payload = [
                publicAccess: true,
                activityLastUpdated: new Date(1665986923281),
                activityCount: 1,
                speciesRecorded: 1
        ]
        when:
        job.iterateOverProjectActivities()
        def result = job.cacheService.cache[ProjectActivityService.PA_STATS_CACHE_KEY_PREFIX+pa.projectActivityId].resp

        then:
        result == payload
    }
}

class TestProjectActivityService extends ProjectActivityService {
    @Override
    boolean isProjectActivityDataPublic (Map projectActivity) {
        return true
    }

    @Override
    Date getLastUpdatedActivityForProjectActivity(String pActivityId) {
        new Date(1665986923281)
    }

    @Override
    int getActivityCountForProjectActivity(String pActivityId) {
        1
    }

    @Override
    int getSpeciesRecordedForProjectActivity(String pActivityId) {
        1
    }

    @Override
    List<ProjectActivity> list (int offset = 0, int max = MAX_QUERY_RESULT_SIZE) {
        if (!offset)
            [ new ProjectActivity(projectActivityId: 'abc') ]
        else
            []
    }
}
