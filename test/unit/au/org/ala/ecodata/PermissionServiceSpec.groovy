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

    void "A user can be assigned a role for a hub"() {
        setup:
        String userId = 'u1'
        String hubId = 'h1'
        AccessLevel role = AccessLevel.admin

        when:
        service.addUserAsRoleToHub(userId, role, hubId)

        then:
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, AccessLevel.admin, hubId) != null
    }

    void "A user can only be assigned one role for a hub"() {
        setup:
        String userId = '1'
        String hubId = '1'
        new UserPermission(userId:userId, entityId:hubId, entityType:Hub.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)

        when:
        service.addUserAsRoleToHub(userId, AccessLevel.editor, hubId)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, hubId).size() == 1
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, AccessLevel.editor, hubId) != null
    }

    void "A user can have a role unassigned from a hub"() {
        setup:
        String userId = '1'
        String hubId = '1'
        new UserPermission(userId:userId, entityId:hubId, entityType:Hub.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)

        when:
        service.removeUserRoleFromHub(userId, AccessLevel.admin, hubId)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, hubId).size() == 0
    }

    void "A user can be assigned a role for a program"() {
        setup:
        String userId = 'u1'
        String programId = 'p1'
        AccessLevel role = AccessLevel.admin

        when:
        service.addUserAsRoleToProgram(userId, role, programId)
        UserPermission up = UserPermission.findAll()[0]

        then:
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, AccessLevel.admin, programId) != null
    }

    void "A user can only be assigned one role for a program"() {
        setup:
        String userId = '1'
        String programId = '1'
        new UserPermission(userId:userId, entityId:programId, entityType:Program.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)

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

        when:
        service.removeUserAsRoleFromProgram(userId, AccessLevel.admin, programId)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, programId).size() == 0
    }

    void "the list of users with permissions configured on a program can be returned"() {

        setup:
        String programId = 'p1'
        Set<String> userIds = ['1','2','3']
        userIds.each { userId ->
            new UserPermission(entityId:programId, entityType:Program.name, userId: userId, accessLevel:AccessLevel.admin.name()).save(flush:true, failOnError: true)
        }

        when:
        List users = service.getMembersOfProgram(programId).members

        then:
        users.size() == userIds.size()
        new HashSet(users.collect{it.userId}).equals(userIds)
    }

    void "A user with moderator role on a project must get access permission"() {

        setup:
        new UserPermission(entityId:'p1', entityType:Project.name, userId: '1', accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: '1', accessLevel:AccessLevel.editor.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: '2', accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        when:
        Boolean permission = service.canUserModerateProjects(userId, projectIds)

        then:
        permission == expectedReturnValue

        where:
        userId | projectIds | expectedReturnValue
        null   | null       | false
        ''     | ''         | false
        '1'    | 'p1'       | true
        '1'    | 'p1,p2'    | false
        '1'    | 'p1,p2,p3' | false
        '1'    | 'p2'       | false
        '2'    | 'p1'       | false
        '2'    | 'p2'       | true
        '3'    | 'p1'       | false
    }

    void "delete user Permission when userID is Provided "(){

        setup:
        String userId = "1"
        new UserPermission(entityId:'p1', entityType:Project.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p3', entityType:Project.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)


        when:
        def results = service.deleteUserPermissionByUserId(userId)

        then:
        UserPermission.findAllByUserId(userId).size() == 0
        results.status == 200
        !results.error
    }

    void "unable to find user when wrong userID  Provided or no user exist in userPermission database table "(){

        setup:
        String userId = "1"
        new UserPermission(entityId:'p1', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p3', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)


        when:
        def results = service.deleteUserPermissionByUserId(userId)

        then:
        UserPermission.findAllByUserId("2").size() == 3
        results.status == 400
        results.error == "No User Permissions found"
    }
}
