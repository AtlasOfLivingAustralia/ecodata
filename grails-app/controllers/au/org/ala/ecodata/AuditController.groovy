package au.org.ala.ecodata

import grails.converters.JSON

import static org.apache.http.HttpStatus.SC_BAD_REQUEST

class AuditController {

    def userService
    AuditService auditService
    CommonService commonService

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
        def auditMessages = AuditMessage.findAllByProjectId(projectInstance.projectId, [sort:'date', order:'desc'])

        [projectInstance: projectInstance, auditMessages: auditMessages]
    }

    def getRecentEdits() {
        params.max = params.max?:20
        params.offset = params.offset?:0
        params.sort = params.sort?:"date"
        params.order = params.order?:"desc"
        def auditMessages = AuditMessage.list(params)
        //def auditMessages = AuditMessage.findAll(params, { projectId ==  params.projectId }) // move to its own method
        def count = AuditMessage.count()
        def output = [
                totalRecords: count,
                offset: params.offset,
                max: params.max,
                sort: params.sort,
                order: params.order,
                results: auditMessages]
        render output as JSON
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

    def ajaxGetAuditMessagesForProject() {

        def projectInstance = Project.findByProjectId(params.projectId)
        def retVal  = [success:false, errorMessage:'', messages:[], userMap:[:]]
        if (!projectInstance) {
            retVal.message = "Invalid project id ${params.projectId}"
        } else {
            def auditMessages = auditService.getAllMessagesForProject(projectInstance.projectId)

             if (auditMessages) {
                auditMessages.each{ audit ->
                    def matches = auditMessages.findAll{it.entityId == audit.entityId && it.date < audit.date }
                    if(matches?.size() > 0){
                        audit.entity['compareId'] = matches.get(0).id
                    }
                }
                retVal.success = true
                retVal.messages = auditMessages
                retVal.userMap = auditService.getUserDisplayNamesForMessages(auditMessages)
            }
        }

        render(retVal as JSON)
    }

    def ajaxGetAuditMessage() {
        def auditMessage = AuditMessage.get(params.id)
        def results = [success:true, errorMessage:'']
        if (auditMessage) {
            results.message = auditMessage
        } else {
            results.success = false
            results.errorMessage = "Could not find audit message with specified id!"
        }
        render(results as JSON)
    }

    def ajaxGetUserDetails() {
        def results = [success: true, errorMessage:'']
        if (!params.id) {
            results.success = false
            results.errorMessage = "You must supply a userId"
        } else {
            def userDetails = userService.getUserForUserId(params.id as String)
            if (!userDetails) {
                results.success = false
                results.errorMessage = "User not found!"
            } else {
                results.user = userDetails
            }
        }

        render(results as JSON)
    }

    /**
     * this fucntion is used by datatables to load audit message for each page.
     * @return
     */
    def getAuditMessagesForProjectPerPage(){
        String projectId = params.id;
        Integer start = params.getInt('start')?:0;
        Integer size = params.getInt('size')?:10;
        String sort = params.sort?:'date';
        String order = params.orderBy?:'desc';
        String q = params.q?:null;

        if (!projectId) {
            response.sendError(SC_BAD_REQUEST, 'Project id is required')
        } else {
            Map results = auditService.getAuditMessagesForProjectPerPage(projectId,start,size,sort,order,q);

            render(contentType: 'application/json', text: [ data: results.data, recordsTotal: results.count, recordsFiltered: results.count] as JSON)
        }
    }

    /**
     * Get the previous edited version of the current entity. This is done for comparing two objects.
     */
    def getAutoCompareAuditMessage(){
        String auditId = params.auditId
        AuditMessage compareAudit = auditService.getAutoCompareAuditMessage(auditId)
        Map compare = compareAudit?commonService.toBareMap(compareAudit):[:]
        render(contentType: 'application/json', text: ['message':compare] as JSON)
    }
}
