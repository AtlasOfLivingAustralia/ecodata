package au.org.ala.ecodata

import com.mongodb.MongoExecutionTimeoutException
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.commons.httpclient.HttpStatus
import spock.lang.Specification
/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(PermissionsController)
@Mock(Program)
class PermissionsControllerSpec extends Specification {
    PermissionService permissionService = Mock(PermissionService)

    def setup() {
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
        String [] projects = ['1']
        permissionService.isUserEditorForProjects('1', projects) >> [:]
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
        String [] ids = ['1']
        String userId = '1'
        permissionService.isUserEditorForProjects(userId,ids) >> {throw new MongoExecutionTimeoutException(123,'Cannot execute query!')}
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
        String [] ids = ['1']
        String userId = '1'
        permissionService.isUserEditorForProjects(userId, ids) >> ['1':true]
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
        println response.text
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
        println response.text
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

}
