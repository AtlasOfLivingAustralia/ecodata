package au.org.ala.ecodata

import grails.converters.JSON
import groovy.json.JsonSlurper

import static org.apache.http.HttpStatus.*

class BulkImportController {

    BulkImportService bulkImportService

    static allowedMethods = ['create': 'POST', 'get': 'GET', 'list': 'GET', 'update': ['PUT', 'POST']]

    @RequireApiKey
    def list() {
        String sort = params.sort ?: "lastUpdated"
        String order = params.order ?: "desc"
        int offset = params.getInt('offset', 0)
        int max = params.getInt('max',10)
        String search = params.query ?: ""
        Map query = [:]
        if (params.containsKey('userId')) {
            query.userId = params.userId
        }

        try {
            Map result = bulkImportService.list(query, [sort: sort, order: order, max: max, offset: offset], search)
            render text: [status: 'ok', total: result.total, items: result.items] as JSON, contentType: 'application/json'
        }
        catch (Exception ex) {
            def message = "Error listing bulk imports - ${ex.message}"
            log.error(message, ex)
            render text: [status: 'error', error: message] as JSON, contentType: 'application/json', status: SC_INTERNAL_SERVER_ERROR
        }
    }


    @RequireApiKey
    def create() {
        def json = new JsonSlurper().parse( request.inputStream)
        if (!json.userId) {
            render text: [status: "error", error: 'Missing userId'] as JSON, status: SC_BAD_REQUEST, contentType: 'application/json'
        } else {
            BulkImport bulkImport = bulkImportService.create(json);
            if (!bulkImport.hasErrors()) {
                render text: [status: 'ok', bulkImportId: bulkImport.bulkImportId] as JSON, contentType: "application/json", status: SC_CREATED
            } else {
                render text: [status: "error", error: 'Failed to save bulk import data'] as JSON, status: SC_INTERNAL_SERVER_ERROR, contentType: 'application/json'
            }
        }

    }

    @RequireApiKey
    def update() {
        def json = new JsonSlurper().parse( request.inputStream)
        if (!params.id) {
            render text: [status: "error", error: "Missing id"] as JSON, status: SC_BAD_REQUEST, contentType: "application/json"
        } else if (params.id != json.bulkImportId) {
            render text: [status: "error", error: "Bulk import identifier provided in JSON body and URL do not match"] as JSON, status: SC_BAD_REQUEST, contentType: "application/json"
        } else {
            Map result = bulkImportService.update(json)
            if (result.error) {
                render(text: result as JSON, status: SC_INTERNAL_SERVER_ERROR, contentType: 'application/json')
            } else {
                render(text: result as JSON, status: SC_OK, contentType: 'application/json')
            }
        }
    }

    @RequireApiKey
    def get() {
        if (!params.id) {
            render text: [status: "error", error: "Missing id"] as JSON, status: SC_BAD_REQUEST, contentType: "application/json"
        } else {
            BulkImport bulkImport = BulkImport.findByBulkImportId(params.id)
            if (bulkImport) {
                respond bulkImport
            } else {
                render text: [status: "error", error: "Bulk import not found"] as JSON, status: SC_NOT_FOUND, contentType: "application/json"
            }
        }
    }
}
