package au.org.ala.ecodata

import com.mongodb.MongoExecutionTimeoutException
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class PermissionsControllerSpec extends Specification implements ControllerUnitTest<PermissionsController>, DataTest {
    PermissionService permissionService = Mock(PermissionService)

    Class[] getDomainClassesToMock() {
        [Program, Hub]
    }

    def setup() {
      //  mockDomain Program
     //   mockDomain Hub
        controller.permissionService = permissionService
    }

    def cleanup() {
    }

    void "canUserEditProjects: when not all parameters are passed"() {
        when:
        params.userId = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    void "canUserEditProjects: when nothing is returned"() {
        given:
        List projects = ['1']
        permissionService.isUserAdminForProjects('1', projects) >> [:]
        when:
        params.userId = '1'
        params.projectIds = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_OK
        response.json.size() == 0
    }

    void "canUserEditProjects: when mongo throws exception"() {
        given:
        List ids = ['1']
        String userId = '1'
        permissionService.isUserAdminForProjects(userId,ids) >> {throw new MongoExecutionTimeoutException(123,'Cannot execute query!')}
        when:
        params.userId = userId
        params.projectIds = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text.contains('Internal server error')
    }

    void "canUserEditProjects: when working perfectly"() {
        given:
        List ids = ['1']
        String userId = '1'
        permissionService.isUserAdminForProjects(userId, ids) >> ['1':true]
        when:
        params.userId = userId
        params.projectIds = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_OK
        response.json['1'] == true
    }

    void "A user can be assigned a role for a program"() {
        setup:
        String programId = '1'
        new Program(programId:programId, name:'test').save()
        String userId = '1'


        when:
        params.userId = userId
        params.programId = programId
        params.role = AccessLevel.admin.name()
        controller.addUserWithRoleToProgram()

        then:
        1 * permissionService.addUserAsRoleToProgram(userId, AccessLevel.admin, programId) >> [status:"ok"]
        response.status == HttpStatus.SC_OK
    }

    void "All parameters must be supplied when assigning a user a program role"(String userId, String programId, String role) {
        setup:
        new Program(programId:programId, name:'test').save()

        when:
        params.userId = userId
        params.programId = programId
        params.role = role
        controller.addUserWithRoleToProgram()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        where:
        userId | programId | role
        null   | '1'       | 'admin'
        '1'    | null      | 'admin'
        '1'    | '1'       | null
    }

    void "A valid role must be supplied when assigning a user a program role"(String role, int result) {
        setup:
        String programId = '1'
        String userId = '1'
        new Program(programId:programId, name:'test').save()


        when:
        params.userId = userId
        params.programId = programId
        params.role = role
        controller.addUserWithRoleToProgram()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.addUserAsRoleToProgram(userId, AccessLevel.valueOf(role), programId) >> [status: "ok"]
        }
        response.status == result

        where:
        role                                  | result
        AccessLevel.admin.name()              | HttpStatus.SC_OK
        AccessLevel.caseManager.name()        | HttpStatus.SC_OK
        AccessLevel.editor.name()             | HttpStatus.SC_OK
        AccessLevel.projectParticipant.name() | HttpStatus.SC_OK
        "test"                                | HttpStatus.SC_BAD_REQUEST


    }

    void "A user can have a role unassigned for a program"() {
        setup:
        String programId = '1'
        new Program(programId:programId, name:'test').save()
        String userId = '1'


        when:
        params.userId = userId
        params.programId = programId
        params.role = AccessLevel.admin.name()
        controller.removeUserWithRoleFromProgram()

        then:
        1 * permissionService.removeUserAsRoleFromProgram(userId, AccessLevel.admin, programId) >> [status:"ok"]
        response.status == HttpStatus.SC_OK
    }

    void "All parameters must be supplied when unassigning a user from a program role"(String userId, String programId, String role) {
        setup:
        new Program(programId:programId, name:'test').save()

        when:
        params.userId = userId
        params.programId = programId
        params.role = role
        controller.removeUserWithRoleFromProgram()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        where:
        userId | programId | role
        null   | '1'       | 'admin'
        '1'    | null      | 'admin'
        '1'    | '1'       | null
    }

    void "the members of a program can be returned"() {
        setup:
        String programId = 'p1'

        when:
        params.id = programId
        controller.getMembersOfProgram()

        then:
        1 * permissionService.getMembersOfProgram(programId, null, null, null, null) >> [status: "ok"]
    }

    void "the member of programs can be returned in batches"() {
        setup:
        String programId = 'p1'
        Integer max = 10
        Integer offset = 0
        String sort = "userId"
        String order = "desc"

        when:
        params.id = programId
        params.max = max
        params.offset = offset
        params.sort = sort
        params.order = order
        controller.getMembersOfProgram()

        then:
        1 * permissionService.getMembersOfProgram(programId, max, offset, order, sort) >> [status: "ok"]
    }

    void "A valid role must be supplied when unassigning a user from a program role"(String role, int result) {
        setup:
        String programId = '1'
        String userId = '1'
        new Program(programId:programId, name:'test').save()


        when:
        params.userId = userId
        params.programId = programId
        params.role = role
        controller.removeUserWithRoleFromProgram()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.removeUserAsRoleFromProgram(userId, AccessLevel.valueOf(role), programId) >> [status: "ok"]
        }
        response.status == result

        where:
        role                                  | result
        AccessLevel.admin.name()              | HttpStatus.SC_OK
        AccessLevel.caseManager.name()        | HttpStatus.SC_OK
        AccessLevel.editor.name()             | HttpStatus.SC_OK
        AccessLevel.projectParticipant.name() | HttpStatus.SC_OK
        "test"                                | HttpStatus.SC_BAD_REQUEST


    }

    void "A user can be assigned a role for a hub"() {
        setup:
        String hubId = '1'
        new Hub(hubId:hubId, urlPath:'test').save()
        String userId = '1'

        when:
        params.userId = userId
        params.hubId = hubId
        params.role = AccessLevel.admin.name()
        controller.addUserWithRoleToHub()

        then:
        1 * permissionService.addUserAsRoleToHub(userId, AccessLevel.admin, hubId) >> [status:"ok"]
        response.status == HttpStatus.SC_OK
    }

    void "All parameters must be supplied when assigning a user a hub role"(String userId, String hubId, String role) {
        setup:
        new Hub(hubId:hubId, urlPath:'test').save()

        when:
        params.userId = userId
        params.programId = hubId
        params.role = role
        controller.addUserWithRoleToHub()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        where:
        userId | hubId | role
        null   | '1'       | 'admin'
        '1'    | null      | 'admin'
        '1'    | '1'       | null
    }

    void "A valid role must be supplied when assigning a user a hub role"(String role, int result) {
        setup:
        String hubId = '1'
        String userId = '1'
        new Hub(hubId:hubId, urlPath:'test').save()


        when:
        params.userId = userId
        params.hubId = hubId
        params.role = role
        controller.addUserWithRoleToHub()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.addUserAsRoleToHub(userId, AccessLevel.valueOf(role), hubId) >> [status: "ok"]
        }
        response.status == result

        where:
        role                                  | result
        AccessLevel.admin.name()              | HttpStatus.SC_OK
        AccessLevel.caseManager.name()        | HttpStatus.SC_OK
        AccessLevel.editor.name()             | HttpStatus.SC_OK
        AccessLevel.projectParticipant.name() | HttpStatus.SC_OK
        "test"                                | HttpStatus.SC_BAD_REQUEST


    }

    void "A user can have a role unassigned from a hub"() {
        setup:
        String hubId = '1'
        new Hub(hubId:hubId, urlPath:'test', skin:'configurableHubTemplate1').save()
        String userId = '1'


        when:
        params.userId = userId
        params.hubId = hubId
        params.role = AccessLevel.admin.name()
        controller.removeUserWithRoleFromHub()

        then:
        1 * permissionService.removeUserRoleFromHub(userId, AccessLevel.admin, hubId) >> [status:"ok"]
        println response.text
        response.status == HttpStatus.SC_OK
    }

    void "All parameters must be supplied when unassigning a user from a hub role"(String userId, String hubId, String role) {
        setup:
        new Hub(hubId:hubId, urlPath:'test').save()

        when:
        params.userId = userId
        params.hubId = hubId
        params.role = role
        controller.removeUserWithRoleFromHub()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        where:
        userId | hubId | role
        null   | '1'       | 'admin'
        '1'    | null      | 'admin'
        '1'    | '1'       | null
    }

    void "A valid role must be supplied when unassigning a user from a hub role"(String role, int result) {
        setup:
        String hubId = '1'
        String userId = '1'
        new Hub(hubId:hubId, urlPath:'test').save()


        when:
        params.userId = userId
        params.hubId = hubId
        params.role = role
        controller.removeUserWithRoleFromHub()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.removeUserRoleFromHub(userId, AccessLevel.valueOf(role), hubId) >> [status: "ok"]
        }
        response.status == result

        where:
        role                                  | result
        AccessLevel.admin.name()              | HttpStatus.SC_OK
        AccessLevel.caseManager.name()        | HttpStatus.SC_OK
        AccessLevel.editor.name()             | HttpStatus.SC_OK
        AccessLevel.projectParticipant.name() | HttpStatus.SC_OK
        "test"                                | HttpStatus.SC_BAD_REQUEST


    }

}
