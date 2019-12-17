package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.ManagementUnitXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.DELETED

class ManagementUnitService {
    
    def commonService
    def siteService
    def reportService
    def downloadService

    ManagementUnit get(String muId, includeDeleted = false) {
        if (includeDeleted) {
            return ManagementUnit.findByManagementUnitId(muId)
        }
        ManagementUnit managementUnit = ManagementUnit.findByManagementUnitIdAndStatusNotEqual(muId, DELETED)

        if(managementUnit ){
            if (managementUnit?.associatedOrganisations?.size()>0
                    && managementUnit?.associatedOrganisations[0]?.description.compareToIgnoreCase('Service Provider')==0) {
                String serviceProviderId = managementUnit.associatedOrganisations[0].organisationId

                ManagementUnit[] relevantManagementUnits = ManagementUnit.withCriteria{
                    sizeGt('associatedOrganisations',0)
                }.findAll{
                    it.associatedOrganisations[0].organisationId == serviceProviderId &&
                    it.managementUnitId != managementUnit.managementUnitId
                }
                managementUnit['relevantManagementUnits'] = relevantManagementUnits.collect{return ["name":it.name, "managementUnitId": it.managementUnitId]}
            }

        }
        return managementUnit
    }

    /**
     * Get a list of management units with limited info of sites
     * @param ids
     * @return a list of management units
     */
    List get(String[] ids){
        ManagementUnit[] mues = ManagementUnit.findAllByManagementUnitIdInList(ids.toList()) //convert to list
        // Retrieve site
        //todo ? add relations between MU and Site
        List results = []
        for(ManagementUnit mu in mues){
            Map muInfo = mu.toMap()
            if (mu.managementUnitSiteId){
                //Not work
                //Map site = siteService.getSiteWithLimitedFields(mu.managementUnitSiteId,["siteId","name","extent.geometry.state"])
                Site site = Site.findBySiteId(mu.managementUnitSiteId)

                muInfo['site'] = ["siteId":site.siteId,
                                  "name": site.name,
                                  "state": site.extent?.geometry?.state
                                  ]
            }
            results << muInfo
        }
        return results
    }

    ManagementUnit findByName(String name) {
        return ManagementUnit.findByNameAndStatusNotEqual(name, DELETED)
    }

    ManagementUnit create(ManagementUnit mu) {
        mu.managementUnitId = Identifiers.getNew(true, '')
        save(mu)
    }

    ManagementUnit save(ManagementUnit mu) {
        mu.save(flush:true)
        return mu
    }

    def delete(String id, boolean destroy) {
        ManagementUnit mu = get(id)
        if (mu) {
            try {
                if (destroy) {
                    mu.delete()
                } else {
                    mu.status = DELETED
                    mu.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                ManagementUnit.withSession { session -> session.clear() }
                def error = "Error deleting a management unit ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    List<ManagementUnit> findAllManagementUnitsForUser(String userId) {
        List userMUs = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, ManagementUnit.class.name, DELETED)
        List result = ManagementUnit.findAllByManagementUnitIdInList(userMUs?.collect{it.entityId})
        result
    }

    /**
     * @param ids a list of management unit ids to query.  If null or empty, all ManagementUnit sites will be returned.
     * @return a geojson FeatureCollection containing the management unit boundaries including a geojson property
     * "type" which can be used to colour a map produced from the collection.
     */
    Map managementUnitSiteMap(List ids) {

        // An empirically determined tolerance to produce a small enough result with valid geometry.
        final double MANAGEMENT_UNIT_SITE_TOLERANCE = 0.1
        List managementUnits
        if (ids) {
            managementUnits = ManagementUnit.findAllByManagementUnitIdInListAndStatusNotEqual(ids, Status.DELETED)
        }
        else {
            managementUnits = ManagementUnit.findAllByStatusNotEqual(Status.DELETED)
        }

        Map featureCollection = [type: "FeatureCollection", features: []]

        managementUnits.each { ManagementUnit managementUnit ->
            if (managementUnit.managementUnitSiteId) {
                Map site = siteService.get(managementUnit.managementUnitSiteId)

                if (site) {
                    Map json = siteService.toGeoJson(site)

                    if (json && json.geometry) {
                        // The program sites are very high resolution which would result in very large response to the
                        // client, so we are simplifying them for this map.
                        json.geometry = GeometryUtils.simplify(json.geometry, MANAGEMENT_UNIT_SITE_TOLERANCE)
                        json.properties.name = managementUnit.name
                        json.properties.managementUnitId = managementUnit.managementUnitId
                        featureCollection.features << json
                    }
                    else {
                        log.warn("No geometry for management unit site ${managementUnit.managementUnitSiteId}")
                    }
                }
                else {
                    log.warn("No site for management unit site ${managementUnit.managementUnitSiteId}")
                }

            }
        }

        GeometryUtils.assignDistinctValuesToNeighbouringFeatures(featureCollection.features, "type")
        featureCollection
    }
    /**
     * Get reports of a management unit
     * @param id
     * @return
     */
    List<Map> getReports(String id){
        ManagementUnit mu = get(id, false)
        List reports = reportService.getReportsOfManagementUnit(id)
        reports.collect{
            it['managementUnitId'] = mu['managementUnitId']
            it['managementUnitName'] = mu['name']
        }
        return reports
    }
    /**
     * Generate MU reports in a given period
     * @param startDate
     * @param endDate
     * @return count of reports and downloadId
     */
    Map generateReports(Date startDate, Date endDate){
        List<Map> reports =  getReportingActivities(startDate,endDate)
        int countOfReports = reports.count{it.progress="started"}
        log.info("It contains " + countOfValid +" reports with data")

        params.fileExtension = "xlsx"

        Closure doDownload = { File file ->
            XlsExporter exporter = new XlsExporter(file.absolutePath)
            ManagementUnitXlsExporter muXlsExporter = new ManagementUnitXlsExporter(exporter)
            muXlsExporter.export(reports)
            exporter.sizeColumns()
            exporter.save()
        }
        String downloadId = downloadService.generateReports(params, doDownload)
        return [count: countOfReports, downloadId: downloadId]
    }

    /**
     *
     * @param startDate
     * @param endDate
     * @param reportDownloadBaseUrl  Base url of downloading generated report
     * @param senderEmail
     * @param systemEmail
     * @param receiverEmail
     * @return
     */
    public generateReportsInPeriods(Date startDate, Date endDate, String reportDownloadBaseUrl, String senderEmail, String systemEmail, String receiverEmail ){
        List<Map> reports =  getReportingActivities(startDate,endDate)
        int countOfReports = reports.count{it.progress="started"}
        log.info("It contains " + countOfValid +" reports with data")

        Map params = [:]
        params.fileExtension = "xlsx"
        params.reportDownloadBaseUrl = reportDownloadBaseUrl
        params.senderEmail = senderEmail
        params.systemEmail = systemEmail
        params.email = receiverEmail

        Closure doDownload = { File file ->
            XlsExporter exporter = new XlsExporter(file.absolutePath)
            ManagementUnitXlsExporter  muXlsExporter = new ManagementUnitXlsExporter(exporter)
            muXlsExporter.export(reports)
            exporter.sizeColumns()
            exporter.save()
        }
        String downloadId = downloadService.generateReports(params, doDownload)
        Map message =[:]
        if (countOfReports>0){
            message = [message:"Your will receive an email notification when report is generated", details:downloadId]
        }else{
            message = [message:"Your download will be emailed to you when it is complete. <p> WARNING, the period you requested may not have reports.", details: downloadId]
        }
        return message
    }


    /**
     *  Get reports of all management units in a period
     *
     * @param start
     * @param end
     * @return
     */
    List<Map> getReportingActivities(Date startDate, Date endDate){
        ManagementUnit[] mus =  ManagementUnit.findAll().toArray()
        List reports = reportService.getReportsOfManagementUnits(mus,startDate,endDate)
        return reports
    }

    int[] getFinancialYearPeriods(){
        String[] muIds = ManagementUnit.findAll().toArray().managementUnitId
        Date[] periods = reportService.getPeriodOfManagmentUnitReport(muIds)
        int[] finacialYears = []
        if (periods && periods[0] && periods[1]){
            finacialYears = calculateFinancialYear(periods[0],periods[1])
        }

        return finacialYears
    }

    private int[] calculateFinancialYear(Date startDate, Date endDate){
        // idx starts from 0
        int startMonth = startDate.getAt(Calendar.MONTH)
        int startYear = startDate.getAt(Calendar.YEAR)
        if (startMonth < 6) {
            startYear --
        }

        int endMonth = endDate.getAt(Calendar.MONTH)
        int endYear = endDate.getAt(Calendar.YEAR)
        if (endMonth >= 6)
            endYear ++
        List periods = []
        for(startYear; startYear<endYear; startYear++){
            periods<<startYear
        }

        return periods
    }


}
