package au.org.ala.ecodata

import au.org.ala.web.AuthService
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest

class PermissionServiceSpec extends MongoSpec implements ServiceUnitTest<PermissionService> {

    UserService userService = Stub(UserService)
    AuthService authService = Mock(AuthService)

    void setup() {
        cleanupData()
        service.userService = userService
        userService.getUserForUserId(_) >> { String userId -> [userId:userId, displayName:"a user"]}
        service.authService = authService
    }

    void tearDown() {
        cleanupData()
    }

    private void cleanupData() {
        UserPermission.findAll().each{it.delete(flush:true)}
        Project.findAll().each {it.delete(flush: true)}
        ManagementUnit.findAll().each { it.delete(flush:true)}
        Program.findAll().each { it.delete(flush:true)}
        Organisation.findAll().each {it.delete(flush:true)}
    }


    void "the list of users with permissions configured on a hub can be returned"() {

        setup:
        String hubId = 'hub'
        Set<String> userIds = ['1','2','3']
        userIds.each { userId ->
            new UserPermission(entityId:hubId, entityType:Hub.class.name, userId: userId, accessLevel:AccessLevel.admin.name()).save(flush:true, failOnError: true)
        }

        when:
        List users = service.getMembersForHub(hubId, false)

        then:
        users.size() == userIds.size()
        new HashSet(users.collect{it.userId}).equals(userIds)

        and:"The userService isn't used to lookup the user name"
        0 * userService._

    }

    void "A user can be assigned a role for a hub"() {
        setup:
        String userId = 'u1'
        String hubId = 'h1'
        AccessLevel role = AccessLevel.admin
        Map param = [userId:userId, role: 'admin', hubId: hubId, entityId: '1']

        when:
        service.addUserAsRoleToHub(param)

        then:
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, 'admin', '1') != null
    }

    void "A user can only be assigned one role for a hub"() {
        setup:
        String userId = '1'
        String hubId = '1'
        new UserPermission(userId:userId, entityId:hubId, entityType:Hub.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)
        Map param = [userId:userId, role: 'admin', hubId: hubId]

        when:
        service.addUserAsRoleToHub(param)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, hubId).size() == 1
        UserPermission.findByUserIdAndAccessLevelAndEntityId(userId, 'admin', hubId) != null
    }

    void "A user can have a role unassigned from a hub"() {
        setup:
        String userId = '1'
        String hubId = '1'
        String role = 'admin'
        new UserPermission(userId:userId, entityId:hubId, entityType:Hub.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)
        Map params = [userId:userId, role:role, hubId:hubId, entityId:hubId]

        when:
        service.removeUserRoleFromHub(params)

        then:
        UserPermission.findAllByUserIdAndEntityId(userId, '1').size() == 0
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

    void "delete user Permission when userID is Provided entity Type is organisation"(){

        setup:
        String userId = "1"
        String hubId = "h1"
        new UserPermission(entityId:'org1', entityType:Organisation.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new Organisation(organisationId: "org1", hubId:hubId, name:"test organisation").save(flush: true, failOnError: true)

        when:
        def results = service.deleteUserPermissionByUserId(userId, hubId)

        then:
        UserPermission.findAllByUserId(userId).size() == 0
        results.status == 200
        !results.error
    }

    def "Organisation permissions need to be removed if the organisation is running any MERIT projects"() {
        setup: "A biocollect organisation with 2 merit projects and 1 biocollect project"
        String userId = "1"
        String hubId = "h1"
        new UserPermission(entityId:'p1', entityType:Project.name, userId: userId, accessLevel:AccessLevel.editor.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: userId, accessLevel:AccessLevel.editor.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p3', entityType:Project.name, userId: userId, accessLevel:AccessLevel.editor.name()).save(flush:true, failOnError: true)

        new Organisation(organisationId: "org1", hubId:"hub2", name:"test organisation").save(flush: true, failOnError: true)
        new Project(projectId:"p1", name:"p1", hubId:hubId, organisationId:"org1").save(flush:true, failOnError: true)
        new Project(projectId:"p2", name:"p2", hubId:hubId, orgIdSvcProvider: "org1").save(flush:true, failOnError: true)
        new Project(projectId:"p3", name:"p3", hubId:"hub2", organisationId: "org1").save(flush:true, failOnError: true)

        when: "We delete the MERIT user permissions"
        def results = service.deleteUserPermissionByUserId(userId, hubId)

        then: "Then the organisation permission and both MERIT project permissions are removed, but the biocollect project reamins"
        UserPermission.findAllByUserId(userId).size() == 1
        results.status == 200
        !results.error
    }

    void "delete user Permission when userID is Provided entity Type is Project"(){

        setup:
        String userId = "1"
        String hubId = 'h1'
        new UserPermission(entityId:'p1', entityType:Project.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new Project(projectId: "p1", name:"test", hubId: hubId).save(flush: true, failOnError: true)

        when:
        def results = service.deleteUserPermissionByUserId(userId, hubId)

        then:
        UserPermission.findAllByUserId(userId).size() == 0
        results.status == 200
        !results.error
    }

    void "delete user Permission when userID is Provided entity Type is Program"(){

        setup:
        String userId = "1"
        String hubId = 'h1'
        new UserPermission(entityId:'p1', entityType:Program.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new Program(programId: "p1", name:"test program", hubId: hubId).save(flush: true, failOnError: true)

        when:
        def results = service.deleteUserPermissionByUserId(userId, hubId)

        then:
        UserPermission.findAllByUserId(userId).size() == 0
        results.status == 200
        !results.error
    }

    void "delete user Permission when userID is Provided entity Type is Management Unit"(){

        setup:
        String userId = "1"
        String hubId = 'h1'
        new UserPermission(entityId:'m1', entityType:ManagementUnit.name, userId: userId, accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new ManagementUnit(managementUnitId: "m1", name:"test mu", hubId:hubId).save(flush: true, failOnError: true)

        when:
        def results = service.deleteUserPermissionByUserId(userId, hubId)

        then:
        UserPermission.findAllByUserId(userId).size() == 0
        results.status == 200
        !results.error
    }

    void "unable to find user when wrong userID  Provided or no user exist in userPermission database table "(){

        setup:
        String userId = "1"
        String hubId = 'h1'
        new UserPermission(entityId:'p1', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p3', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)


        when:
        def results = service.deleteUserPermissionByUserId(userId, hubId)

        then:
        UserPermission.findAllByUserId("2").size() == 3
        results.status == 400
        results.error == "No User Permissions found"
    }

    void "The PermissionService can return all expired ACL entries"() {
        setup:
        Date date1 = DateUtil.parse("2021-11-01T00:00:00Z")
        Date date2 = DateUtil.parse("2022-11-01T00:00:00Z")
        new UserPermission(entityId:'p1', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name(), expiryDate: date1).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p2', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name()).save(flush:true, failOnError: true)
        new UserPermission(entityId:'p3', entityType:Project.name, userId: "2", accessLevel:AccessLevel.moderator.name(), expiryDate: date2).save(flush:true, failOnError: true)

        expect:
        service.findPermissionsByExpiryDate(date1).size() == 1
        service.findPermissionsByExpiryDate(date2).size() == 2
        service.findPermissionsByExpiryDate(DateUtil.parse("2021-12-01T00:00:00Z"))



    }

    void "return the list of Merit Hub users"() {

        setup:
        def roles = [AccessLevel.admin, AccessLevel.caseManager, AccessLevel.readOnly];
        Integer offset = 0
        Integer max = 10
        String hubId = 'merit123'
        String userId = '1'
        List userIds = ['1']
        new UserPermission(entityId:hubId, entityType:Hub.name, userId: userId, accessLevel:AccessLevel.admin.name()).save(flush:true, failOnError: true)


        when:
        def resp = service.getMembersForHubPerPage(hubId, offset, max, userId, roles)

        then:
        1 * authService.getUserDetailsById(userIds) >> []
        resp.count == 1
    }

    void "The owning hub for a permission can be identified"() {
        setup:
        new Project(projectId:'p1', name:"Project 1", hubId:'h1').save(flush:true, failOnError:true)
        new ManagementUnit(managementUnitId:'m1', name:"MU 1", hubId:'h1').save(flush:true, failOnError:true)
        new Program(programId:'prg1', name:"Program 1", hubId:'h1').save(flush:true, failOnError:true)
        new Organisation(organisationId:'o1', name:"Organisation 1", hubId:'h1').save(flush:true, failOnError:true)
        UserPermission projectPermission = new UserPermission(entityType:Project.class.name, entityId:"p1", userId:"u1", accessLevel: AccessLevel.admin)
        UserPermission muPermission = new UserPermission(entityType:ManagementUnit.class.name, entityId:"m1", userId:"u1", accessLevel: AccessLevel.admin)
        UserPermission orgPermission = new UserPermission(entityType:Organisation.class.name, entityId:"o1", userId:"u1", accessLevel: AccessLevel.admin)
        UserPermission programPermission = new UserPermission(entityType:Program.class.name, entityId:"prg1", userId:"u1", accessLevel: AccessLevel.admin)

        expect:
        service.findOwningHubId(projectPermission) == 'h1'
        service.findOwningHubId(muPermission) == 'h1'
        service.findOwningHubId(orgPermission) == 'h1'
        service.findOwningHubId(programPermission) == 'h1'

    }
}
