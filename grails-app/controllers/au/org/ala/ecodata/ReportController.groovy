package au.org.ala.ecodata

import grails.converters.JSON
import org.apache.http.HttpStatus

import java.text.ParseException

class ReportController {

    static responseFormats = ['json', 'xml']
    def reportingService
    ReportService reportService

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
        respond reportingService.submit(id, params.comment)
    }

    @RequireApiKey
    def approve(String id) {
        Map params = request.JSON
        respond reportingService.approve(id, params.comment)
    }

    @RequireApiKey
    def returnForRework(String id) {
        Map params = request.JSON
        respond reportingService.returnForRework(id, params.comment, params.categories)
    }

    @RequireApiKey
    def cancel(String id) {
        Map params = request.JSON
        respond reportingService.cancel(id, params.comment, params.categories)
    }

    @RequireApiKey
    def adjust(String id) {
        Map params = request.JSON
        respond reportingService.adjust(id, params.comment, params.adjustmentActivityType)
    }

    @RequireApiKey
    def runReport() {
        Map params = request.JSON

        render reportingService.aggregateReports(params.searchCriteria, params.reportConfig) as JSON
    }

    /**
     * startDate and endDate need to be ISO 8601
     *
     * Get reports of all management units in a given period
     */
    def generateReportsInPeriod(){
        try{
            Map message = reportService.generateReportsInPeriods(params.startDate, params.endDate, params.reportDownloadBaseUrl, params.senderEmail, params.systemEmail,params.email,params.getBoolean("summaryFlag", false), params.entity, params.hubId)
            respond(message, status:200)
        }catch ( ParseException e){
            def message = [message: 'Error: You need to provide startDate and endDate in the format of ISO 8601']
            respond(message, status: HttpStatus.SC_NOT_ACCEPTABLE)
        }catch(Exception e){
            def message = [message: 'Fatal: ' + e.message]
            respond(message, status:HttpStatus.SC_NOT_ACCEPTABLE)
        }
    }
}
