package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.ManagementUnitXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
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
     * Get reports of a given management unit
     * @param id reportId
     */
    def report(String id){

        List<Map> activities =  managementUnitService.getReportingActivities(id)
        ManagementUnit mu = managementUnitService.get(id, false)
        activities.collect{
            it['managementUnitId'] = mu['managementUnitId']
            it['managementUnitName'] = mu['name']
        }

        File tmpFile = File.createTempFile(id, '.xslx')
        XlsExporter exporter = new XlsExporter(tmpFile.name)
        exporter.setResponseHeaders(response)

        ManagementUnitXlsExporter  muXlsExporter = new ManagementUnitXlsExporter(exporter)

        muXlsExporter.export(activities)
        exporter.save(response.outputStream)

    }


    /**
     * Get reports of all management units in a given period
     */
    def generateReportsInPeriod(){
        String startDate = params.startDate
        String endDate = params.endDate
        try{
            Date sDate = Date.from(Instant.parse(startDate))
            Date eDate = Date.from(Instant.parse(endDate))
            List<Map> reports =  managementUnitService.getReportingActivities(sDate,eDate)
            int countOfReports = reports.count{it.progress="started"}
            log.info("It contains " + countOfValid +" reports with data")

            params.fileExtension = "xlsx"

            Closure doDownload = { File file ->
                XlsExporter exporter = new XlsExporter(file.absolutePath)
                ManagementUnitXlsExporter  muXlsExporter = new ManagementUnitXlsExporter(exporter)
                muXlsExporter.export(reports)
                exporter.sizeColumns()
                exporter.save()
            }
            String downloadId = downloadService.generateReports(params, doDownload)
            if (countOfReports>0){
                Map message = [message:"Your will receive an email notification when report is generated", details:downloadId]
                respond(message, status:200)
            }else{
                Map message = [message:"Your download will be emailed to you when it is complete. <p> WARNING, the period you requested may not have reports.", details: downloadId]
                respond (message, status:200)
            }
       }catch ( ParseException e){
            def message = [message: 'Error: You need to provide startDate and endDate in the format of ISO 8601']
            respond asJson(message, status:201)
       }catch(Exception e){
            def message = [message: 'Fatal: ' + e.message]
            respond asJson(message, status:201)
        }
    }

    def getReportPeriods(){
        int[] financialYears = managementUnitService.getFinancialYearPeriods()
        //response.setContentType("application/json")
        respond financialYears
    }
}
