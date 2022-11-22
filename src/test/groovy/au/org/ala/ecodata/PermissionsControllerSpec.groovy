package au.org.ala.ecodata

import com.mongodb.MongoExecutionTimeoutException
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

class PermissionsControllerSpec extends Specification implements ControllerUnitTest<PermissionsController>, DataTest {
    PermissionService permissionService = Mock(PermissionService)
    OrganisationService organisationService = Mock(OrganisationService)
    ProjectService projectService = Mock(ProjectService)

    Class[] getDomainClassesToMock() {
        [Program, Hub, Project, Organisation, ManagementUnit, Site, UserPermission]
    }

    def setup() {
        controller.permissionService = permissionService
        controller.organisationService = organisationService
        controller.projectService = projectService
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
        request.JSON = [entity:Hub.name, entityId: hubId, role: AccessLevel.admin.name(), userId: userId]

        when:
        params.userId = userId
        params.hubId = hubId
        params.role = AccessLevel.admin.name()
        request.method = "POST"
        controller.addUserWithRoleToHub()

        then:
        1 * permissionService.addUserAsRoleToHub(request.JSON) >> [status:"ok"]
        response.status == HttpStatus.SC_OK
    }

    void "All parameters must be supplied when assigning a user a hub role"(String userId, String hubId, String role) {
        setup:
        new Hub(hubId:hubId, urlPath:'test').save()

        when:
        params.userId = userId
        params.programId = hubId
        params.role = role
        request.method = "POST"
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

        request.JSON = [entity:Hub.name, entityId: hubId, role: role, userId: userId]

        when:
        request.method = "POST"
        controller.addUserWithRoleToHub()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.addUserAsRoleToHub(request.JSON) >> [status: "ok"]
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
        request.JSON = [entity:Hub.name, entityId: hubId, role: AccessLevel.admin.name(), userId: userId]


        when:
        params.userId = userId
        params.hubId = hubId
        params.role = AccessLevel.admin.name()
        request.method = "POST"
        controller.removeUserWithRoleFromHub()

        then:
        1 * permissionService.removeUserRoleFromHub(request.JSON) >> [status:"ok"]
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
        request.method = "POST"
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
        request.JSON = [entity:Hub.name, entityId: hubId, role: role, userId: userId]

        when:
        params.userId = userId
        params.hubId = hubId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromHub()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.removeUserRoleFromHub(request.JSON) >> [status: "ok"]
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

    void "delete user Permission when userID is Provided"(){
        setup:
        String userId = "1"
        String hubId = "h1"
        Map details = [status: 200, error: false]

        when:
        params.id = userId
        params.hubId = hubId
        request.method = "POST"
        controller.deleteUserPermission()
        def result = response.getJson()


        then:
        1 * permissionService.deleteUserPermissionByUserId(userId, hubId) >>  details

        then:

        result.status == HttpStatus.SC_OK
        result.error == false
    }

    void "The userId must be supplied when calling deleteUserPermission"(){
        setup:
        String hubId = "h1"

        when:
        params.hubId = hubId
        request.method = "POST"
        controller.deleteUserPermission()
        def result = response.getJson()

        then:
        0 * permissionService.deleteUserPermissionByUserId(_,_)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        result.error != null
    }

    void "Index"() {
        when:
        controller.index();
        def result = response.getJson()
        then:
        response.status == HttpStatus.SC_OK
        result.message == "Hello"
    }

    void "Add admin to organisation - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.addAdminToOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Required params not provided: adminId, userId, organisationId"
    }

    void "Add admin to organisation - Invalid organisation"(){
        setup:
        String userId = "1"
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.addAdminToOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Organisation not found for organisationId: 1"
    }

    void "Add admin to organisation - valid organisation"(){
        setup:
        String userId = "1"
        String projectId = '1'
        Organisation org = new Organisation(organisationId: 1, name: "Org1")
        org.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.addAdminToOrganisation()

        then:
        1 * permissionService.addUserAsAdminToProject(userId, projectId) >> [status: "ok", id: projectId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Add user as role to project - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.addUserAsRoleToProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, role, projectId"
    }

    void "Add user as role to project - Invalid project"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project not found for projectId: 1"
    }

    void "Add user as role to project - Invalid role"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = "test"

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToProject()

        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error determining role: No enum constant au.org.ala.ecodata.AccessLevel.test"
    }

    void "Add user as role to project - valid project"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToProject()

        then:
        1 * permissionService.addUserAsRoleToProject(userId, AccessLevel.admin, projectId) >> [status: "ok", id: projectId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Add user as role to project - error"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToProject()

        then:
        1 * permissionService.addUserAsRoleToProject(userId, AccessLevel.admin, projectId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error adding editor: [status:error, error:Error]"
    }

    void "Add user as role to organisation - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.addUserAsRoleToOrganisation()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, role, organisationId"
    }

    void "Add user as role to organisation - Invalid organisation"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = AccessLevel.admin.name()

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Organisation not found for organisationId: 1"
    }

    void "Add user as role to organisation - Invalid role"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = "test"

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToOrganisation()

        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error determining role: No enum constant au.org.ala.ecodata.AccessLevel.test"
    }

    void "Add user as role to organisation - valid organisation"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = AccessLevel.admin.name()
        Organisation org = new Organisation(organisationId: 1, name: "Org1")
        org.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToOrganisation()

        then:
        1 * permissionService.addUserAsRoleToOrganisation(userId, AccessLevel.admin, organisationId) >> [status: "ok", id: organisationId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Add user as role to organisation - error"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = AccessLevel.admin.name()
        Organisation org = new Organisation(organisationId: 1, name: "Org1")
        org.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.addUserAsRoleToOrganisation()

        then:
        1 * permissionService.addUserAsRoleToOrganisation(userId, AccessLevel.admin, organisationId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error adding editor: [status:error, error:Error]"
    }

    void "Remove user role from project - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, role, projectId"
    }

    void "Remove user role from project - Invalid project"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project not found for projectId: 1"
    }

    void "Remove user role from project - Invalid role"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = "test"

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error determining role: No enum constant au.org.ala.ecodata.AccessLevel.test"
    }

    void "Remove user role from project - valid project"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.editor.name()
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        1 * permissionService.removeUserAsRoleToProject(userId, AccessLevel.editor, projectId) >> [status: "ok", id: projectId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Remove user role from project - remove admin role where admin count is greater than 1"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        1 * permissionService.getAllAdminsForProject(projectId) >> [new UserPermission(userId:'1', accessLevel:AccessLevel.admin),
                                                                    new UserPermission(userId:'2', accessLevel:AccessLevel.admin)]
        1 * permissionService.removeUserAsRoleToProject(userId, AccessLevel.admin, projectId) >> [status: "ok", id: projectId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Remove user role from project - remove admin role where admin count is 1"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        1 * permissionService.getAllAdminsForProject(projectId)  >> [new UserPermission(userId:'1', accessLevel:AccessLevel.admin)]
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Cannot remove the last admin for a project"
    }

    void "Remove user role from project - error"(){
        setup:
        String userId = "1"
        String projectId = '1'
        String role = AccessLevel.admin.name()
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromProject()

        then:
        1 * permissionService.removeUserAsRoleToProject(userId, AccessLevel.admin, projectId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error removing user/role: [status:error, error:Error]"
    }

    void "A user can have a role unassigned for a management unit"() {
        setup:
        String managementUnitId = '1'
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()
        String userId = '1'


        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        params.role = AccessLevel.admin.name()
        controller.removeUserWithRoleFromManagementUnit()

        then:
        1 * permissionService.removeUserAsRoleFromManagementUnit(userId, AccessLevel.admin, managementUnitId) >> [status:"ok"]
        response.status == HttpStatus.SC_OK
    }

    void "All parameters must be supplied when unassigning a user from a management unit role"(String userId, String managementUnitId, String role) {
        setup:
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()

        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        params.role = role
        controller.removeUserWithRoleFromManagementUnit()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        where:
        userId | managementUnitId | role
        null   | '1'       | 'admin'
        '1'    | null      | 'admin'
        '1'    | '1'       | null
    }

    void "A valid role must be supplied when unassigning a user from a management unit role"(String role, int result) {
        setup:
        String managementUnitId = '1'
        String userId = '1'
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()


        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        params.role = role
        controller.removeUserWithRoleFromManagementUnit()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.removeUserAsRoleFromManagementUnit(userId, AccessLevel.valueOf(role), managementUnitId) >> [status: "ok"]
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

    void "All parameters must be supplied when assigning a user from a management unit role"(String userId, String managementUnitId, String role) {
        setup:
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()

        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        params.role = role
        controller.addUserWithRoleToManagementUnit()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        where:
        userId | managementUnitId | role
        null   | '1'       | 'admin'
        '1'    | null      | 'admin'
        '1'    | '1'       | null
    }

    void "A valid role must be supplied when assigning a user from a management unit role"(String role, int result) {
        setup:
        String managementUnitId = '1'
        String userId = '1'
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()


        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        params.role = role
        controller.addUserWithRoleToManagementUnit()

        then:
        if (result == HttpStatus.SC_OK) {
            1 * permissionService.addUserAsRoleToManagementUnit(userId, AccessLevel.valueOf(role), managementUnitId) >> [status: "ok"]
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

    void "Get Members Of Management Unit"() {

        setup:
        String managementUnitId = '1'
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()

        when:
        controller.getMembersOfManagementUnit(managementUnitId)

        then:
        1 * permissionService.getMembersOfManagementUnit(managementUnitId, null, null, null, null) >> [status: "ok"]
        response.status == HttpStatus.SC_OK
    }

    void "Remove user role from organisation - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.removeUserWithRoleFromOrganisation()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, role, organisationId"
    }

    void "Remove user role from organisation  - Invalid organisation"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = AccessLevel.admin.name()

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Organisation not found for organisationId: 1"
    }

    void "Remove user role from organisation  - Invalid role"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = "test"

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromOrganisation()

        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error determining role: No enum constant au.org.ala.ecodata.AccessLevel.test"
    }

    void "Remove user role from organisation  - valid organisation"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = AccessLevel.admin.name()
        Organisation org = new Organisation(organisationId: 1, name: "Org1")
        org.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromOrganisation()

        then:
        1 * permissionService.removeUserAsRoleFromOrganisation(userId, AccessLevel.admin, organisationId) >> [status: "ok", id: organisationId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Remove user role from organisation  - error"(){
        setup:
        String userId = "1"
        String organisationId = '1'
        String role = AccessLevel.admin.name()
        Organisation org = new Organisation(organisationId: 1, name: "Org1")
        org.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.organisationId = organisationId
        params.role = role
        request.method = "POST"
        controller.removeUserWithRoleFromOrganisation()

        then:
        1 * permissionService.removeUserAsRoleFromOrganisation(userId, AccessLevel.admin, organisationId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Organisation removing user/role: [status:error, error:Error]"
    }

    void "Add star project for user - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.addStarProjectForUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, projectId."
    }

    void "Add star project for user - Invalid project"(){
        setup:
        String userId = "1"
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.addStarProjectForUser()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project not found for projectId: 1"
    }

    void "Add star project for user  - valid project"(){
        setup:
        String userId = "1"
        String projectId = '1'
        Project p = new Project(projectId: 1, name: "Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.addStarProjectForUser()

        then:
        1 * permissionService.addUserAsRoleToProject(userId, AccessLevel.starred, projectId) >> [status: "ok", id: projectId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Add star project for user  - error"(){
        setup:
        String userId = "1"
        String projectId = '1'
        Project p = new Project(projectId: 1, name: "Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.addStarProjectForUser()

        then:
        1 * permissionService.addUserAsRoleToProject(userId, AccessLevel.starred, projectId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error adding editor: [status:error, error:Error]"
    }

    void "Remove star project for user - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.removeStarProjectForUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, projectId."
    }

    void "Remove star project for user - Invalid project"(){
        setup:
        String userId = "1"
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.removeStarProjectForUser()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project not found for projectId: 1"
    }

    void "Remove star project for user  - valid project"(){
        setup:
        String userId = "1"
        String projectId = '1'
        Project p = new Project(projectId: 1, name: "Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.removeStarProjectForUser()

        then:
        1 * permissionService.removeUserAsRoleToProject(userId, AccessLevel.starred, projectId) >> [status: "ok", id: projectId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 1"
    }

    void "Remove star project for user  - error"(){
        setup:
        String userId = "1"
        String projectId = '1'
        Project p = new Project(projectId: 1, name: "Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.removeStarProjectForUser()

        then:
        1 * permissionService.removeUserAsRoleToProject(userId, AccessLevel.starred, projectId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error removing star: [status:error, error:Error]"
    }

    void "Remove star project for user  - not starred"(){
        setup:
        String userId = "1"
        String projectId = '1'
        Project p = new Project(projectId: 1, name: "Project 1")
        p.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.projectId = projectId
        request.method = "POST"
        controller.removeStarProjectForUser()

        then:
        1 * permissionService.removeUserAsRoleToProject(userId, AccessLevel.starred, projectId) >> null
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project: ${projectId} not starred for userId: ${userId}"
    }

    void "Add star site for user - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.addStarSiteForUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, siteId."
    }

    void "Add site project for user - Invalid site"(){
        setup:
        String userId = "1"
        String siteId = '1'

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.addStarSiteForUser()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Site not found for siteId: 1"
    }

    void "Add star site for user  - valid site"(){
        setup:
        String userId = "1"
        String siteId = '1'
        Site site = new Site(siteId: 1, name: "Site 1")
        site.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.addStarSiteForUser()

        then:
        1 * permissionService.addUserAsRoleToSite(userId, AccessLevel.starred, siteId) >> [status: "ok", id: siteId]
        response.status == HttpStatus.SC_OK
        response.text == '{"id":"1"}'
    }

    void "Add site project for user  - error"(){
        setup:
        String userId = "1"
        String siteId = '1'
        Site site = new Site(siteId: 1, name: "Site 1")
        site.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.addStarSiteForUser()

        then:
        1 * permissionService.addUserAsRoleToSite(userId, AccessLevel.starred, siteId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error adding starred site: [status:error, error:Error]"
    }

    void "Remove star site for user - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.removeStarSiteForUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: userId, projectId."
    }

    void "Remove star site for user - Invalid site"(){
        setup:
        String userId = "1"
        String siteId = '1'

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.removeStarSiteForUser()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project not found for projectId: 1"
    }

    void "Remove star site for user  - valid site"(){
        setup:
        String userId = "1"
        String siteId = '1'
        Site site = new Site(siteId: 1, name: "Site 1")
        site.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.removeStarSiteForUser()

        then:
        1 * permissionService.removeUserAsRoleToSite(userId, AccessLevel.starred, siteId) >> [status: "ok", id: siteId]
        response.status == HttpStatus.SC_OK
        response.text == '{"id":"1"}'
    }

    void "Remove star site for user  - error"(){
        setup:
        String userId = "1"
        String siteId = '1'
        Site site = new Site(siteId: 1, name: "Site 1")
        site.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.removeStarSiteForUser()

        then:
        1 * permissionService.removeUserAsRoleToSite(userId, AccessLevel.starred, siteId) >> [status: 'error', error: 'Error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == "Error removing star: [status:error, error:Error]"
    }

    void "Remove star site for user  - not starred"(){
        setup:
        String userId = "1"
        String siteId = '1'
        Site site = new Site(siteId: 1, name: "Site 1")
        site.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.removeStarSiteForUser()

        then:
        1 * permissionService.removeUserAsRoleToSite(userId, AccessLevel.starred, siteId) >> null
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project: ${siteId} not starred for userId: ${userId}"
    }

    void "Get users for a hub" () {
        setup:
        String hubId = '1'
        new Hub(hubId:hubId, name:'test').save()

        when:
        controller.getByHub(hubId)

        then:
        1 * permissionService.getMembersForHub(hubId) >> [new UserPermission(userId:'1', accessLevel:AccessLevel.admin)]
        response.status == HttpStatus.SC_OK
    }

    void "Get users for an organisation" () {
        setup:
        String organisationId = '1'
        new Organisation(organisationId:organisationId, name:'test').save()

        when:
        controller.getByOrganisation(organisationId)

        then:
        1 * permissionService.getMembersForOrganisation(organisationId) >> [new UserPermission(userId:'1', accessLevel:AccessLevel.admin)]
        response.status == HttpStatus.SC_OK
    }

    void "Get users for a project" () {
        setup:
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        controller.getByProject(projectId)

        then:
        1 * permissionService.getMembersForProject(projectId) >> [new UserPermission(userId:'1', accessLevel:AccessLevel.admin)]
        response.status == HttpStatus.SC_OK
    }

    @Unroll
    void "Get access levels for a given base level"(String baseLevel, int result, int noOfAccessLevels) {
        setup:

        when:
        params.baseLevel = baseLevel
        controller.getAllAccessLevels()

        then:
        response.status == result
        def accessLevel = response.getJson()
        accessLevel.size() == noOfAccessLevels

        where:
        baseLevel                             | result            | noOfAccessLevels
        AccessLevel.admin.name()              | HttpStatus.SC_OK  |  1
        AccessLevel.caseManager.name()        | HttpStatus.SC_OK  |  2
        AccessLevel.moderator.name()          | HttpStatus.SC_OK  |  3
        AccessLevel.editor.name()             | HttpStatus.SC_OK  |  4
        AccessLevel.projectParticipant.name() | HttpStatus.SC_OK  |  5
        AccessLevel.readOnly.name()           | HttpStatus.SC_OK  |  6
        AccessLevel.starred.name()            | HttpStatus.SC_OK  |  7
        //if baseLevel is not set or invalid then returns accesslevel above editor
        "test"                                | HttpStatus.SC_OK  |  4

    }

    void "Clear all permissions - when no permissions" () {
        setup:

        when:
        controller.clearAllPermissionsForAllUsers()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'No UserPermissions found'
    }

    void "Clear all permissions" () {
        setup:
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'1', entityType:Hub.name).save()

        when:
        controller.clearAllPermissionsForAllUsers()

        then:
        response.status == HttpStatus.SC_OK
        response.text == 'OK'
    }

    void "Clear all permissions for userId - when no permissions" () {
        setup:
        String userId = '1'

        when:
        params.id = userId
        controller.clearAllPermissionsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'No UserPermissions found for userId: 1'
    }

    void "Clear all permissions for userId" () {
        setup:
        String userId = '1'
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'1', entityType:Hub.name).save()

        when:
        params.id = userId
        controller.clearAllPermissionsForUserId()

        then:
        response.status == HttpStatus.SC_OK
        response.text == 'OK'
    }

    void "Is User Grant Manager For Organisation - when no mandatory params" () {
        setup:
        String userId = '1'
        String organisationId = '1'

        when:
        params.userId = userId
        controller.isUserGrantManagerForOrganisation()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, organisationId'
    }

    void "Is User Grant Manager For Organisation - invalid organisation" () {
        setup:
        String userId = '1'
        String organisationId = '1'

        when:
        params.userId = userId
        params.organisationId = organisationId
        controller.isUserGrantManagerForOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Organisation not found for organisationId: 1'
    }

    void "Is User Grant Manager For Organisation - valid organisation" () {
        setup:
        String userId = '1'
        String organisationId = '1'
        new Organisation(organisationId:organisationId, name:'test').save()

        when:
        params.userId = userId
        params.organisationId = organisationId
        controller.isUserGrantManagerForOrganisation()
        def result = response.getJson()

        then:
        1 * permissionService.isUserGrantManagerForOrganisation(userId, organisationId) >> false
        response.status == HttpStatus.SC_OK
        result.userIsGrantManager == false
    }

    void "Is User admin For Organisation - when no mandatory params" () {
        setup:
        String userId = '1'
        String organisationId = '1'

        when:
        params.userId = userId
        controller.isUserAdminForOrganisation()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, organisationId'
    }

    void "Is User admin For Organisation - invalid organisation" () {
        setup:
        String userId = '1'
        String organisationId = '1'

        when:
        params.userId = userId
        params.organisationId = organisationId
        controller.isUserAdminForOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Organisation not found for organisationId: 1'
    }

    void "Is User admin For Organisation - valid organisation" () {
        setup:
        String userId = '1'
        String organisationId = '1'
        new Organisation(organisationId:organisationId, name:'test').save()

        when:
        params.userId = userId
        params.organisationId = organisationId
        controller.isUserAdminForOrganisation()
        def result = response.getJson()

        then:
        1 * permissionService.isUserAdminForOrganisation(userId, organisationId) >> false
        response.status == HttpStatus.SC_OK
        result.userIsAdmin == false
    }

    void "Is User admin For project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.isUserAdminForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectId'
    }

    void "Is User admin For project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserAdminForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is User admin For project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserAdminForProject()
        def result = response.getJson()

        then:
        1 * permissionService.isUserAdminForProject(userId, projectId) >> false
        response.status == HttpStatus.SC_OK
        result.userIsAdmin == false
    }

    void "Is User in role For project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'
        String role = AccessLevel.admin.name()

        when:
        params.userId = userId
        controller.isUserInRoleForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectId, role'
    }

    void "Is User in role For project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        String role = AccessLevel.admin.name()

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        controller.isUserInRoleForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is User in role For project - invalid role" () {
        setup:
        String userId = '1'
        String projectId = '1'
        String role = 'test'

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        controller.isUserInRoleForProject()

        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == 'Error determining role: No enum constant au.org.ala.ecodata.AccessLevel.test'
    }

    void "Is User in role For project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        String role = AccessLevel.admin.name()
        new Project(projectId:projectId, name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'1', entityType:Project.name).save()

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = role
        controller.isUserInRoleForProject()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.inRole == true
    }

    void "Is User Participant For project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.isUserParticipantForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectId'
    }

    void "Is User Participant For project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserParticipantForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is User Participant For project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.projectParticipant, entityId:'1', entityType:Project.name).save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserParticipantForProject()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.userIsParticipant == true
    }

    void "Is User editor For project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.isUserEditorForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectId'
    }

    void "Is User editor For project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserEditorForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is User editor For project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.editor, entityId:'1', entityType:Project.name).save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserEditorForProject()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.userIsEditor == true
    }

    void "Is User moderator For project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.isUserModeratorForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectId'
    }

    void "Is User moderator For project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserModeratorForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is User moderator For project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.moderator, entityId:'1', entityType:Project.name).save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserModeratorForProject()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.userIsModerator == true
    }

    void "Is User case manager For project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.isUserCaseManagerForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectId'
    }

    void "Is User case manager  For project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserCaseManagerForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is User case manager  For project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.caseManager, entityId:'1', entityType:Project.name).save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isUserCaseManagerForProject()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.userIsCaseManager == true
    }

    void "can user edit project - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.canUserEditProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: adminId, userId, projectId'
    }

    void "can user edit project - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.canUserEditProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "can user edit project - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.canUserEditProject()
        def result = response.getJson()

        then:
        1 * permissionService.isUserEditorForProject(userId, projectId) >> false
        response.status == HttpStatus.SC_OK
        result.userIsEditor == false
    }

    void "Is project starred by user - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        controller.isProjectStarredByUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: id'
    }

    void "Is project starred by user - invalid project" () {
        setup:
        String userId = '1'
        String projectId = '1'

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isProjectStarredByUser()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "Is project starred by user - valid project" () {
        setup:
        String userId = '1'
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Project.name).save()

        when:
        params.userId = userId
        params.projectId = projectId
        controller.isProjectStarredByUser()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.isProjectStarredByUser == true
    }

    void "Is user editor for projects - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectIds = '1,2'

        when:
        params.userId = userId
        controller.isUserEditorForProjects()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectIds'
    }

    void "Is user editor for projects - valid" () {
        setup:
        String userId = '1'
        String projectIds = '1,2'

        when:
        params.userId = userId
        params.projectIds = projectIds
        controller.isUserEditorForProjects()
        def result = response.getJson()

        then:
        1 * permissionService.isUserEditorForProjects(userId, projectIds) >> true
        response.status == HttpStatus.SC_OK
        result.userIsEditor == true
    }

    void "Is user moderator for projects - when no mandatory params" () {
        setup:
        String userId = '1'
        String projectIds = '1,2'

        when:
        params.userId = userId
        controller.canUserModerateProjects()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, projectIds'
    }

    void "Is user moderator for projects - valid" () {
        setup:
        String userId = '1'
        String projectIds = '1,2'

        when:
        params.userId = userId
        params.projectIds = projectIds
        controller.canUserModerateProjects()
        def result = response.getJson()

        then:
        1 * permissionService.canUserModerateProjects(userId, projectIds) >> true
        response.status == HttpStatus.SC_OK
        result.userCanModerate == true
    }

    void "get starred Site Ids For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getStarredSiteIdsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: id'
    }

    void "get starred Site Ids For UserId - valid" () {
        setup:
        String userId = '1'
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Site.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Site.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Site.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getStarredSiteIdsForUserId()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0] == '1'
    }

    void "get starred project Ids For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getStarredProjectsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: id'
    }

    void "get starred project Ids For UserId - valid" () {
        setup:
        String userId = '1'
        new Project(projectId:'1', name:'test').save()
        new Project(projectId:'2', name:'test').save()
        new Project(projectId:'3', name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Project.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getStarredProjectsForUserId()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0].projectId == '1'
    }

    void "get user roles For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getUserRolesForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId'
    }

    void "get user roles For UserId - valid" () {
        setup:
        String userId = '1'
        new Project(projectId:'1', name:'test').save()
        new Project(projectId:'2', name:'test').save()
        new Project(projectId:'3', name:'test').save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Project.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getUserRolesForUserId()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.roles.size() == 1
        result.roles[0].role == 'admin'
        result.roles[0].entityType == 'au.org.ala.ecodata.Project'
        result.roles[0].entityId == '2'
    }

    void "get organisations For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getOrganisationsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId'
    }

    void "get organisations For UserId - valid" () {
        setup:
        String userId = '1'
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Organisation.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Organisation.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Organisation.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getOrganisationsForUserId()
        def result = response.getJson()

        then:
        0 * organisationService.get('1')
        1 * organisationService.get('2') >> new Organisation(organisationId:'2', name:'test')
        0 * organisationService.get('3')
        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0].accessLevel.name == 'admin'
        result[0].organisation.organisationId == '2'
    }

    void "get organisation For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getOrganisationIdsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId'
    }

    void "get organisation For UserId - valid" () {
        setup:
        String userId = '1'
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Organisation.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Organisation.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Organisation.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getOrganisationIdsForUserId()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0] == '2'
    }

    void "get all projects For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getAllProjectsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId'
    }

    void "get all projects For UserId - valid" () {
        setup:
        String userId = '1'
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Project.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getAllProjectsForUserId()
        def result = response.getJson()

        then:
        1 * projectService.get('1', ProjectService.FLAT) >> [projectId:'1', name:'test']
        1 * projectService.get('2', ProjectService.FLAT) >> [projectId:'2', name:'test']
        0 * projectService.get('3', ProjectService.FLAT)
        response.status == HttpStatus.SC_OK
        result.size() == 2
        result[0].starred == true
        result[0].name == 'test'
        result[0].projectId == '1'
        result[1].starred == null
        result[1].name == 'test'
        result[1].projectId == '2'
    }

    void "get projects For UserId - when no mandatory params" () {
        setup:
        String userId = '1'

        when:
        controller.getProjectsForUserId()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId'
    }

    void "get projects For UserId - valid" () {
        setup:
        String userId = '1'
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Project.name, status: Status.DELETED).save()

        when:
        params.id = userId
        controller.getProjectsForUserId()
        def result = response.getJson()

        then:
        0 * projectService.get('1', ProjectService.FLAT)
        1 * projectService.get('2', ProjectService.FLAT) >> [projectId:'2', name:'test']
        0 * projectService.get('3', ProjectService.FLAT)
        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0].accessLevel.name == 'admin'
        result[0].project.projectId == '2'
    }

    void "get members For Organisation - when no mandatory params" () {
        setup:

        when:
        controller.getMembersForOrganisation()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required parameters not provided: organisationId.'
    }

    void "get members For Organisation - invalid organisation" () {
        setup:
        String organisationId = '1'

        when:
        params.id = organisationId
        controller.getMembersForOrganisation()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Organisation not found for organisationId: 1'
    }

    void "get members For Organisation - valid organisation" () {
        setup:
        String organisationId = '1'
        new Organisation(organisationId:organisationId, name:'test').save()

        when:
        params.id = organisationId
        controller.getMembersForOrganisation()
        def result = response.getJson()

        then:
        1 * permissionService.getMembersForOrganisation(organisationId) >> [[role:'admin', userId:'1', displayName: 'displayName', userName: 'userName']]
        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0].role == 'admin'
        result[0].userId == '1'
        result[0].displayName == 'displayName'
        result[0].userName == 'userName'
    }

    void "get members For project per page - when no mandatory params" () {
        setup:

        when:
        controller.getMembersForProjectPerPage()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.errorMessage == 'Required path not provided: projectId.'
    }

    void "get members For project per page - invalid project" () {
        setup:
        String projectId = '1'

        when:
        params.projectId = projectId
        controller.getMembersForProjectPerPage()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.errorMessage == 'Project not found.'
    }

    void "get members For project per page - valid project" () {
        setup:
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        params.projectId = projectId
        controller.getMembersForProjectPerPage()
        def result = response.getJson()

        then:
        1 * permissionService.getMembersForProjectPerPage(projectId, 0 ,10) >> [totalNbrOfAdmins: 1, data:['1': [userId: '1', role: 'admin'],
                                                                                                         '2' : [userId : '2', role : 'editor']], count:2]
        response.status == HttpStatus.SC_OK
        result.totalNbrOfAdmins == 1
        result.recordsTotal == 2
        result.recordsFiltered == 2
        result.data.size() == 2

    }

    void "get members For project - when no mandatory params" () {
        setup:
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        controller.getMembersForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required path not provided: projectId.'
    }

    void "get members For project - invalid project" () {
        setup:
        String projectId = '1'

        when:
        params.id = projectId
        controller.getMembersForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "get members For project - valid project" () {
        setup:
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        params.id = projectId
        controller.getMembersForProject()

        then:
        1 * permissionService.getMembersForProject(projectId, [AccessLevel.admin, AccessLevel.caseManager, AccessLevel.moderator, AccessLevel.editor, AccessLevel.projectParticipant]) >> ['1': [userId: '1', role: 'admin'], '2' : [userId : '2', role : 'editor']].values().toList()

        response.status == HttpStatus.SC_OK
        response.getJson().size() == 2
    }

    void "get editors For project - when no mandatory params" () {
        setup:
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        controller.getEditorsForProject()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required path not provided: projectId.'
    }

    void "get editors For project - invalid project" () {
        setup:
        String projectId = '1'

        when:
        params.id = projectId
        controller.getEditorsForProject()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'Project not found for projectId: 1'
    }

    void "get editors For project - valid project" () {
        setup:
        String projectId = '1'
        new Project(projectId:projectId, name:'test').save()

        when:
        params.id = projectId
        controller.getEditorsForProject()
        def result = response.getJson()

        then:
        1 * permissionService.getUsersForProject(projectId) >> ['1'].toList()

        response.status == HttpStatus.SC_OK
        result.size() == 1
        result[0] == '1'
    }

    void "is Site Starred By User - not provided mandatory params"(){
        setup:
        String userId = "1"

        when:
        params.userId = userId
        request.method = "POST"
        controller.isSiteStarredByUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "Required params not provided: id"
    }

    void "is Site Starred By User  - Invalid site"(){
        setup:
        String userId = "1"
        String siteId = '1'

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.isSiteStarredByUser()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == "Project not found for projectId: 1"
    }

    void "is Site Starred By User   - valid site"(){
        setup:
        String userId = "1"
        String siteId = '1'
        Site site = new Site(siteId: 1, name: "Site 1")
        site.save(flush:true, failOnError: true)
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Site.name, status: Status.ACTIVE).save()

        when:
        params.userId = userId
        params.siteId = siteId
        request.method = "POST"
        controller.isSiteStarredByUser()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.isSiteStarredByUser == true
    }

    void "get Merit Hub members per page" () {
        setup:
        String hubId = '123'
        String userId = '1'
        new Hub(hubId:hubId, urlPath:'merit').save()

        when:
        params.hubId = hubId
        params.userId = userId
        request.method = "GET"
        controller.getMembersForHubPerPage()
        def result = response.getJson()

        then:
        1 * permissionService.getMembersForHubPerPage(hubId, 0 ,10, userId) >> [totalNbrOfAdmins: 1, data:['1': [userId: '1', role: 'admin'], '2' : [userId : '2', role : 'readOnly']], count:2]
        response.status == HttpStatus.SC_OK
        result.recordsTotal == 2
        result.recordsFiltered == 2
        result.data.size() == 2

    }

    void "get Merit Hub members per page - when no mandatory params" () {
        setup:

        when:
        controller.getMembersForHubPerPage()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.errorMessage == 'Required path not provided: hubId.'
    }

    void "Merit Hub members per page - hub not existing" () {
        setup:
        String hubId = '1'

        when:
        params.hubId = hubId
        controller.getMembersForHubPerPage()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.errorMessage == 'Hub not found.'
    }

    void "checks if the user have existing role on a hub project" () {
        setup:
        String userId = '1'
        String entityId = '12'
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'1', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'2', entityType:Project.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.starred, entityId:'3', entityType:Project.name, status: Status.DELETED).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'4', entityType:Project.name, status: Status.DELETED).save()

        when:
        params.userId = userId
        params.entityId = entityId
        controller.doesUserHaveHubProjects()
        def result = response.getJson()

        then:

        0 * projectService.doesUserHaveHubProjects('1', '11') >> false
        0 * projectService.doesUserHaveHubProjects('3', '13') >> false
        0 * projectService.doesUserHaveHubProjects('4', '14') >> false
        1 * projectService.doesUserHaveHubProjects('1', '12') >> true

        response.status == HttpStatus.SC_OK
        result.doesUserHaveHubProjects == true
    }

    void "This returns the UserPermission" () {
        setup:
        String userId = '1'
        String entityId = '12'
        Date date1 = DateUtil.parse("2022-02-12T00:00:00Z")
        new UserPermission(userId:'2', accessLevel:AccessLevel.starred, entityId:'20', entityType:Hub.name, status: Status.ACTIVE).save()
        new UserPermission(userId:'1', accessLevel:AccessLevel.admin, entityId:'12', entityType:Hub.name, status: Status.ACTIVE, expiryDate: date1).save()


        when:
        params.userId = userId
        params.entityId = entityId
        controller.findUserPermission()


        then:

        1 * permissionService.findUserPermission('1', '12') >> new UserPermission(userId:'1', entityId:'12', entityType:Hub.name)
        response.status == HttpStatus.SC_OK
    }

    void "Add star to a management unit for user"(){
        setup:
        String userId = "123"
        String managementUnitId = '567'
        ManagementUnit mu = new ManagementUnit(managementUnitId: 567, name: "MU test")
        mu.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        request.method = "POST"
        controller.addStarManagementUnitForUser()

        then:
        1 * permissionService.addUserAsRoleToManagementUnit(userId, AccessLevel.starred, managementUnitId) >> [status: "ok", id: managementUnitId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 567"
    }

    void "Add star to a management unit for user but with missing param"(){
        setup:
        String userId = "123"
        String managementUnitId = '567'

        when:

        request.method = "POST"
        controller.addStarManagementUnitForUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, managementUnitId.'
    }

    void "Remove star from a management unit for user"(){
        setup:
        String userId = "123"
        String managementUnitId = '567'
        ManagementUnit mu = new ManagementUnit(managementUnitId: 567, name: "MU test")
        mu.save(flush:true, failOnError: true)

        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        request.method = "POST"
        controller.removeStarManagementUnitForUser()

        then:
        1 * permissionService.removeUserAsRoleFromManagementUnit(userId, AccessLevel.starred, managementUnitId) >> [status: "ok", id: managementUnitId]
        response.status == HttpStatus.SC_OK
        response.text == "success: 567"
    }

    void "Remove star from a management unit for user but with missing param"(){
        setup:
        String userId = "123"
        String managementUnitId = '567'

        when:

        request.method = "POST"
        controller.removeStarManagementUnitForUser()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: userId, managementUnitId.'
    }

    void "Is management unit starred by user" () {
        setup:
        String userId = '123'
        String managementUnitId = '567'
        new ManagementUnit(managementUnitId:managementUnitId, name:'test').save()
        new UserPermission(userId:'123', accessLevel:AccessLevel.starred, entityId:'567', entityType:ManagementUnit.name).save()

        when:
        params.userId = userId
        params.managementUnitId = managementUnitId
        controller.isManagementUnitStarredByUser()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_OK
        result.isManagementUnitStarredByUser == true
    }

    void "Is management unit starred by user but with missing param" () {
        setup:
        String userId = '123'
        String managementUnitId = '567'


        when:
        params.userId = userId

        controller.isManagementUnitStarredByUser()
        def result = response.getJson()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'Required params not provided: id, managementUnitId.'
    }

    void "Is user a member of project"(String userId, String projectId, List roles, int statusCode) {
        setup:
        if (projectId) {
            new Project(projectId:projectId, name:"Project 1").save()
        }

        when:
        params.userId = userId
        params.projectId = projectId
        params.role = roles
        controller.isUserMemberOfProject()

        then:
        response.status == statusCode

        where:
        userId | projectId | roles                  | statusCode
        null   | null      | null                   | 400
        '1'    | null      | ['admin']              | 400
        '1'    | '1'       | ['admin']              | 200
    }
}
