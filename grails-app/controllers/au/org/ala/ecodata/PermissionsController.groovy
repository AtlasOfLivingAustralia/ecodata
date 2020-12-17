package au.org.ala.ecodata

import grails.converters.JSON
import org.springframework.http.HttpStatus

import static au.org.ala.ecodata.Status.*
import static org.apache.http.HttpStatus.*

/**
 * Controller for getting and setting user <-> project
 * ({@link UserPermission}) access permissions.
 *
 * @see au.org.ala.ecodata.UserPermission
 */
class PermissionsController {
    PermissionService permissionService
    ProjectService projectService
    OrganisationService organisationService

    static allowedMethods = [deleteUserPermission:"POST"]
    def index() {
        render([message: "Hello"] as JSON)
    }

    /**
     * @deprecated for generic {@link #addUserAsRoleToProject()}
     * @return
     */
    def addEditorToProject() {
        String adminId = params.adminId
        String userId = params.userId
        String projectId = params.projectId

        if (adminId && userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                Map ps = permissionService.addUserAsEditorToProject(adminId, userId, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status: 500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 404, text: 'Required params not provided: adminId, userId, projectId'
        }

    }

    /**
     * @deprecated for generic {@link #addUserAsRoleToProject()}
     * @return
     */
    def addUserAsAdminToProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                Map ps = permissionService.addUserAsAdminToProject(userId, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status: 500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 404, text: 'Required params not provided: adminId, userId, projectId'
        }
    }

    def addAdminToOrganisation() {
        String userId = params.userId
        String organisationId = params.projectId

        if (userId && organisationId) {
            Organisation organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                Map ps = permissionService.addUserAsAdminToProject(userId, organisationId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status: 500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status: 404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status: 404, text: 'Required params not provided: adminId, userId, organisationId'
        }
    }

    /**
     * Create a {@link UserDetails user}-{@link Project project}-{@link UserPermission role}
     * {@link UserPermission} object
     *
     * @return
     */
    def addUserAsRoleToProject() {
        String userId = params.userId
        String projectId = params.projectId
        String role = params.role

        if (userId && projectId && role) {
            Project project = Project.findByProjectId(projectId)

            try {
                AccessLevel ac = AccessLevel.valueOf(role)

                if (project) {
                    log.debug "addUserAsRoleToProject: ${userId}, ${ac}, ${project}"
                    Map ps = permissionService.addUserAsRoleToProject(userId, ac, projectId)
                    if (ps.status == "ok") {
                        render "success: ${ps.id}"
                    } else {
                        render status: 500, text: "Error adding editor: ${ps}"
                    }
                } else {
                    render status: 404, text: "Project not found for projectId: ${projectId}"
                }
            } catch (Exception e) {
                render status: 500, text: "Error determining role: ${e.message}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, role, projectId'
        }
    }

    /**
     * Create a {@link UserDetails user}-{@link Organisation organisation}-{@link UserPermission role}
     * {@link UserPermission} object
     *
     * @return
     */
    def addUserAsRoleToOrganisation() {
        String userId = params.userId
        String organisationId = params.organisationId
        String role = params.role

        if (userId && organisationId && role) {
            Organisation organisation = Organisation.findByOrganisationId(organisationId)

            try {
                AccessLevel ac = AccessLevel.valueOf(role)

                if (organisation) {
                    log.debug "addUserAsRoleToOrganisation: ${userId}, ${ac}, ${organisationId}"
                    Map ps = permissionService.addUserAsRoleToOrganisation(userId, ac, organisationId)
                    if (ps.status == "ok") {
                        render "success: ${ps.id}"
                    } else {
                        render status: 500, text: "Error adding editor: ${ps}"
                    }
                } else {
                    render status: 404, text: "Organisation not found for organisationId: ${organisationId}"
                }
            } catch (Exception e) {
                render status: 500, text: "Error determining role: ${e.message}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, role, organisationId'
        }
    }

    /**
     * Delete a {@link UserDetails user}-{@link Project project}-{@link UserPermission role}
     * {@link UserPermission} object
     *
     * @return
     */
    def removeUserWithRoleFromProject() {
        String userId = params.userId
        String projectId = params.projectId
        String role = params.role

        if (userId && projectId && role) {
            Project project = Project.findByProjectId(projectId)

            try {
                AccessLevel accessLevel = AccessLevel.valueOf(role)

                if (project) {
                    log.debug "removeUserAsRoleToProject: ${userId}, ${accessLevel}, ${project}"

                    // Make sure the last admin for a project cannot be removed
                    // Note: the UI should not allow this: this check is just a precaution
                    if (accessLevel == AccessLevel.admin && permissionService.getAllAdminsForProject(projectId)?.size() == 1) {
                        render status: 400, text: "Cannot remove the last admin for a project"
                    } else {
                        Map ps = permissionService.removeUserAsRoleToProject(userId, accessLevel, projectId)
                        if (ps.status == "ok") {
                            render "success: ${ps.id}"
                        } else {
                            render status: 500, text: "Error removing user/role: ${ps}"
                        }
                    }
                } else {
                    render status: 404, text: "Project not found for projectId: ${projectId}"
                }
            } catch (Exception e) {
                render status: 500, text: "Error determining role: ${e.message}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, role, projectId'
        }
    }

    def addUserWithRoleToProgram(String userId, String programId, String role) {
        Program program = Program.findByProgramId(programId)
        Closure addToProgram = { String userId2, String role2, String programId2 ->
            permissionService.addUserAsRoleToProgram(userId2, AccessLevel.valueOf(role2), programId2)}
        Map result = validateAndUpdatePermission(program, programId, role, userId, addToProgram)
        render status:result.status, text:result.text
    }

    def removeUserWithRoleFromProgram(String userId, String programId, String role) {
        Program program = Program.findByProgramId(programId)
        Closure removeFromProgram = { String userId2, String role2, String programId2 ->
            permissionService.removeUserAsRoleFromProgram(userId2, AccessLevel.valueOf(role2), programId2)}
        Map result = validateAndUpdatePermission(program, programId, role, userId, removeFromProgram)
        render status:result.status, text:result.text
    }

    def removeUserWithRoleFromManagementUnit(String userId, String managementUnitId, String role) {
        ManagementUnit mu = ManagementUnit.findByManagementUnitId(managementUnitId)
        Closure removeFromManagementUnit = { String userId2, String role2, String managementUnitId2 ->
            permissionService.removeUserAsRoleFromManagementUnit(userId2, AccessLevel.valueOf(role2), managementUnitId2)}
        Map result = validateAndUpdatePermission(mu, managementUnitId, role, userId, removeFromManagementUnit)
        render status:result.status, text:result.text
    }

    def addUserWithRoleToManagementUnit(String userId, String managementUnitId, String role) {
        ManagementUnit mu = ManagementUnit.findByManagementUnitId(managementUnitId)
        Closure addToManagementUnit = { String userId2, String role2, String managementUnitId2 ->
            permissionService.addUserAsRoleToManagementUnit(userId2, AccessLevel.valueOf(role2), managementUnitId2)}
        Map result = validateAndUpdatePermission(mu, managementUnitId, role, userId, addToManagementUnit)
        render status:result.status, text:result.text
    }


    def addUserWithRoleToHub(String userId, String hubId, String role) {
        Hub hub = Hub.findByHubId(hubId)
        Closure addToHub= { String userId2, String role2, String hubId2 ->
            permissionService.addUserAsRoleToHub(userId2, AccessLevel.valueOf(role2), hubId2)}
        Map result = validateAndUpdatePermission(hub, hubId, role, userId, addToHub)
        render status:result.status, text:result.text
    }

    def removeUserWithRoleFromHub(String userId, String hubId, String role) {
        Hub hub = Hub.findByHubId(hubId)
        Closure removeFromProgram = { String userId2, String role2, String hubId2 ->
            permissionService.removeUserRoleFromHub(userId2, AccessLevel.valueOf(role2), hubId2)}
        Map result = validateAndUpdatePermission(hub, hubId, role, userId, removeFromProgram)
        render status:result.status, text:result.text
    }

    /**
     * Returns the users and their roles for a hub.
     * @param id the hubId of the hub.
     * @return
     */
    def getMembersOfProgram(String id) {
        if (!id) {
            render status:400, text:'The id parameter must be supplied'
        }
        Integer max = params.max as Integer
        Integer offset = params.offset as Integer
        String sort = params.sort
        String order = params.order
        render permissionService.getMembersOfProgram(id, max, offset, order, sort) as JSON
    }

    /**
     * Returns the users and their roles for a hub.
     * @param id the hubId of the hub.
     * @return
     */
    def getMembersOfManagementUnit(String id) {
        if (!id) {
            render status:400, text:'The id parameter must be supplied'
        }
        Integer max = params.max as Integer
        Integer offset = params.offset as Integer
        String sort = params.sort
        String order = params.order
        render permissionService.getMembersOfManagementUnit(id, max, offset, order, sort) as JSON
    }



    private Map validateAndUpdatePermission(entity, String entityId, String role, String userId, Closure serviceCall) {
        Map result = validate(entity, entityId, role, userId)

        if (!result) {
            result = serviceCall(userId, role, entityId)
            if (result.status == "ok") {
                result = [status:200 , text:"success: ${result.id}"]
            } else {
                result = [status: 500, text: "Error removing user/role: ${result}"]
            }
        }
        result
    }

    private Map validate(entity, String entityId, String role, String userId) {
        Map result
        if (!entityId || !role || !userId) {
            result = [status:400, text: 'Required params not provided: userId, role, id']
        }
        else if (!entity) {
            result = [status:404, text: 'Not found']
        }
        else {
            try {
                AccessLevel.valueOf(role)
            }
            catch (Exception e) {
                result = [status:400, text: 'Invalid role: '+role]
            }
        }
        result
    }

    /**
     * Delete a {@link UserDetails user}-{@link Organisation project}-{@link UserPermission role}
     * {@link UserPermission} object
     *
     * @return
     */
    def removeUserWithRoleFromOrganisation() {
        String userId = params.userId
        String organisationId = params.organisationId
        String role = params.role

        if (userId && organisationId && role) {
            Organisation organisation = Organisation.findByOrganisationId(organisationId)

            try {
                AccessLevel ac = AccessLevel.valueOf(role)

                if (organisation) {
                    log.debug "removeUserWithRoleFromOrganisation: ${userId}, ${ac}, ${organisation}"
                    def ps = permissionService.removeUserAsRoleFromOrganisation(userId, ac, organisationId)
                    if (ps.status == "ok") {
                        render "success: ${ps.id}"
                    } else {
                        render status: 500, text: "Organisation removing user/role: ${ps}"
                    }
                } else {
                    render status: 404, text: "Organisation not found for organisationId: ${organisationId}"
                }
            } catch (Exception e) {
                render status: 500, text: "Error determining role: ${e.message}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, role, organisationId'
        }
    }

    /**
     * Create a {@link AccessLevel#starred starred} role user-project {@link UserPermission}
     *
     * @return
     */
    def addStarProjectForUser() {
        String projectId = params.projectId
        String userId = params.userId
        AccessLevel role = AccessLevel.starred

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                log.debug "addUserAsRoleToProject: ${userId}, ${role}, ${project}"
                Map ps = permissionService.addUserAsRoleToProject(userId, role, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status: 500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId.'
        }
    }

    /**
     * Delete a {@link AccessLevel#starred starred} role user-project {@link UserPermission}
     *
     * @return
     */
    def removeStarProjectForUser() {
        String projectId = params.projectId
        String userId = params.userId
        AccessLevel role = AccessLevel.starred

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                Map ps = permissionService.removeUserAsRoleToProject(userId, role, projectId)
                if (ps && ps.status == "ok") {
                    render "success: ${ps.id}"
                } else if (ps) {
                    render status: 500, text: "Error removing star: ${ps}"
                } else {
                    render status: 404, text: "Project: ${projectId} not starred for userId: ${userId}"
                }
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId.'
        }
    }


    /**
     * Create a {@link AccessLevel#starred starred} role user-project {@link UserPermission}
     *
     * @return
     */
    def addStarSiteForUser() {
        String siteId = params.siteId
        String userId = params.userId
        AccessLevel role = AccessLevel.starred

        if (userId && siteId) {
            Site site = Site.findBySiteId(siteId)
            if (site) {
                log.debug "addUserAsRoleToSite: ${userId}, ${role}, ${site}"
                Map ps = permissionService.addUserAsRoleToSite(userId, role, siteId)
                if (ps.status == "ok") {
                    def result = [id: "${ps.id}"]
                    render result as JSON
                } else {
                    render status: 500, text: "Error adding starred site: ${ps}"
                }
            } else {
                render status: 404, text: "Site not found for siteId: ${siteId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, siteId.'
        }
    }

    /**
     * Delete a {@link AccessLevel#starred starred} role user-project {@link UserPermission}
     *
     * @return
     */
    def removeStarSiteForUser() {
        String userId = params.userId
        String siteId = params.siteId
        AccessLevel role = AccessLevel.starred

        if (userId && siteId) {
            Site site = Site.findBySiteId(siteId)
            if (site) {
                Map ps = permissionService.removeUserAsRoleToSite(userId, role, siteId)
                if (ps && ps.status == "ok") {
                    def result = [id: "${ps.id}"]
                    render result as JSON
                } else if (ps) {
                    render status: 500, text: "Error removing star: ${ps}"
                } else {
                    render status: 404, text: "Project: ${siteId} not starred for userId: ${userId}"
                }
            } else {
                render status: 404, text: "Project not found for projectId: ${siteId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId.'
        }
    }

    /**
     * Does a given {@link Site site} have {@link AccessLevel#starred starred} level access
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def isSiteStarredByUser() {
        String userId = params.userId
        String siteId = params.siteId

        if (userId && siteId) {
            Site site = Site.findBySiteId(siteId)
            if (site) {
                List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityIdAndEntityTypeAndAccessLevel(userId, siteId, Site.class.name, AccessLevel.starred)
                Map outMap = [isSiteStarredByUser: !permissions.isEmpty()]
                render outMap as JSON
            } else {
                render status: 404, text: "Project not found for projectId: ${siteId}"
            }

        } else {
            render status: 400, text: "Required params not provided: id"
        }
    }


    /**
     * Get a list of users with {@link AccessLevel#editor editor} role access to the given projectId
     *
     * @return
     */
    def getEditorsForProject() {
        String projectId = params.id
        log.debug "projectId = ${projectId}"
        if (projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                List userList = permissionService.getUsersForProject(projectId)
                render userList as JSON
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required path not provided: projectId.'
        }
    }

    /**
     * Get a list of users with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link Project project} (via {@link Project#projectId projectId})
     *
     * @return
     */
    def getMembersForProject() {
        String projectId = params.id

        if (projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                List members = permissionService.getMembersForProject(projectId)
                render members as JSON
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required path not provided: projectId.'
        }
    }

    /**
     * Get project members, support pagination.
     * @return project members one page at a time
     */
    @RequireApiKey
    def getMembersForProjectPerPage() {
        String projectId = params.projectId
        Integer start = params.getInt('offset')?:0
        Integer size = params.getInt('max')?:10

        if (projectId){
            Project project = Project.findByProjectId(projectId)
            if (project) {
                Map results = permissionService.getMembersForProjectPerPage(projectId,start,size)
                render(contentType: 'application/json', text: [ data: results.data, totalNbrOfAdmins: results.totalNbrOfAdmins, recordsTotal: results.count, recordsFiltered: results.count] as JSON)
            } else {
                response.sendError(SC_NOT_FOUND, 'Project not found.')
            }
        } else {
            response.sendError(SC_BAD_REQUEST, 'Required path not provided: projectId.')
        }
    }

    /**
     * Get a list of users with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link Organisation organisation} (via {@link Organisation#organisationId organisationId})
     */
    def getMembersForOrganisation() {
        String organisationId = params.id

        if (organisationId) {
            Organisation organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                List members = permissionService.getMembersForOrganisation(organisationId)
                render members as JSON
            } else {
                render status: 404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status: 400, text: 'Required parameters not provided: organisationId.'
        }
    }

    /**
     * Get a list of {@link Project projects} with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getProjectsForUserId() {
        String userId = params.id
        if (userId) {
            List<UserPermission> up = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqualAndStatusNotEqual(userId, Project.class.name, AccessLevel.starred, DELETED, params)
            List out = []
            up.each {
                Map t = [:]
                log.debug "it.projectId = ${it.entityId}"
                t.project = projectService.get(it.entityId, ProjectService.FLAT)
                t.accessLevel = it.accessLevel
                if (t.project) out.add t
            }
            render out as JSON
        } else {
            render status: 400, text: "Required params not provided: userId"
        }
    }

    /**
     * Get a list of {@link Project projects} with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getAllProjectsForUserId() {
        def userId = params.id
        if (userId) {
            Map projectRetrieved = [:]
            def up = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, Project.class.name, DELETED, params)
            List out  = []
            up.each {
                Map t
                log.debug "it.projectId = ${it.entityId}"
                if(!projectRetrieved[it.entityId]){
                    t = projectService.get(it.entityId, ProjectService.FLAT)
                    if(it.accessLevel == AccessLevel.starred){
                        t.starred = true
                    }

                    if (t) out.add t
                    projectRetrieved[it.entityId] = true
                }
            }
            render out as JSON
        } else {
            render status:400, text: "Required params not provided: userId"
        }
    }
    /**
     * Lightweight version of getOrganisationsForUserId() to return just the organisation ids
     *
     * @return
     */
    def getOrganisationIdsForUserId() {
        String userId = params.id
        if (userId) {
            List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqualAndStatusNotEqual(userId, Organisation.class.name, AccessLevel.starred, DELETED, params)
            List out = permissions.collect { it.entityId }
            render out as JSON
        } else {
            render status: 400, text: "Required params not provided: userId"
        }
    }

    /**
     * Get a list of {@link Organisation organisations} with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getOrganisationsForUserId() {
        String userId = params.id
        if (userId) {
            List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqualAndStatusNotEqual(userId, Organisation.class.name, AccessLevel.starred, DELETED, params)
            List out = []
            permissions.each {
                Map t = [:]
                t.organisation = organisationService.get(it.entityId)
                t.accessLevel = it.accessLevel
                if (t.organisation) out.add t
            }
            render out as JSON
        } else {
            render status: 400, text: "Required params not provided: userId"
        }
    }

    def getUserRolesForUserId() {
        String userId = params.id
        if (userId) {
            List<UserPermission> permissions = UserPermission.findAllByUserIdAndAccessLevelNotEqualAndStatusNotEqual(userId, AccessLevel.starred, DELETED, params)
            Map result = [roles:permissions.collect { [entityId:it.entityId, entityType:it.entityType, role:it.accessLevel.name()] }]

            render result as JSON
        } else {
            render status: 400, text: "Required params not provided: userId"
        }
    }


    /**
     * Get a list of {@link Project projects} with {@link AccessLevel#starred starred} level access
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getStarredProjectsForUserId() {
        String userId = params.id

        if (userId) {
            List<UserPermission> permissions = UserPermission.findAllByUserIdAndAccessLevelAndStatusNotEqual(userId, AccessLevel.starred, DELETED)
            render permissions.collect { Project.findByProjectId(it.entityId) }?.minus(null) as JSON
        } else {
            render status: 400, text: "Required params not provided: id"
        }
    }

    /**
     * Get a list of {@link Site#id} with {@link AccessLevel#starred starred} level access
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getStarredSiteIdsForUserId() {
        String userId = params.id

        if (userId) {
            List<UserPermission> permissions = UserPermission.findAllByUserIdAndAccessLevelAndStatusNotEqual(userId, AccessLevel.starred, DELETED)
            render permissions.collect { it.entityId } as JSON
        } else {
            render status: 400, text: "Required params not provided: id"
        }
    }

    /**
     * Does a given {@link Project project} have {@link AccessLevel#starred starred} level access
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def isProjectStarredByUser() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityIdAndEntityTypeAndAccessLevel(userId, projectId, Project.class.name, AccessLevel.starred)
                Map outMap = [isProjectStarredByUser: !permissions.isEmpty()]
                render outMap as JSON
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }

        } else {
            render status: 400, text: "Required params not provided: id"
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#moderator moderator}
     * level access or higher for a comma separated list of {@link Project projects}.
     *
     * @return JSON object with a single property representing a boolean value
     */
    def canUserModerateProjects() {
        String userId = params.userId
        String projectIds = params.projectIds

        if (userId && projectIds) {
            Map out = [userCanModerate: permissionService.canUserModerateProjects(userId, projectIds)]
            render out as JSON
        } else {
            render status: HttpStatus.BAD_REQUEST, text: 'Required params not provided: userId, projectIds'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#editor editor}
     * level access or higher for a list of {@link Project projects}.
     *
     * @params projectIds - comma separated list of projects
     * @params userId
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserEditorForProjects() {
        String userId = params.userId
        String projectIds = params.projectIds

        if (userId && projectIds) {
            Map out = [userIsEditor: permissionService.isUserEditorForProjects(userId, projectIds)]
            render out as JSON
        } else {
            render status: HttpStatus.BAD_REQUEST, text: 'Required params not provided: userId, projectIds'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#editor editor}
     * level access or higher for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def canUserEditProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                Map out = [userIsEditor: permissionService.isUserEditorForProject(userId, projectId)]
                render out as JSON
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: adminId, userId, projectId'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#editor editor}
     * level access or higher for a given list of {@link Project project}
     *
     * @return JSON object with projectId as key and edit permission as value
     */
    def canUserEditProjects() {
        String userId = params.userId
        List projectIds = params.projectIds?.split(',')

        if (projectIds?.size()) {
            try{
                Map out =  permissionService.isUserAdminForProjects(userId, projectIds)
                render out as JSON
            } catch (Exception e){
                log.error(e.message);
                log.error(e.stackTrace);
                render status: 500, text: 'Internal server error'
            }
        } else {
            render status: 400, text: 'Required params not provided: projectIds'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#caseManager caseManager}
     * level access for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserCaseManagerForProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                UserPermission permission = UserPermission.findByUserIdAndEntityIdAndAccessLevel(userId, projectId, AccessLevel.caseManager)
                render([userIsCaseManager: permission != null] as JSON)
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#moderator moderator}
     * level access for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserModeratorForProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                UserPermission permission = UserPermission.findByUserIdAndEntityIdAndAccessLevel(userId, projectId, AccessLevel.moderator)
                render([userIsModerator: permission != null] as JSON)
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#caseManager caseManager}
     * level access for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserEditorForProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                UserPermission permission = UserPermission.findByUserIdAndEntityIdAndAccessLevel(userId, projectId, AccessLevel.editor)
                render([userIsEditor: permission != null] as JSON)
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#caseManager caseManager}
     * level access for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserParticipantForProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                UserPermission permission = UserPermission.findByUserIdAndEntityIdAndAccessLevel(userId, projectId, AccessLevel.projectParticipant)
                render([userIsParticipant: permission != null] as JSON)
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have specified level access for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserInRoleForProject() {
        String userId = params.userId
        String projectId = params.projectId

        try {
            AccessLevel accessLevel = params.role

            if (userId && projectId && accessLevel) {
                Project project = Project.findByProjectId(projectId)
                if (project) {
                    UserPermission permission = UserPermission.findByUserIdAndEntityIdAndAccessLevel(userId, projectId, accessLevel)
                    render([inRole: permission != null] as JSON)
                } else {
                    render status: 404, text: "Project not found for projectId: ${projectId}"
                }
            } else {
                render status: 400, text: 'Required params not provided: userId, projectId, role'
            }
        } catch (IllegalArgumentException e) {
            render status: 500, text: "Error determining role: ${e.message}"
        }
    }

    /**
     * Does a given {@link UserDetails#userId userId} have {@link AccessLevel#admin admin} level access
     * for a given {@link Project project}
     *
     * @return
     */
    def isUserAdminForProject() {
        String userId = params.userId
        String projectId = params.projectId

        if (userId && projectId) {
            Project project = Project.findByProjectId(projectId)
            if (project) {
                render([userIsAdmin: permissionService.isUserAdminForProject(userId, projectId)] as JSON)
            } else {
                render status: 404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, projectId'
        }
    }

    def isUserAdminForOrganisation() {
        String userId = params.userId
        String organisationId = params.organisationId

        if (userId && organisationId) {
            Organisation organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                render([userIsAdmin: permissionService.isUserAdminForOrganisation(userId, organisationId)] as JSON)
            } else {
                render status: 404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, organisationId'
        }
    }

    def isUserGrantManagerForOrganisation() {
        String userId = params.userId
        String organisationId = params.organisationId

        if (userId && organisationId) {
            Organisation organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                render ([userIsGrantManager: permissionService.isUserGrantManagerForOrganisation(userId, organisationId)] as JSON)
            } else {
                render status: 404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status: 400, text: 'Required params not provided: userId, organisationId'
        }
    }

    /**
     * Admin function to clear all UserPermissions entries for the
     * specified user.
     *
     * @return
     */
    def clearAllPermissionsForUserId() {
        String userId = params.id // REST style URL (no params)
        List<UserPermission> permissions = UserPermission.findAllByUserId(userId)
        if (permissions.size() > 0) {
            permissions.each {
                log.debug "it = ${it}"
                try {
                    it.delete(flush: true)
                    //return [status:'ok', id: it.id]
                } catch (Exception e) {
                    String msg = "Failed to delete UserPermission: ${e.message}"
                    log.error msg, e
                    render status: 500, text: msg
                }
            }
            render text: "OK"
        } else {
            render status: 400, text: "No UserPermissions found for userId: ${userId}"
        }
    }

    /**
     * Admin function to clear all UserPermissions entries for
     * ALL users in the system.
     *
     * @return
     */
    def clearAllPermissionsForAllUsers() {
        List<UserPermission> permissions = UserPermission.list()
        if (permissions.size() > 0) {
            permissions.each {
                try {
                    it.delete(flush: true)
                } catch (Exception e) {
                    String msg = "Failed to delete UserPermission: ${e.message}"
                    log.error msg, e
                    render status: 500, text: msg
                }
            }
            render text: "OK"
        } else {
            render status: 400, text: "No UserPermissions found"
        }
    }

    /**
     * Return a list of the {@link AccessLevel} enum values for the given baseLevel and higher
     * if baseLevel is not set or invalid then returns accesslevel above editor
     * See the custom JSON serializer in Bootstrap.groovy
     *
     * @return JSON representation of AccessLevel values
     */
    def getAllAccessLevels() {
        AccessLevel accessLevel = AccessLevel.find { it.name() == params.baseLevel } ?: AccessLevel.editor
        render AccessLevel.values().findAll { it.code >= accessLevel.code } as JSON
    }
    /**
     * Get user permissions for the given projectId
     *
     * @param id project identifier.
     */
    @PreAuthorise(idType="projectId")
    def getByProject(String id) {
        response.setContentType('application/json; charset="UTF-8"')
        render permissionService.getMembersForProject(id) as JSON
    }

    /**
     * Get user permissions for the given organisationId
     *
     * @param id organisation identifier.
     */
    @PreAuthorise(idType="organisationId")
    def getByOrganisation(String id) {
        response.setContentType('application/json; charset="UTF-8"')
        render permissionService.getMembersForOrganisation(id) as JSON
    }

    /**
     * Returns the users and their roles for a hub.
     * @param id the hubId of the hub.
     * @return
     */
    def getByHub(String id) {
        if (!id) {
            render status:400, text:'The id parameter must be supplied'
        }
        render permissionService.getMembersForHub(id) as JSON
    }

    /**
     * Admin function to delete all UserPermissions entries for the specific userId for merit user
     * @return
     */
    def deleteUserPermission(){
        String userId = params.id
        Map results = permissionService.deleteUserPermissionByUserId(userId)
        render results as JSON
    }
}
