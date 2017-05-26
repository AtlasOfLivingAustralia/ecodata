package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.metadata.*
import com.mongodb.BasicDBObject
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet

import static au.org.ala.ecodata.Status.DELETED

/**
 * Export Citizen Science style projects to a zip file containing:
 *
 * <ol>
 *     <li>An Excel spreadsheet with:
 *          <ol>
 *              <li>1 tab listing all projects</li>
 *              <li>1 tab listing all sites</li>
 *              <li>1 tab for each Project Activity in the set of projects, with all fields from all Outputs as columns</li>
 *          </ol>
 *     </li>
 *     <li>A directory containing shape files (as .zip files) for each site</li>
 *     <li>A directory containing all images, with subdirectories for ActivityIds and RecordsIds</li>
 * </ol>
 */
class CSProjectXlsExporter extends ProjectExporter {
    static Log log = LogFactory.getLog(CSProjectXlsExporter.class)

    private def imageMapper = {
        if (it.imageId)
            return Holders.grailsApplication.config.imagesService.baseURL + "/image/details?imageId=" + it.imageId
        def doc = documentMap[it.documentId]
        return doc?.externalUrl ?: doc?.identifier ?: doc?.thumbnail ?: it.identifier ?: it.documentId
    }

    List<String> projectHeaders = ['Project ID', 'Grant ID', 'External ID', 'Organisation', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Funding']
    List<String> projectProperties = ['projectId', 'grantId', 'externalId', 'organisationName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', new DatePropertyGetter('plannedStartDate', DateTimeParser.Style.DATE,null, null,  timeZone), new DatePropertyGetter('plannedEndDate', DateTimeParser.Style.DATE,  null, null, timeZone), 'funding']

    List<String> siteHeaders = ['Site ID', 'Name', 'Description', 'lat', 'lon']
    List<String> siteProperties = ['siteId', 'name', 'description', 'lat', 'lon']
    List<String> surveyHeaders = ['Project ID', 'Project Activity ID', 'Activity ID', 'Site IDs', 'Start date', 'End date', 'Description', 'Status','Attribution', 'Latitude', 'Longitude']

    List<String> recordHeaders = ["Occurrence ID", "GUID", "Scientific Name", "Rights Holder", "Institution ID", "Access Rights", "Basis Of Record", "Data Set ID", "Data Set Name", "Recorded By", "Event Date", "Event Time", "Event Timestamp", "Event Remarks", "Location ID", "Location Name", "Locality", "Location Remarks", "Latitude", "Longitude", "Multimedia"]
    List<String> recordProperties = ["occurrenceID", "guid", "scientificName", "rightsHolder", "institutionID", "accessRights", "basisOfRecord", "datasetID", "datasetName", "recordedBy", "eventDateCorrected", "eventTime", "eventDate", "eventRemarks", "locationID", "locationName", "locality", "localtionRemarks", "latitude", "longitude", new MultimediaGetter("multimedia", imageMapper) ]

    DoublePropertyGetter generalisedLatitudeGetter =  new DoublePropertyGetter("generalisedDecimalLatitude")
    DoublePropertyGetter decimalLatitudeGetter =  new DoublePropertyGetter("decimalLatitude")
    DoublePropertyGetter locationLatitudeGetter = new DoublePropertyGetter("locationLatitude")
    CompositeGetter<Double> generalLatitudeGetter = new CompositeGetter<Double>(generalisedLatitudeGetter, decimalLatitudeGetter, locationLatitudeGetter)
    CompositeGetter<Double> accurateLatitudeGetter = new CompositeGetter<Double>(decimalLatitudeGetter, locationLatitudeGetter, generalisedLatitudeGetter)
    DoublePropertyGetter generalisedLongitudeGetter =  new DoublePropertyGetter("generalisedDecimalLongitude")
    DoublePropertyGetter decimalLongitudeGetter =  new DoublePropertyGetter("decimalLongitude")
    DoublePropertyGetter locationLongitudeGetter =  new DoublePropertyGetter("locationLongitude")
    CompositeGetter<Double> generalLongitudeGetter = new CompositeGetter<Double>(generalisedLongitudeGetter, decimalLongitudeGetter, locationLongitudeGetter)
    CompositeGetter<Double> accurateLongitudeGetter = new CompositeGetter<Double>(decimalLongitudeGetter, locationLongitudeGetter, generalisedLongitudeGetter)

    ProjectActivityService projectActivityService = Holders.grailsApplication.mainContext.getBean("projectActivityService")
    ProjectService projectService = Holders.grailsApplication.mainContext.getBean("projectService")
    SiteService siteService = Holders.grailsApplication.mainContext.getBean("siteService")
    ActivityService activityService = Holders.grailsApplication.mainContext.getBean("activityService")
    RecordService recordService = Holders.grailsApplication.mainContext.getBean("recordService")
    UserService userService = Holders.grailsApplication.mainContext.getBean("userService")

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet recordSheet

    Map<String, AdditionalSheet> surveySheets = [:]
    Map<String, Object> documentMap

    public CSProjectXlsExporter(XlsExporter exporter, Map<String, Object> documentMap,TimeZone timeZone) {
        super(exporter, [], documentMap, timeZone)
        this.documentMap = documentMap
    }

    @Override
    void export(Map project) {
        projectSheet()
        sitesSheet()
        recordSheet()

        addSites(project)

        addProjectActivities(project)

        addRecords(project)

        int row = projectSheet.getSheet().lastRowNum
        projectSheet.add([project], projectProperties, row + 1)
    }

    @Override
    void export(String projectId, Set<String> activityIds) {
        projectSheet()
        sitesSheet()
        recordSheet()

        Map project = projectService.get(projectId)

        addSites(project)

        addProjectActivities(project, activityIds)

        addRecords(project, activityIds)

        int row = projectSheet.getSheet().lastRowNum
        projectSheet.add([project], projectProperties, row + 1)
    }

    private void addSites(Map project) {
        if (project.sites) {
            List sites = project.sites.collect {
                def centre = it.extent?.geometry?.centre
                [siteId: it.siteId, name: it.name, description: it.description, lat: centre ? centre[1] : "", lon: centre ? centre[0] : ""]
            }
            int row = sitesSheet.getSheet().lastRowNum
            sitesSheet.add(sites, siteProperties, row + 1)
        }
    }

    private void addProjectActivities(Map project, Set<String> activityIds = null) {
        List<Map> projectActivities = projectActivityService.getAllByProject(project.projectId, ProjectActivityService.ALL)

        List<String> restrictedSurveys = projectActivityService.listRestrictedProjectActivityIds(userService.currentUserDetails?.userId, project.projectId)

        projectActivities.each { survey ->
            if (!restrictedSurveys.contains(survey.projectActivityId)) {
                AdditionalSheet sheet = surveySheets[survey.name]
                if (!sheet) {
                    sheet = createSurveySheet(survey, activityIds)
                    surveySheets.put(survey.name, sheet)
                }
            }
        }
    }

    private AdditionalSheet createSurveySheet(Map projectActivity, Set<String> activityIds = null) {
        AdditionalSheet sheet = null

        List<Map> activities = activityService.findAllForProjectActivityId(projectActivity.projectActivityId)

        if (activities && (activityIds == null || !activityIds.isEmpty())) {
            List<String> headers = []
            headers.addAll(surveyHeaders)

            OutputModelProcessor processor = new OutputModelProcessor()

            Set<String> uniqueOutputs = [] as HashSet<String>
            activities.each { activity ->
                List rows = [[:]]
                // need to differentiate between an empty set of activity ids (which means don't export any activities),
                // and a null value (which means export all activities).
                if (activityIds == null || activityIds.contains(activity.activityId)) {
                    List properties = [
                            new ConstantGetter("projectId", projectActivity.projectId),
                            new ConstantGetter("projectActivityId", projectActivity.projectActivityId),
                            new ConstantGetter("activityId", activity.activityId),
                            new ConstantGetter("sites", projectActivity.sites.collect { it.siteId }.join(", ")),
                            new DateConstantGetter("startDate", projectActivity.startDate, null, null, DateTimeParser.Style.DATE, timeZone),
                            new DateConstantGetter("endDate", projectActivity.endDate, null, null, DateTimeParser.Style.DATE, timeZone),
                            new ConstantGetter("description", projectActivity.description),
                            new ConstantGetter("status", projectActivity.status),
                            new ConstantGetter("attribution", projectActivity.attribution),
                            generalLatitudeGetter,
                            generalLongitudeGetter
                    ]

                    activity?.outputs?.each { output ->
                        Map outputConfig = outputProperties(output.name)
                        if (!uniqueOutputs.contains(output.name)) {
                            headers.addAll(outputConfig.headers)
                            uniqueOutputs << output.name
                        }

                        properties.addAll(outputConfig.propertyGetters)

                        OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(output.name))

                        List rowSets = processor.flatten(output, outputModel)

                        // some outputs (e.g. with list datatypes) result in multiple rows in the spreadsheet, so make sure that the existing rows are duplicated
                        while (rows.size() < rowSets.size()) {
                            rows << rows[0].clone()
                            // shallow clone is ok here, we just need to ensure we have a different map instance
                        }

                        if (rowSets.size() == 1 && rows.size() > 1) {
                            rows.each {
                                if (rowSets[0] instanceof BasicDBObject) {
                                    it.putAll(rowSets[0].toMap())
                                }
                            }
                        } else {
                            rowSets.eachWithIndex { outputFields, index ->
                                if (outputFields instanceof BasicDBObject) {
                                    rows[index].putAll(outputFields.toMap())
                                }
                            }
                        }
                    }
                    if (!rows[0].isEmpty()) {
                        if (!sheet) {
                            sheet = exporter.sheet(exporter.sheetName(projectActivity.name))
                        }
                        sheet.add(rows, properties, sheet.sheet.lastRowNum + 1)
                    }
                }
            }

            if (sheet) {
                sheet.fillHeader(headers)
                exporter.styleRow(sheet, 0, exporter.headerStyle(exporter.getWorkbook()))
            }
        }

        sheet
    }

    private addRecords(Map project, Set<String> activityIds = null) {
        List properties = []
        properties.addAll(recordProperties)

        List<String> restrictedSurveys = projectActivityService.listRestrictedProjectActivityIds(userService.currentUserDetails?.userId, project.projectId)
        def userId = userService.currentUserDetails?.userId
        def permission = false
        if (userId) {
            List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndAccessLevelNotEqualAndStatusNotEqual(userId, Project.class.name, AccessLevel.starred, DELETED)
            permission = permissions?.find { it.entityId == project.projectId }

        }
        def latitudeGetter = permission ? accurateLatitudeGetter : generalLatitudeGetter
        properties[properties.indexOf("latitude")] = latitudeGetter
        def longitudeGetter = permission ? accurateLongitudeGetter : generalLongitudeGetter
        properties[properties.indexOf("longitude")] = longitudeGetter
        // location of sighting does not reflect the date/time and timezone that was used to capture the record in Biocollect,
        // For consistency with the browser we should honour the client timeZone to do the calculations back as with any other date field
//        properties[properties.indexOf("eventDateCorrected")] = new DatePropertyGetter("eventDate", DateTimeParser.Style.DATE, latitudeGetter, longitudeGetter, timeZone)

        properties[properties.indexOf("eventDateCorrected")] = new DatePropertyGetter("eventDate", DateTimeParser.Style.DATE, null, null, timeZone)
        recordService.getAllByProject(project.projectId).each {
            // need to differentiate between an empty set of activity ids (which means don't export any activities),
            // and a null value (which means export all activities).
            if (!restrictedSurveys.contains(it.projectActivityId) && (activityIds == null || activityIds.contains(it.activityId))) {
                recordSheet.add([it], properties, recordSheet.sheet.lastRowNum + 1)
            }
        }
    }

    private AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Project', projectHeaders)
        }
        projectSheet
    }

    private AdditionalSheet sitesSheet() {
        if (!sitesSheet) {
            sitesSheet = exporter.addSheet('Sites', siteHeaders)
        }
        sitesSheet
    }

    private AdditionalSheet recordSheet() {
        if (!recordSheet) {
            recordSheet = exporter.addSheet('DwC Records', recordHeaders)
        }
        recordSheet
    }

}
