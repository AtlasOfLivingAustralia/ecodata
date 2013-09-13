package au.org.ala.ecodata

/**
 * Service to set and get permissions on projects
 */
class PermissionService {

    static transactional = false
    def authService, userService // found in ala-web-theme plugin

    public boolean isUserEditorForProject(String userId, Project project) {
        def isEditor = false

        if (userId && project) {
            isEditor = ( UserPermission.findAllByUserIdAndProjectAndAccessLevel(userId, project, AccessLevel.editor) )
        }

        return isEditor
    }

    public boolean isUserAdminForProject(String userId, Project project) {
        def isEditor = false

        if (userId && project) {
            isEditor = ( UserPermission.findAllByUserIdAndProjectAndAccessLevel(userId, project, AccessLevel.admin) )
            log.debug "isEditor = ${isEditor}"
        }

        return isEditor
    }

    /**
     * Does the request userId have permission to edit the requested projectId?
     *
     * @param userId
     * @param project
     * @return
     */
    def canUserEditProject(String userId, Project project) {
        def isEditor = false

        if (userId && project) {
            def ups = UserPermission.findAllByUserIdAndProject(userId, project)
            ups.findAll {
                if (it.accessLevel.code >= AccessLevel.editor.code) {
                    isEditor = true
                }
            }

            log.debug "userCanEdit = ${isEditor}"
        }

        return isEditor // bolean
    }

    def addUserAsEditorToProject(currentUserId, targetUserId, project) {
        if ((isUserAdminForProject(currentUserId, project) || authService.userInRole("ROLE_ADMIN")) && targetUserId) {
            addUserAsRoleToProject(targetUserId, AccessLevel.editor, project)
        }
    }

    def addUserAsAdminToProject(userId, project) {
        addUserAsRoleToProject(userId, AccessLevel.admin, project)
    }

    def getUsersForProject(project) {
        def up = UserPermission.findAllByProjectAndAccessLevel(project, AccessLevel.editor)
        up.collect { it.userId } // return just a list of userIds
    }

    def getMembersForProject(project) {
        def up = UserPermission.findAllByProjectAndAccessLevelNotEqual(project, AccessLevel.starred)
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

    def getProjectsForUserAndAccessLevel(String userId, AccessLevel accessLevel, Project project) {
        def up = UserPermission.findAllByUserIdAndProjectAndAccessLevel(userId, project, accessLevel)
        up.collect { Project.get(it.project.id) } // return just a list of userIds
    }

    def addUserAsRoleToProject(String userId, AccessLevel accessLevel, Project project) {
        def prevRoles = UserPermission.findAllByUserIdAndProjectAndAccessLevelNotEqual(userId, project, AccessLevel.starred)
        log.debug "0. prevRoles = ${prevRoles}"
        //def highestRoleCode = prevRoles.findAll{ it.accessLevel.code }.max()

        //if (accessLevel.code > highestRoleCode) {
            def up = new UserPermission(userId: userId, project: project, accessLevel: accessLevel)
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

    def removeUserAsRoleToProject(String userId, AccessLevel accessLevel, Project project) {
        def up = UserPermission.findByUserIdAndProjectAndAccessLevel(userId, project, accessLevel)
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
