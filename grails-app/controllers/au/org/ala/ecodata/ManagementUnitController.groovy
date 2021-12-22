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
     * @param The request body should contain a JSON object with a single attribute: ids which is an array
     * of managementUnitIds to retrieve.
     * @return a list of management units with the ids matching the request.
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
            ManagementUnit mu = new ManagementUnit()
            bindData(mu, request.JSON, [include:ManagementUnit.bindingProperties])
            respond managementUnitService.create(mu)
        }
        else {
            ManagementUnit mu = managementUnitService.get(id)
            if (!mu) {
                respond null
            }
            else {
                bindData(mu, request.JSON, [include:ManagementUnit.bindingProperties])
                respond managementUnitService.save(mu)
            }
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
            Map message = managementUnitService.generateReportsInPeriods(params.startDate, params.endDate, params.reportDownloadBaseUrl, params.senderEmail, params.systemEmail,params.email,params.getBoolean("summaryFlag", false))
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
        List financialYears = managementUnitService.getFinancialYearPeriods()
        response.setContentType("application/json")
        respond financialYears
    }
}
