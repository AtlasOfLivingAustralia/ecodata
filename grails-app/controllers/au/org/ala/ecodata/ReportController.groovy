package au.org.ala.ecodata

import grails.converters.JSON
import org.apache.http.HttpStatus

import java.text.ParseException
@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class ReportController {

    static responseFormats = ['json', 'xml']
    def reportingService
    ReportService reportService

    def get(String id) {
        respond reportingService.get(id, false)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def update(String id) {
        if (!id) {
            respond reportingService.create(request.JSON)
        }
        else {
            respond reportingService.update(id, request.JSON)
        }
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
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
    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def reset(String id) {
        respond reportingService.reset(id)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def submit(String id) {
        Map params = request.JSON
        respond reportingService.submit(id, params.comment)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def approve(String id) {
        Map params = request.JSON
        respond reportingService.approve(id, params.comment)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def returnForRework(String id) {
        Map params = request.JSON
        respond reportingService.returnForRework(id, params.comment, params.categories)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def cancel(String id) {
        Map params = request.JSON
        respond reportingService.cancel(id, params.comment, params.categories)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def adjust(String id) {
        Map params = request.JSON
        respond reportingService.adjust(id, params.comment, params.adjustmentActivityType)
    }

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
