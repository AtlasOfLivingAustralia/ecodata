package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.Report
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.getters.PropertyGetter
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Exports project, site, activity and output data to a Excel spreadsheet.
 */
class ProjectXlsExporter extends ProjectExporter {

    static String DATE_CELL_FORMAT = "dd/mm/yyyy"
    static Log log = LogFactory.getLog(ProjectXlsExporter.class)

    List<String> projectHeaders = ['Project ID', 'Grant ID', 'External ID', 'Organisation', 'Service Provider', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Funding', 'Status', 'Last Modified', 'State 1', 'State 2', 'State 3']

    List<String> projectProperties = ['projectId', 'grantId', 'externalId', 'organisationName', 'serviceProviderName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'funding', 'status', 'lastUpdated']

    List<String> siteHeaders = ['Site ID', 'Name', 'Description', 'lat', 'lon', 'State', 'NRM', 'Electorate', 'Last Modified']
    List<String> siteProperties = ['siteId', 'name', 'description', 'lat', 'lon', 'state', 'nrm', 'elect', 'lastUpdated']
    List<String> commonActivityHeaders = ['Project ID', 'Grant ID', 'External ID', 'Programme', 'Sub-Programme', 'Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Stage', 'Description', 'Activity Type', 'Theme', 'Status', 'Report Status', 'Last Modified']
    List<String> activityProperties = ['projectId', 'grantId', 'externalId', 'associatedProgram', 'associatedSubProgram', 'activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'stage', 'description', 'type', 'mainTheme', 'progress', 'publicationStatus', 'lastUpdated']
    List<String> outputTargetHeaders = ['Project ID', 'Output Target Measure', 'Target', 'Units']
    List<String> outputTargetProperties = ['projectId', 'scoreLabel', new StringToDoublePropertyGetter('target'), 'units']


    XlsExporter exporter

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet activitiesSheet
    AdditionalSheet outputTargetsSheet

    Map<String, List<AdditionalSheet>> typedActivitySheets = [:]

    public ProjectXlsExporter(XlsExporter exporter, String dateFormat = DATE_CELL_FORMAT) {
        this.exporter = exporter
        exporter.setDateCellFormat(dateFormat)
    }

    public void export(Map project) {

        OutputModelProcessor processor = new OutputModelProcessor()
        Map activitiesModel = metadataService.activitiesModel()
        projectSheet()
        outputTargetsSheet()
        sitesSheet()

        int row = projectSheet.getSheet().lastRowNum
        List states = project.sites?.collect{ it?.extent?.geometry?.state }?.unique()
        List properties = new ArrayList(projectProperties)
        states.eachWithIndex { String state, int i ->
            String key = "state" + i
            project.put(key, state)
            properties << key
        }
        projectSheet.add([project], properties, row+1)

        if (project.outputTargets) {
            List nonZeroTargets = project.outputTargets.findAll{ it.scoreLabel && it.target && it.target != "0" }
            List targets = nonZeroTargets.collect {[projectId:project.projectId] << it}
            row = outputTargetsSheet.getSheet().lastRowNum
            outputTargetsSheet.add(targets, outputTargetProperties, row+1)
        }

        if (project.sites) {
            def sites = project.sites.collect {
                def centre = it.extent?.geometry?.centre
                Map props = it.extent?.geometry ?: [:]
                [siteId:it.siteId, name:it.name, description:it.description, lat:centre?centre[1]:"", lon:centre?centre[0]:"", lastUpdated:it.lastUpdated] + props
            }
            row = sitesSheet.getSheet().lastRowNum
            sitesSheet.add(sites, siteProperties, row+1)
        }
        if (project.activities) {
            project.activities.each { activity ->

                Map commonData = project + activity + [stage:getStage(activity, project)]
                List activityData = []
                List activityGetters = []

                activityGetters += activityProperties

                Map activityModel = activitiesModel.activities.find{it.name == activity.type}
                if (activityModel) {
                    activityModel.outputs?.each {output ->
                        if (output != 'Photo Points') { // This is legacy data which doesn't display in the spreadsheet
                            Map config = outputProperties(output)

                            activityGetters += config.propertyGetters

                            OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(output))
                            Map outputData = activity.outputs?.find { it.name == output }
                            if (outputData) {
                                List flatData = processor.flatten(outputData, outputModel, false)
                                flatData = flatData.collect { it + commonData }
                                activityData += flatData
                            }
                        }
                    }
                    AdditionalSheet activitySheet = getActivitySheet(activityModel)
                    int activityRow = activitySheet.sheet.lastRowNum
                    activitySheet.add(activityData, activityGetters, activityRow+1)

                }
                else {
                    log.error("Found activity not in model: "+activity.type)
                }
            }
        }
    }

    String getStage(Map activity, project) {
        Date activityEndDate = activity.plannedEndDate

        if (!activityEndDate) {
            log.error("No end date for activity: ${activity.activityId}, project: ${project.projectId}")
            return ''
        }

        Report report = project.reports?.find { it.fromDate.getTime() < activityEndDate.getTime() && it.toDate.getTime() >= activityEndDate.getTime() }

        report ? report.name : ''
    }

    AdditionalSheet getActivitySheet(Map activityModel) {
        String activityType = activityModel.name
        if (!typedActivitySheets[activityType]) {
            List<String> headers = buildActivityHeaders(activityModel)
            typedActivitySheets[activityType] = exporter.addSheet(activityType, headers)
        }
        typedActivitySheets[activityType]
    }

    List<String> buildActivityHeaders(Map activityModel) {
        List<String> activityHeaders = [] + commonActivityHeaders

        activityModel.outputs?.each { output ->
            Map config = outputProperties(output)
            activityHeaders += config.headers
        }

        activityHeaders
    }

    AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Project', projectHeaders)
        }
        projectSheet
    }

    AdditionalSheet sitesSheet() {
        if (!sitesSheet) {
            sitesSheet = exporter.addSheet('Sites', siteHeaders)
        }
        sitesSheet
    }

    AdditionalSheet activitiesSheet() {
        if (!activitiesSheet) {
            activitiesSheet = exporter.addSheet('Activities', commonActivityHeaders)
        }
        activitiesSheet
    }

    AdditionalSheet outputTargetsSheet() {
        if (!outputTargetsSheet) {
            outputTargetsSheet = exporter.addSheet('Output Targets', outputTargetHeaders)
        }
        outputTargetsSheet
    }

    class StringToDoublePropertyGetter extends PropertyGetter<Object, Number> {

        StringToDoublePropertyGetter(String propertyName) {
            super(propertyName)
        }

        @Override
        protected format(Object value) {
            try {
                return Double.parseDouble(value?.toString())
            }
            catch (NumberFormatException e) {
                return null
            }
        }
    }
}
