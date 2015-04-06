package au.org.ala.ecodata

import grails.converters.JSON

/**
 * Controller for getting and setting user <-> project
 * ({@link UserPermission}) access permissions.
 *
 * @see au.org.ala.ecodata.UserPermission
 */
class PermissionsController {
    def permissionService, projectService, organisationService

    def index() {
        def msg = [message: "Hello"]
        render msg as JSON
    }

    /**
     * @deprecated for generic {@link #addUserAsRoleToProject()}
     * @return
     */
    def addEditorToProject() {
        def adminId = params.adminId
        def userId = params.userId
        def projectId = params.projectId

        if (adminId && userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def ps = permissionService.addUserAsEditorToProject(adminId, userId, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:404, text: 'Required params not provided: adminId, userId, projectId'
        }

    }

    /**
     * @deprecated for generic {@link #addUserAsRoleToProject()}
     * @return
     */
    def addUserAsAdminToProject() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def ps = permissionService.addUserAsAdminToProject(userId, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:404, text: 'Required params not provided: adminId, userId, projectId'
        }
    }

    def addAdminToOrganisation() {
        def userId = params.userId
        def organisationId = params.projectId

        if (userId && organisationId) {
            def organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                def ps = permissionService.addUserAsAdminToProject(userId, organisationId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status:404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status:404, text: 'Required params not provided: adminId, userId, organisationId'
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
            def project = Project.findByProjectId(projectId)
            AccessLevel ac
            try {
                ac = AccessLevel.valueOf(role)
            } catch (Exception e) {
                render status:500, text: "Error determining role: ${e.message}"
            }

            if (project) {
                log.debug "addUserAsRoleToProject: ${userId}, ${ac}, ${project}"
                def ps = permissionService.addUserAsRoleToProject(userId, ac, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, role, projectId'
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
            def organisation = Organisation.findByOrganisationId(organisationId)
            AccessLevel ac
            try {
                ac = AccessLevel.valueOf(role)
            } catch (Exception e) {
                render status:500, text: "Error determining role: ${e.message}"
            }

            if (organisation) {
                log.debug "addUserAsRoleToOrganisation: ${userId}, ${ac}, ${organisationId}"
                def ps = permissionService.addUserAsRoleToOrganisation(userId, ac, organisationId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status:404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, role, organisationId'
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
            def project = Project.findByProjectId(projectId)
            AccessLevel ac
            try {
                ac = AccessLevel.valueOf(role)
            } catch (Exception e) {
                render status:500, text: "Error determining role: ${e.message}"
            }

            if (project) {
                log.debug "addUserAsRoleToProject: ${userId}, ${ac}, ${project}"
                def ps = permissionService.removeUserAsRoleToProject(userId, ac, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error removing user/role: ${ps}"
                }
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, role, projectId'
        }
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
            def project = Organisation.findByOrganisationId(organisationId)
            AccessLevel ac
            try {
                ac = AccessLevel.valueOf(role)
            } catch (Exception e) {
                render status:500, text: "Error determining role: ${e.message}"
            }

            if (project) {
                log.debug "removeUserWithRoleFromOrganisation: ${userId}, ${ac}, ${project}"
                def ps = permissionService.removeUserAsRoleFromOrganisation(userId, ac, organisationId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Organisation removing user/role: ${ps}"
                }
            } else {
                render status:404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, role, organisationId'
        }
    }

    /**
     * Create a {@link AccessLevel#starred starred} role user-project {@link UserPermission}
     *
     * @return
     */
    def addStarProjectForUser() {
        def projectId = params.projectId
        def userId = params.userId
        AccessLevel role = AccessLevel.starred
        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                log.debug "addUserAsRoleToProject: ${userId}, ${role}, ${project}"
                def ps = permissionService.addUserAsRoleToProject(userId, role, projectId)
                if (ps.status == "ok") {
                    render "success: ${ps.id}"
                } else {
                    render status:500, text: "Error adding editor: ${ps}"
                }
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, projectId.'
        }
    }

    /**
     * Delete a {@link AccessLevel#starred starred} role user-project {@link UserPermission}
     *
     * @return
     */
    def removeStarProjectForUser() {
        def projectId = params.projectId
        def userId = params.userId
        AccessLevel role = AccessLevel.starred
        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def ps = permissionService.removeUserAsRoleToProject(userId, role, projectId)
                if (ps && ps.status == "ok") {
                    render "success: ${ps.id}"
                } else if (ps) {
                    render status:500, text: "Error removing star: ${ps}"
                } else {
                    render status:404, text: "Project: ${projectId} not starred for userId: ${userId}"
                }
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, projectId.'
        }
    }

    /**
     * Get a list of users with {@link AccessLevel#editor editor} role access to the given projectId
     *
     * @return
     */
    def getEditorsForProject() {
        def projectId = params.id
        log.debug "projectId = ${projectId}"
        if (projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def userList = permissionService.getUsersForProject(projectId)
                render userList as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required path not provided: projectId.'
        }
    }

    /**
     * Get a list of users with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link Project project} (via {@link Project#projectId projectId})
     *
     * @return
     */
    def getMembersForProject() {
        def projectId = params.id

        if (projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def members = permissionService.getMembersForProject(projectId)
                render members as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required path not provided: projectId.'
        }
    }

    /**
     * Get a list of users with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link Organisation organisation} (via {@link Organisation#organisationId organisationId})
     */
    def getMembersForOrganisation() {
        def organisationId = params.id

        if (organisationId) {
            def organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                def members = permissionService.getMembersForOrganisation(organisationId)
                render members as JSON
            } else {
                render status:404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status:400, text: 'Required parameters not provided: organisationId.'
        }
    }

    /**
     * Get a list of {@link Project projects} with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getProjectsForUserId() {
        def userId = params.id
        if (userId) {
            def up = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqual(userId, Project.class.name, AccessLevel.starred, params)
            def out  = []
            up.each {
                def t = [:]
                log.debug "it.projectId = ${it.entityId}"
                t.project = projectService.get(it.entityId, ProjectService.FLAT)
                t.accessLevel = it.accessLevel
                if (t.project) out.add t
            }
            render out as JSON
        } else {
            render status:400, text: "Required params not provided: userId"
        }
    }

    /**
     * Get a list of {@link Organisation organisations} with {@link AccessLevel#editor editor} level access or higher
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def getOrganisationsForUserId() {
        def userId = params.id
        if (userId) {
            def up = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqual(userId, Organisation.class.name, AccessLevel.starred, params)
            def out  = []
            up.each {
                def t = [:]
                log.debug "it.organisationId = ${it.entityId}"
                t.organisation = organisationService.get(it.entityId)
                t.accessLevel = it.accessLevel
                if (t.organisation) out.add t
            }
            render out as JSON
        } else {
            render status:400, text: "Required params not provided: userId"
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
            def up = UserPermission.findAllByUserIdAndAccessLevel(userId, AccessLevel.starred)
            render up.collect { Project.findByProjectId(it.entityId) } as JSON
        } else {
            render status:400, text: "Required params not provided: id"
        }
    }

    /**
     * Does a given {@link Project project} have {@link AccessLevel#starred starred} level access
     * for a given {@link UserDetails#userId userId}
     *
     * @return
     */
    def isProjectStarredByUser() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def up = UserPermission.findAllByUserIdAndEntityIdAndEntityTypeAndAccessLevel(userId, projectId, Project.class.name, AccessLevel.starred)
                def outMap = [ isProjectStarredByUser: !up.isEmpty()]
                render outMap as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }

        } else {
            render status:400, text: "Required params not provided: id"
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#editor editor}
     * level access or higher for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def canUserEditProject() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def out = [userIsEditor: permissionService.isUserEditorForProject(userId, projectId)]
                render out as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: adminId, userId, projectId'
        }
    }

    /**
     * Does the request {@link UserDetails#userId userId} have {@link AccessLevel#caseManager caseManager}
     * level access for a given {@link Project project}
     *
     * @return JSON object with a single property representing a boolean value
     */
    def isUserCaseManagerForProject() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def cm = UserPermission.findByUserIdAndEntityIdAndAccessLevel(userId, projectId, AccessLevel.caseManager)
                def out = [userIsCaseManager: (cm) ? true : false]
                render out as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: adminId, userId, projectId'
        }
    }

    /**
     * Does a given {@link UserDetails#userId userId} have {@link AccessLevel#admin admin} level access
     * for a given {@link Project project}
     *
     * @return
     */
    def isUserAdminForProject() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def out = [userIsAdmin: permissionService.isUserAdminForProject(userId, projectId)]
                render out as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, projectId'
        }
    }

    def isUserAdminForOrganisation() {
        def userId = params.userId
        def organisationId = params.organisationId

        if (userId && organisationId) {
            def organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                def out = [userIsAdmin: permissionService.isUserAdminForOrganisation(userId, organisationId)]
                render out as JSON
            } else {
                render status:404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, organisationId'
        }
    }

    def isUserGrantManagerForOrganisation() {
        def userId = params.userId
        def organisationId = params.organisationId

        if (userId && organisationId) {
            def organisation = Organisation.findByOrganisationId(organisationId)
            if (organisation) {
                def out = [userIsGrantManager: permissionService.isUserGrantManagerForOrganisation(userId, organisationId)]
                render out as JSON
            } else {
                render status:404, text: "Organisation not found for organisationId: ${organisationId}"
            }
        } else {
            render status:400, text: 'Required params not provided: userId, organisationId'
        }
    }



    /**
     * Admin function to clear all UserPermissions entries for the
     * specified user.
     *
     * @return
     */
    def clearAllPermissionsForUserId() {
        def userId = params.id // REST style URL (no params)
        def up = UserPermission.findAllByUserId(userId)
        if (up.size() > 0) {
            up.each {
                log.debug "it = ${it}"
                try {
                    it.delete(flush: true)
                    //return [status:'ok', id: it.id]
                } catch (Exception e) {
                    def msg = "Failed to delete UserPermission: ${e.message}"
                    log.error msg, e
                    render status: 500, text: msg
                }
            }
            render text: "OK"
        } else {
            render status:400, text: "No UserPermissions found for userId: ${userId}"
        }
    }

    /**
     * Admin function to clear all UserPermissions entries for
     * ALL users in the system.
     *
     * @return
     */
    def clearAllPermissionsForAllUsers() {
        def up = UserPermission.list()
        if (up.size() > 0) {
            up.each {
                try {
                    it.delete(flush: true)
                } catch (Exception e) {
                    def msg = "Failed to delete UserPermission: ${e.message}"
                    log.error msg, e
                    render status: 500, text: msg
                }
            }
            render text: "OK"
        } else {
            render status:400, text: "No UserPermissions found"
        }
    }

    /**
     * Return a list of the {@link AccessLevel} enum values for editor and higher
     * See the custom JSON serializer in Bootstrap.groovy
     *
     * @return JSON representation of AccessLevel values
     */
    def getAllAccessLevels() {
        render AccessLevel.values().findAll { it.code >= AccessLevel.editor.code } as JSON
    }
}
