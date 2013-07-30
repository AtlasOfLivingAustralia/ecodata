package au.org.ala.ecodata

import org.springframework.dao.DataIntegrityViolationException

class DocumentController {

    def documentService

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    def index() {
        log.debug "Total documentss = ${Document.count()}"
        render "${Document.count()} documents"
    }

    def get(String id) {
        def detail = []
        if (!id) {
            def list = documentService.getAll(params.includeDeleted as boolean, params.view)
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def doc = documentService.get(id, detail)
            if (doc) {
                asJson doc
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    def delete(String id) {
        def a = Document.findByDocumentId(id)
        if (a) {
            if (params.destroy) {
                a.delete()
            } else {
                a.status = 'deleted'
                a.save(flush: true)
            }
            render (status: 200, text: 'deleted')
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    def update(String id) {
        def props = request.JSON
        log.debug props
        def result
        def message
        if (id) {
            result = documentService.update(props,id)
            message = [message: 'updated']
        }
        else {
            result = documentService.create(props)
            message = [message: 'created', documentId: result.documentId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            //Document.withSession { session -> session.clear() }
            log.error result.error
            render status:400, text: result.error
        }
    }

}
