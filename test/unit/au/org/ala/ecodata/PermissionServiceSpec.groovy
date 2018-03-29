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

    void "A user can be assigned a role for a program"() {
        setup:
        String userId = 'u1'
        String programId = 'p1'
        AccessLevel role = AccessLevel.admin

        when:
        service.addUserAsRoleToProgram(userId, role, programId)
        UserPermission up = UserPermission.findAll()[0]
        println up.accessLevel.name()+','+up.entityId+','+up.entityType+','+up.userId

        then:
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, AccessLevel.admin, programId) != null
    }

    void "A user can only be assigned one role for a program"() {
        setup:
        String userId = '1'
        String programId = '1'
        new UserPermission(userId:userId, entityId:programId, entityType:Program.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)

        println "All"+UserPermission.findAll()
        when:
        service.addUserAsRoleToProgram(userId, AccessLevel.editor, programId)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, programId).size() == 1
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, AccessLevel.editor, programId) != null
    }

    void "A user can have a role unassigned from a program"() {
        setup:
        String userId = '1'
        String programId = '1'
        new UserPermission(userId:userId, entityId:programId, entityType:Program.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)

        println "All"+UserPermission.findAll()
        when:
        service.removeUserAsRoleFromProgram(userId, AccessLevel.admin, programId)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, programId).size() == 0
    }

}
