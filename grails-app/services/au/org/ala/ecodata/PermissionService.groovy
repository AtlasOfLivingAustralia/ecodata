package au.org.ala.ecodata

import au.org.ala.web.AuthService
import au.org.ala.web.CASRoles

import static au.org.ala.ecodata.Status.DELETED
/**
 * Service to set and get permissions on projects for each user
 */
class PermissionService {

    static transactional = false
    AuthService authService
    UserService userService // found in ala-auth-plugin

    boolean isUserAlaAdmin(String userId) {
        userId && userService.getRolesForUser(userId)?.contains(CASRoles.ROLE_ADMIN)
    }

    public boolean isUserAdminForProject(String userId, String projectId) {
        def isAdmin = false

        if (userId && projectId) {
            isAdmin = ( UserPermission.findAllByUserIdAndEntityTypeAndEntityIdAndAccessLevel(userId, Project.class.name, projectId, AccessLevel.admin) )
        }

        return isAdmin
    }

    public boolean isUserAdminForOrganisation(String userId, String organisationId) {
        def isAdmin = false

        if (userId && organisationId) {
            isAdmin = ( UserPermission.findAllByUserIdAndEntityTypeAndEntityIdAndAccessLevel(userId, Organisation.class.name, organisationId, AccessLevel.admin) )
        }

        return isAdmin
    }

    def isUserEditorForProject(String userId, String projectId) {
        def isEditor = false

        if (userId && projectId) {
            def ups = getUserAccessForEntity(userId, Project, projectId)
            ups.findAll {
                if (it.accessLevel.code >= AccessLevel.editor.code) {
                    isEditor = true
                }
            }

            log.debug "userCanEdit = ${isEditor}"
        }

        return isEditor // bolean
    }

    def isUserGrantManagerForOrganisation(String userId, String organisationId) {
        def isGrantManager = false
        if (userId && organisationId) {
            def ups = getUserAccessForEntity(userId, Organisation, organisationId)
            ups.findAll {
                if (it.accessLevel.code >= AccessLevel.caseManager.code) {
                    isGrantManager = true
                }
            }
        }

        return isGrantManager
    }

    private def getUserAccessForEntity(String userId, Class entityType, String entityId ) {
        return UserPermission.findAllByUserIdAndEntityTypeAndEntityId(userId, entityType.name, entityId)

    }

    def addUserAsEditorToProject(currentUserId, targetUserId, projectId) {
        if ((isUserAdminForProject(currentUserId, projectId) || authService.userInRole("ROLE_ADMIN")) && targetUserId) {
            addUserAsRoleToProject(targetUserId, AccessLevel.editor, projectId)
        }
    }

    def addUserAsAdminToProject(String userId, String projectId) {
        addUserAsRoleToProject(userId, AccessLevel.admin, projectId)
    }

    def getUsersForProject(String projectId) {
        def up = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevel(projectId, Project.class.name, AccessLevel.editor)
        up.collect { it.userId } // return just a list of userIds
    }

    List<String> getProjectsForUser(String userId, AccessLevel... accessLevels) {
        UserPermission.withCriteria {
            eq "userId", userId
            eq "entityType", Project.class.name
            ne "status", DELETED

            if (accessLevels) {
                'in' "accessLevel", accessLevels
            }

            projections {
                property("entityId")
            }
        }
    }

    def getMembersForProject(String projectId) {
        def up = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevelNotEqual(projectId, Project.class.name, AccessLevel.starred)
        def out = []
        up.each {
            def rec = [:]
            def u = userService.getUserForUserId(it.userId?:"0")
            rec.role = it.accessLevel?.toString()
            rec.userId = it.userId
            rec.displayName = u?.displayName
            rec.userName = u?.userName
            out.add(rec)
        }
        out
    }

    def getMembersForOrganisation(String organisationId) {
        def up = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevelNotEqual(organisationId, Organisation.class.name, AccessLevel.starred)
        def out = []
        up.each {
            def rec = [:]
            def u = userService.getUserForUserId(it.userId?:"0")
            rec.role = it.accessLevel?.toString()
            rec.userId = it.userId
            rec.displayName = u?.displayName
            rec.userName = u?.userName
            out.add(rec)
        }
        out
    }

    private def addUserAsRoleToEntity(String userId, AccessLevel accessLevel, Class entityType, String entityId) {
        def prevRoles = UserPermission.findAllByUserIdAndEntityIdAndEntityTypeAndAccessLevelNotEqual(userId, entityId, entityType.name, AccessLevel.starred)
        log.debug "0. prevRoles = ${prevRoles}"

        def up = new UserPermission(userId: userId, entityId: entityId, entityType:entityType.name, accessLevel: accessLevel)
        try {
            up.save(flush: true, failOnError: true)

        } catch (Exception e) {
            def msg = "Failed to save UserPermission: ${e.message}"
            log.error msg, e
            return [status:'error', error: msg]
        }
        if (accessLevel != AccessLevel.starred) {
            // remove any lower roles
            prevRoles.each {
                log.debug "1. prevRole = ${it}"
                if (it != up) {
                    try {
                        it.delete(flush: true)
                        //return [status:'ok', id: it.id]
                    } catch (Exception e) {
                        def msg = "Failed to delete (previous) UserPermission: ${e.message}"
                        log.error msg, e
                        return [status:'error', error: msg]
                    }
                }

            }
        }
        return [status:'ok', id: up.id]
    }

    def addUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        return addUserAsRoleToEntity(userId, accessLevel, Project, projectId)
    }

    def addUserAsRoleToOrganisation(String userId, AccessLevel accessLevel, String organisationId) {
        return addUserAsRoleToEntity(userId, accessLevel, Organisation, organisationId)
    }

    private def removeUserAsRoleToEntity(String userId, AccessLevel accessLevel, Class entityType, String entityId) {
        def up = UserPermission.findByUserIdAndEntityIdAndEntityTypeAndAccessLevel(userId, entityId, entityType.name, accessLevel)
        if (up) {
            try {
                up.delete(flush: true)
                return [status:'ok', id: up.id]
            } catch (Exception e) {
                def msg = "Failed to delete UserPermission: ${e.message}"
                log.error msg, e
                return [status:'error', error: msg]
            }
        }
        return [status:'ok']
    }

    def removeUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        removeUserAsRoleToEntity(userId, accessLevel, Project, projectId)
    }

    def removeUserAsRoleFromOrganisation(String userId, AccessLevel accessLevel, String organisationId) {
        removeUserAsRoleToEntity(userId, accessLevel, Organisation, organisationId)
    }
    /**
     * Deletes all permissions associated with the supplied project.  Used as a part of a project delete operation.
     *
     * Permissions can be soft deleted (by setting status = deleted), or hard deleted (if the 'destroy' parameter is true)
     *
     * @param projectId The id of the project whose permissions are being deleted. Mandatory.
     * @param destroy True to physically delete the permissions, false to soft delete. Optional - defaults to false.
     */
    void deleteAllForProject(String projectId, boolean destroy = false) {
        UserPermission.findAllByEntityId(projectId).each {
            if (destroy) {
                it.delete()
            } else {
                it.status = 'deleted'
                it.save(flush: true)
            }
        }
    }

    List getAllUserPermissionForEntity(String id, String type, String accessLevel){
        UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevel(id, type, accessLevel);
    }

    List getAllAdminsForProject(String id){
        getAllUserPermissionForEntity(id, Project.class.name, 'admin')
    }
}
