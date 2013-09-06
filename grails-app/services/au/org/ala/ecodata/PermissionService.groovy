package au.org.ala.ecodata

/**
 * Service to set and get permissions on projects
 */
class PermissionService {

    static transactional = false
    def authService // found in ala-web-theme plugin

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

    def getProjectsForUserAndAccessLevel(String userId, AccessLevel accessLevel, Project project) {
        def up = UserPermission.findAllByUserIdAndProjectAndAccessLevel(userId, project, accessLevel)
        up.collect { Project.get(it.project.id) } // return just a list of userIds
    }

    def addUserAsRoleToProject(String userId, AccessLevel accessLevel, Project project) {
        def up = new UserPermission(userId: userId, project: project, accessLevel: accessLevel)
        try {
            up.save(flush: true, failOnError: true)
            return [status:'ok', id: up.id]
        } catch (Exception e) {
            def msg = "Failed to save UserPermission: ${e.message}"
            log.error msg, e
            return [status:'error', error: msg]
        }
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
