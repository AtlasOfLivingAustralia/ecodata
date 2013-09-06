package au.org.ala.ecodata

import grails.converters.JSON

class AuditController {
    def userService
    def entityAuditMessageTableFragment() {
        def entityId = params.entityId
        def messages = []
        if (entityId) {
            messages = AuditMessage.findAllByEntityId(entityId, [sort:'date', order: 'asc'])
        }
        [auditMessages: messages]
    }

    def messageEntityDetails() {
        def auditMessage = AuditMessage.get(params.id)
        [auditMessage: auditMessage]
    }

    def findProjectResultsTableFragment() {
        def q = params.q
        def projectList = []
        if (q) {
            projectList = Project.findAllByNameIlike('%' + q + '%')
        }
        [projectList: projectList]
    }

    def auditProject() {
        def projectInstance = Project.findByProjectId(params.projectId)
        if (!projectInstance) {
            flash.message = "Could not locate project with project id ${params.id}"
            redirect(controller: 'admin', action: 'auditMessagesByProject')
            return
        }
        def auditMessages = AuditMessage.findAllByProjectId(projectInstance.projectId, [sort:'date', order:'asc'])

        [projectInstance: projectInstance, auditMessages: auditMessages]
    }

    def getRecentEditsForUserId() {
        def userId = params.id
        def user = userService.getUserForUserId(userId) // checks auth for userid
        if (user) {
            def auditMessages = AuditMessage.findAllByUserIdAndEntityTypeNotEqual(userId, "au.org.ala.ecodata.UserPermission", [sort:'date', order:'desc',max: 10])
            render auditMessages as JSON
        } else {
            render status:404, text: "User not found for userId: ${userId}"
        }
    }

}
