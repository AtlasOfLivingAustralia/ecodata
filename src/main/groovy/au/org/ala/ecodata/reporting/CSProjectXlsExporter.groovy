package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.metadata.*
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet
import org.bson.types.MinKey

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
    List<String> surveyHeaders = ['Project ID', 'Project Activity ID', 'Activity ID', 'Start date', 'End date', 'Description', 'Status','Attribution', 'Latitude', 'Longitude', 'Centroid Latitude', 'Centroid Longitude','Site Name', 'Site External Id']

    List<String> recordHeaders = ["Occurrence ID", "Activity ID", "GUID", "Scientific Name", "Rights Holder", "Institution ID", "Access Rights", "Basis Of Record", "Data Set ID", "Data Set Name", "Recorded By", "Project Activity ID", "Event Date", "Event Time", "Event Timestamp", "Event Remarks", "Location ID", "Location Name", "Locality", "Location Remarks", "Latitude", "Longitude", "Multimedia","Individual Count"]
    List<String> recordProperties = ["occurrenceID", "guid", "activityId", "scientificName", "rightsHolder", "institutionID", "accessRights", "basisOfRecord", "datasetID", "datasetName", "recordedBy", "projectActivityId", "eventDateCorrected", "eventTime", "eventDate", "eventRemarks", "locationID", "locationName", "locality", "localtionRemarks", "latitude", "longitude", new MultimediaGetter("multimedia", imageMapper), "individualCount" ]

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

    DoublePropertyGetter locationCentroidLatitudeGetter = new DoublePropertyGetter("locationCentroidLatitude")
    DoublePropertyGetter locationCentroidLongitudeGetter = new DoublePropertyGetter("locationCentroidLongitude")

    ProjectActivityService projectActivityService = Holders.grailsApplication.mainContext.getBean("projectActivityService")
    ProjectService projectService = Holders.grailsApplication.mainContext.getBean("projectService")
    SiteService siteService = Holders.grailsApplication.mainContext.getBean("siteService")
    ActivityService activityService = Holders.grailsApplication.mainContext.getBean("activityService")
    RecordService recordService = Holders.grailsApplication.mainContext.getBean("recordService")
    UserService userService = Holders.grailsApplication.mainContext.getBean("userService")
    PermissionService permissionService = Holders.grailsApplication.mainContext.getBean("permissionService")

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

        long start = System.currentTimeMillis()

        projectSheet()
        sitesSheet()
        recordSheet()

        Map project = projectService.get(projectId)

        addSites(project)

        log.info "Add Sites to spreadsheet took ${System.currentTimeMillis() - start} millis"
        start = System.currentTimeMillis()

        addProjectActivities(project, activityIds)

        log.info "Add Project Activity to spreadsheet took ${System.currentTimeMillis() - start} millis"
        start = System.currentTimeMillis()

        addRecords(project, activityIds)

        log.info "Add Records to spreadsheet took ${System.currentTimeMillis() - start} millis"

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
        def userId = userService.currentUserDetails?.userId

        boolean userIsAlaAdmin = false
        if (userId && permissionService.isUserAlaAdmin(userId)) {
            userIsAlaAdmin = true
        }

        List activityList = []
        activityList.addAll(activityIds)

        int batchSize = 1000
        int processed = 0
        def count = batchSize

        List<Map> batchedActivities = []
        // BioCollect currently doesn't use form versioning so just get the latest version of the form.
        ActivityForm form = activityFormService.findActivityForm(projectActivity.pActivityFormName)
        if (activityIds == null || activityIds.isEmpty()) {
            def id = new MinKey()

            while (count == batchSize) {
                batchedActivities = Activity.createCriteria().list([offset: 0, max: batchSize, readOnly: true, sort: 'id', order: "asc"]) {
                    and {
                        eq "projectActivityId", projectActivity.projectActivityId
                        gt "id", id
                    }
                }.collect { activityService.toMap(it, []) }

                sheet = generateSheet(batchedActivities, activityList, projectActivity, userId, userIsAlaAdmin, form, sheet)

                if (batchedActivities.size() > 0)
                    id = batchedActivities.last().id

                count = batchedActivities.size()
                processed += batchSize
            }
        } else {
            def id = new MinKey()

            while (count == batchSize) {
                batchedActivities = Activity.createCriteria().list([offset: 0, max: batchSize, readOnly: true, sort: 'id', order: "asc"]) {
                    eq "projectActivityId", projectActivity.projectActivityId
                    'in' "activityId", activityList
                    gt "id", id
                }.collect { activityService.toMap(it, []) }

                sheet = generateSheet(batchedActivities, activityList, projectActivity, userId, userIsAlaAdmin, form, sheet)

                if (batchedActivities.size() > 0)
                    id = batchedActivities.last().id

                count = batchedActivities.size()
                processed += batchSize
            }
        }
    }

    private generateSheet(List<Map> activities, List activityIds, Map projectActivity, def userId, boolean userIsAlaAdmin, ActivityForm form, AdditionalSheet sheet) {
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
                            new DateConstantGetter("startDate", projectActivity.startDate, null, null, DateTimeParser.Style.DATE, timeZone),
                            new DateConstantGetter("endDate", projectActivity.endDate, null, null, DateTimeParser.Style.DATE, timeZone),
                            new ConstantGetter("description", projectActivity.description),
                            new ConstantGetter("status", projectActivity.status),
                            new ConstantGetter("attribution", projectActivity.attribution),
                            generalLatitudeGetter,
                            generalLongitudeGetter,
                            locationCentroidLatitudeGetter,
                            locationCentroidLongitudeGetter
                    ]
                    // Include user selected projectActivity site associated with the survey
                    def site = projectActivity?.sites?.find{it.siteId == activity.siteId}
                    properties << new ConstantGetter("siteName", site ? site.name : '');
                    properties << new ConstantGetter("siteExternalId", site ? site.externalId : '');

                    boolean userIsProjectMember = false
                    if (userId) {
                        def members = permissionService.getMembersForProject(activity.projectId)
                        userIsProjectMember = members.find{it.userId == userId} || userIsAlaAdmin
                    }

                    activity?.outputs?.each { output ->
                        Map model = form.getFormSection(output.name)?.template
                        if (!model) {
                            log.warn("No form template found for output: ${output.name}")
                            return
                        }
                        OutputMetadata outputModel = new OutputMetadata(model)
                        Map outputConfig = getHeadersAndPropertiesForOutput(outputModel)
                        if (!uniqueOutputs.contains(output.name)) {
                            headers.addAll(outputConfig.headers)
                            uniqueOutputs << output.name
                        }

                        properties.addAll(outputConfig.propertyGetters)

                        processor.hideMemberOnlyAttributes(output, outputModel, userIsProjectMember)
                        List rowSets = processor.flatten2(output, outputModel, OutputModelProcessor.FlattenOptions.REPEAT_ALL)

                        // some outputs (e.g. with list datatypes) result in multiple rows in the spreadsheet, so make sure that the existing rows are duplicated
                        while (rows.size() < rowSets.size()) {
                            rows << rows[0].clone()
                            // shallow clone is ok here, we just need to ensure we have a different map instance
                        }

                        if (rowSets.size() == 1 && rows.size() > 1) {
                            rows.each {
                                it.putAll(rowSets[0])
                            }
                        } else {
                            rowSets.eachWithIndex { outputFields, index ->
                                rows[index].putAll(outputFields)
                            }
                        }

                        // Exclude absence records from the export if the model is configured to do so.
                        if(model?.excludeAbsenceRecord) {
                            List<String> propertyNamesForIndividualCount = outputModel.getPropertyNamesByDwcAttribute("individualCount")
                            if (propertyNamesForIndividualCount) {
                                rows = rows?.findAll{ Map row ->
                                    propertyNamesForIndividualCount.find { String property ->
                                        return (row[property] && row[property] instanceof Number && row[property] > 0)
                                    }
                                }
                            }

                        }
                    }

                    if (rows && !rows[0].isEmpty()) {
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

        List activityIdList = []
        activityIdList.addAll(activityIds)

        def records = []

        if (activityIds == null || activityIds.isEmpty()) {
            records = recordService.getAllByProject(project.projectId)
        } else {
            records = recordService.getAllRecordsByActivityList(activityIdList)
        }

        records.each {
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
