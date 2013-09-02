package au.org.ala.ecodata

import grails.converters.JSON

class PermissionsController {
    def permissionService

    def index() {
        def msg = [message: "Hello"]
        render msg as JSON
    }

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

    def getStarredProjectForUserId() {
        String projectId = params.projectId
        String userId = params.userId
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
            render status:400, text: 'Required params not provided: userId, projectId'
        }
    }

    def starProjectForUser() {
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
            render status:400, text: 'Required params not provided: userId, projectId'
        }
    }

    def getEditorsForProject() {
        def projectId = params.id
        log.debug "projectId = ${projectId}"
        def project = Project.findByProjectId(projectId)
        if (project) {
            def userList = permissionService.getUsersForProject(project)
            render userList as JSON
        } else {
            render status:404, text: "Project not found for projectId: ${projectId}"
        }
    }

    def getProjectsForUserId() {
        def userId = params.id
        if (userId) {
            def up = UserPermission.findAllByUserId(userId, params)
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

    def getStarredProjectsForUserId() {
        String userId = params.id

        if (userId) {
            def up = UserPermission.findAllByUserIdAndAccessLevel(userId, AccessLevel.starred)
            render up.collect { Project.get(it.project.id) } as JSON
        } else {
            render status:400, text: "Required params not provided: id"
        }
    }

    def isProjectStarredByUser() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                def up = UserPermission.findAllByUserIdAndProjectAndAccessLevel(userId, project, AccessLevel.starred)
                render up.collect { Project.get(it.project.id) } as JSON
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }

        } else {
            render status:400, text: "Required params not provided: id"
        }
    }

    def isUserEditorForProject() {
        def userId = params.userId
        def projectId = params.projectId

        if (userId && projectId) {
            def project = Project.findByProjectId(projectId)
            if (project) {
                boolean ps = permissionService.isUserEditorForProject(userId, project)
                render ps
            } else {
                render status:404, text: "Project not found for projectId: ${projectId}"
            }
        } else {
            render status:400, text: 'Required params not provided: adminId, userId, projectId'
        }
    }
}
