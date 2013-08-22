package au.org.ala.ecodata

class AuditController {

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

}
