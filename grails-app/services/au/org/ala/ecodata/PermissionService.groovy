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
    }

    def removeUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        removeUserAsRoleToEntity(userId, accessLevel, Project, projectId)
    }

    /**
     * Deletes all permissions associated with the supplied project.  Used as a part of a project delete operation.
     * UserPermissions don't support soft deletes, even if the project itself is soft-deleted.
     * @param projectId
     */
    def deleteAllForProject(String projectId) {
        UserPermission.findAllByEntityId(projectId).each{it.delete()}
    }
}
