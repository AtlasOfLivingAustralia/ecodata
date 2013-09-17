package au.org.ala.ecodata

import grails.converters.JSON

/**
 * Controller for getting and setting user <-> project
 * ({@link UserPermission}) access permissions.
 *
 * @see au.org.ala.ecodata.UserPermission
 */
class PermissionsController {
    def permissionService

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
                def ps = permissionService.addUserAsEditorToProject(adminId, userId, project)
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
                def ps = permissionService.addUserAsAdminToProject(userId, project)
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
                def ps = permissionService.addUserAsRoleToProject(userId, ac, project)
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
                def ps = permissionService.removeUserAsRoleToProject(userId, ac, project)
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
                def ps = permissionService.addUserAsRoleToProject(userId, role, project)
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
                def ps = permissionService.removeUserAsRoleToProject(userId, role, project)
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
                def userList = permissionService.getUsersForProject(project)
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
                def members = permissionService.getMembersForProject(project)
                render members as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required path not provided: projectId.'
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
            def up = UserPermission.findAllByUserIdAndAccessLevelNotEqual(userId, AccessLevel.starred, params)
            def out  = []
            up.each {
                def t = [:]
                t.project = Project.get(it.project.id)
                t.accessLevel = it.accessLevel
                out.add t
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
            render up.collect { Project.get(it.project.id) } as JSON
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
                def up = UserPermission.findAllByUserIdAndProjectAndAccessLevel(userId, project, AccessLevel.starred)
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
                def out = [userIsEditor: permissionService.isUserEditorForProject(userId, project)]
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
                def out = [userIsEditor: permissionService.isUserAdminForProject(userId, project)]
                render out as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: adminId, userId, projectId'
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
