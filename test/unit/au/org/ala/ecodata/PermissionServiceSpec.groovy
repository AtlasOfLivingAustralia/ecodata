package au.org.ala.ecodata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

@Domain(UserPermission)
@TestMixin(MongoDbTestMixin)
@TestFor(PermissionService)
class PermissionServiceSpec extends Specification {

    UserService userService = Stub(UserService)

    void setup() {
        UserPermission.findAll().each{it.delete(flush:true)}
        service.userService = userService
        userService.getUserForUserId(_) >> { String userId -> [userId:userId, displayName:"a user"]}
    }

    void tearDown() {
        UserPermission.findAll().each{it.delete(flush:true)}
    }


    void "the list of users with permissions configured on a hub can be returned"() {

        setup:
        String hubId = 'hub'
        Set<String> userIds = ['1','2','3']
        userIds.each { userId ->
            new UserPermission(entityId:hubId, entityType:Hub.class.name, userId: userId, accessLevel:AccessLevel.admin.name()).save(flush:true, failOnError: true)
        }

        when:
        List users = service.getMembersForHub(hubId)


        then:
        users.size() == userIds.size()
        new HashSet(users.collect{it.userId}).equals(userIds)


    }

}
