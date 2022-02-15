package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest

class HubServiceSpec extends MongoSpec implements ServiceUnitTest<HubService>, DomainUnitTest<Hub> {


    PermissionService permissionService = Mock(PermissionService)
    CommonService commonService = Stub(CommonService)
    Hub hub = new Hub(urlPath:'test', hubId:'id', status:'active')

    void setup() {
        service.permissionService = permissionService
        service.commonService = commonService
        commonService.toBareMap(_) >> {args -> [urlPath:args[0].urlPath, hubId:args[0].hubId, status:args[0].status]}

        Hub.findAll().each{it.delete(flush:true)}
        hub.save(failOnError:true, flush:true)
    }

    void tearDown() {
        Hub.findAll().each{it.delete(flush:true)}
    }

    void "hubs can be retrieved by their URL path"() {
        setup:
        String path = 'test'
        when:
        Map result = service.findByUrlPath(path)

        then:
        result.urlPath == path
        result.hubId == hub.hubId
    }

    void "hub permissions will be returned when a hub is queried by URL path"() {
        setup:
        String path = 'test'
        List userPermissions = []
        when:
        Map result = service.findByUrlPath(path)

        then:
        1 * permissionService.getMembersForHub(hub.hubId, false) >> userPermissions
        result.urlPath == path
        result.hubId == hub.hubId
        result.userPermissions == userPermissions
    }

    void "Hubs with configuration related to automatic access expiry can be found"() {
        setup:
        new Hub(urlPath:"test1", hubId:"hub1", accessManagementOptions: [expireUsersAfterDurationInactive:"P24M", warnUsersAfterDurationInactive:"P23M"]).save(flush:true, deleteOnerror:true)

        expect:
        service.findHubsEligibleForAccessExpiry().size() == 1
    }

    void "All hubs expect MERIT hub are listed"() {
        setup:
        def result
        service.cacheService = new CacheService()
        new Hub(urlPath:"test1", hubId:"hub1").save(flush:true, deleteOnerror:true)

        when:
        // MERIT hub name is set in application.groovy
        result = service.findBioCollectHubs()

        then:
        result.size() == 2
        result == ["test", "test1"]
    }
}
