package au.org.ala.ecodata

/**
 * Service to set and get permissions on projects for each user
 */
class PermissionService {

    static transactional = false
    def authService, userService // found in ala-web-theme plugin

    public boolean isUserAdminForProject(String userId, String projectId) {
        def isAdmin = false

        if (userId && projectId) {
            isAdmin = ( UserPermission.findAllByUserIdAndProjectIdAndAccessLevel(userId, projectId, AccessLevel.admin) )
            log.debug "isAdmin = ${isAdmin}"
        }

        return isAdmin
    }

    def isUserEditorForProject(String userId, String projectId) {
        def isEditor = false

        if (userId && projectId) {
            def ups = UserPermission.findAllByUserIdAndProjectId(userId, projectId)
            ups.findAll {
                if (it.accessLevel.code >= AccessLevel.editor.code) {
                    isEditor = true
                }
            }

            log.debug "userCanEdit = ${isEditor}"
        }

        return isEditor // bolean
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
        def up = UserPermission.findAllByProjectIdAndAccessLevel(projectId, AccessLevel.editor)
        up.collect { it.userId } // return just a list of userIds
    }

    def getMembersForProject(String projectId) {
        def up = UserPermission.findAllByProjectIdAndAccessLevelNotEqual(projectId, AccessLevel.starred)
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

    def getProjectsForUserAndAccessLevel(String userId, AccessLevel accessLevel, String projectId) {
        def up = UserPermission.findAllByUserIdAndProjectIdAndAccessLevel(userId, projectId, accessLevel)
        up.collect { Project.findByProjectId(it.projectId) } // return just a list of userIds
    }

    def addUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        def prevRoles = UserPermission.findAllByUserIdAndProjectIdAndAccessLevelNotEqual(userId, projectId, AccessLevel.starred)
        log.debug "0. prevRoles = ${prevRoles}"
        //def highestRoleCode = prevRoles.findAll{ it.accessLevel.code }.max()

        //if (accessLevel.code > highestRoleCode) {
            def up = new UserPermission(userId: userId, projectId: projectId, accessLevel: accessLevel)
            try {
                up.save(flush: true, failOnError: true)
                //return [status:'ok', id: up.id]
            } catch (Exception e) {
                def msg = "Failed to save UserPermission: ${e.message}"
                log.error msg, e
                return [status:'error', error: msg]
            }
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
            return [status:'ok', id: up.id]
        //} else {
        //    return [status:'error', error: "User already has a higher access level (role) for this project"]
        //}
    }

    def removeUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        def up = UserPermission.findByUserIdAndProjectIdAndAccessLevel(userId, projectId, accessLevel)
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
    }
}
