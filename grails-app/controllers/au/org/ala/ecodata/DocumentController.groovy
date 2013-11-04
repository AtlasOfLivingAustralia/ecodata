package au.org.ala.ecodata

import grails.converters.JSON

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

    /**
     * Creates or updates a Document object via an HTTP multipart request.
     * This method currently expects:
     * 1) For an update, the document ID should be in the URL path.
     * 2) The document metadata is supplied (JSON encoded) as the value of the
     * "document" HTTP parameter
     * 3) The file contents to be supplied as the value of the "file" HTTP parameter.  This is optional for
     * an update.
     * @param id The ID of an existing document to update.  If not present, a new Document will be created.
     */
    def update(String id) {
        def props = JSON.parse(params.document)
        log.debug props
        def result
        def message

        def file = null
        // Include the file attachment if one exists - it is not currently mandatory as there might be a case
        // for changing metadata without changing the file content.
        if (request.respondsTo('getFile')) {
            file = request.getFile('file')
        }

        if (id) {
            result = documentService.update(props,id, file?.inputStream)
            message = [message: 'updated']
        } else {
            result = documentService.create(props, file?.inputStream)
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
