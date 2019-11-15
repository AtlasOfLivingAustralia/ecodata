package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.ManagementUnitXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import org.apache.http.HttpStatus

import java.text.ParseException
import java.time.Instant


@RequireApiKey
class ManagementUnitController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [get:'GET', findByName: 'GET', search:'GET', findAllForUser: 'GET', update:['PUT', 'POST'], delete:'DELETE',getManagementUnits: ['POST'], findAllForUser:'GET', managementUnitSiteMap: ['GET', 'POST']]

    ManagementUnitService managementUnitService
    ElasticSearchService elasticSearchService
    ActivityService activityService
    DownloadService downloadService
    UserService userService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

    def get(String id) {
        ManagementUnit mu = managementUnitService.get(id, false)
        respond mu
    }

    /**
     * A list of programIds
     * @return a list of programs
     */
    def getManagementUnits() {
        String[] ids =  request.getJSON()?.managementUnitIds
        if(ids){
            List mues =  managementUnitService.get(ids)
            respond mues
        }
        else{
            respond []
        }
    }

    def findByName(String name) {
        respond managementUnitService.findByName(name)
    }

    def update(String id) {
        if (!id) {
            respond managementUnitService.create(request.JSON)
        }
        else {
            respond managementUnitService.update(id, request.JSON)
        }
    }

    def delete(String id) {
        respond managementUnitService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }

    def findAllForUser(String id) {
        respond managementUnitService.findAllManagementUnitsForUser(id)
    }

    /**
     *
     * The POST body can optionally contain a JSON formatted attribute "managementUnitIds" which supplies a list
     * of management unit ids to query.  Otherwise all ManagementUnit sites will be returned.
     * @return a geojson FeatureCollection containing the management unit boundaries including a geojson property
     * "type" which can be used to colour a map produced from the collection.
     */
    def managementUnitSiteMap() {
        List ids = null
        if (request.method == 'POST') {
            ids = request.JSON?.managementUnitIds
        }
        respond managementUnitService.managementUnitSiteMap(ids)
    }


    /**
     * startDate and endDate need to be ISO 8601
     *
     * Get reports of all management units in a given period
     */
    def generateReportsInPeriod(){
        try{
            Date sDate = Date.from(Instant.parse(params.startDate))
            Date eDate = Date.from(Instant.parse(params.endDate))
            Map message = managementUnitService.generateReportsInPeriods(sDate, eDate, params.reportDownloadBaseUrl, params.senderEmail, params.systemEmail,params.email)
            respond(message, status:200)
       }catch ( ParseException e){
            def message = [message: 'Error: You need to provide startDate and endDate in the format of ISO 8601']
            respond(message, status:HttpStatus.SC_NOT_ACCEPTABLE)
       }catch(Exception e){
            def message = [message: 'Fatal: ' + e.message]
            respond(message, status:HttpStatus.SC_NOT_ACCEPTABLE)
        }
    }
    /**
     * Get financial years of managment unit reports cover
     * @return
     */
    def getReportPeriods(){
        int[] financialYears = managementUnitService.getFinancialYearPeriods()
        //response.setContentType("application/json")
        respond financialYears
    }
}
