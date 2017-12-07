package au.org.ala.ecodata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

@Domain(Hub)
@TestMixin(MongoDbTestMixin)
@TestFor(HubService)
class HubServiceSpec extends Specification {


    PermissionService permissionService = Mock(PermissionService)
    CommonService commonService = Stub(CommonService)
    Hub hub = new Hub(urlPath:'test', hubId:'id', status:'active')

    void setup() {
        service.permissionService = permissionService
        service.commonService = commonService
        commonService.toBareMap(_) >> {args -> [urlPath:args[0].urlPath, hubId:args[0].hubId, status:args[0].status]}

        Hub.findByUrlPath(hub.urlPath)?.delete(flush:true)
        hub.save(failOnError:true, flush:true)
    }

    void tearDown() {
        hub.delete(failOnError:true)
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
        1 * permissionService.getMembersForHub(hub.hubId) >> userPermissions
        result.urlPath == path
        result.hubId == hub.hubId
        result.userPermissions == userPermissions
    }

}
