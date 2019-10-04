package au.org.ala.ecodata

import grails.converters.JSON
import org.grails.web.json.JSONObject


class ReportController {

    static responseFormats = ['json', 'xml']
    def reportingService

    def get(String id) {
        respond reportingService.get(id, false)
    }

    @RequireApiKey
    def update(String id) {
        if (!id) {
            respond reportingService.create(request.JSON)
        }
        else {
            respond reportingService.update(id, request.JSON)
        }
    }

    @RequireApiKey
    def delete(String id) {
        respond reportingService.delete(id, params.getBoolean('destroy', false))
    }

    def find(String entity, String id) {
        respond reportingService.findAllByOwner(entity + 'Id', id)
    }

    def findByUserId(String id) {
        respond reportingService.findAllForUser(id)
    }

    def search() {
        def searchCriteria = request.JSON

        def reportList = reportingService.search(searchCriteria)
        respond reportList
    }

    /**
     * Clears any data entered for this report.
     * @param id the reportId of the report to clear.
     * @return
     */
    @RequireApiKey
    def reset(String id) {
        respond reportingService.reset(id)
    }

    @RequireApiKey
    def submit(String id) {
        Map params = request.JSON
        if (params.comment == JSONObject.NULL) {
            params.comment = null
        }

        respond reportingService.submit(id, params.comment)
    }

    @RequireApiKey
    def approve(String id) {
        Map params = request.JSON
        if (params.comment == JSONObject.NULL) {
            params.comment = null
        }
        respond reportingService.approve(id, params.comment)
    }

    @RequireApiKey
    def returnForRework(String id) {
        Map params = request.JSON
        if (params.comment == JSONObject.NULL) {
            params.comment = null
        }
        if (params.category == JSONObject.NULL) {
            params.category = null
        }

        respond reportingService.returnForRework(id, params.comment, params.category)
    }

    @RequireApiKey
    def adjust(String id) {
        Map params = request.JSON
        if (params.comment == JSONObject.NULL) {
            params.comment = null
        }
        respond reportingService.adjust(id, params.comment, params.adjustmentActivityType)
    }

    @RequireApiKey
    def runReport() {
        Map params = request.JSON

        render reportingService.aggregateReports(params.searchCriteria, params.reportConfig) as JSON
    }
}
