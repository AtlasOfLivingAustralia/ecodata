package au.org.ala.ecodata.reporting
import au.org.ala.ecodata.metadata.ConstantGetter
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
    List<String> activityHeaders = ['Project ID','Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Description', 'Activity Type', 'Theme', 'Status', 'Report Status', 'Last Modified']
    List<String> activityProperties = ['projectId', 'activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'description', 'type', 'mainTheme', 'progress', 'publicationStatus', 'lastUpdated']
    List<String> outputTargetHeaders = ['Project ID', 'Output Target Measure', 'Target', 'Units']
    List<String> outputTargetProperties = ['projectId', 'scoreLabel', new StringToDoublePropertyGetter('target'), 'units']
    List<String> outputHeaders = ['Project ID', 'Grant ID', 'External ID', 'Programme', 'Sub-Programme', 'Site ID']
    List<String> outputProperties = ['projectId', 'grantId', 'externalId', 'associatedProgram', 'associatedSubProgram', 'activityId', 'siteId']


    XlsExporter exporter

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet activitiesSheet
    AdditionalSheet outputTargetsSheet

    Map<String, List<AdditionalSheet>> outputSheets = [:]

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
        activitiesSheet()

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

            def outputsByType = [:].withDefault { [] }

            row = activitiesSheet.getSheet().lastRowNum
            activitiesSheet.add(project.activities, activityProperties, row+1)

            project.activities.each { activity ->
                activity?.outputs?.each { output ->
                    def outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(output.name))
                    outputsByType[output.name] += processor.flatten(output, outputModel )
                }
            }

            outputsByType.each { outputName, data ->
                def config = outputProperties(outputName)
                if (config.headers) {
                    def expandedHeaders = outputHeaders + config.headers
                    if (!outputSheets[outputName]) {
                        outputSheets[outputName] = exporter.addSheet(outputName, expandedHeaders)
                    }
                    AdditionalSheet outputSheet = outputSheets[outputName]
                    row = outputSheet.sheet.lastRowNum


                    List getters = outputProperties + config.propertyGetters
                    List expandedData = data.collect{ Map dataRow ->
                        Map activity = project.activities.find {it.activityId == dataRow.activityId}
                        return dataRow + project + [siteId:activity.siteId]
                    }
                    outputSheet.add(expandedData, getters, row+1)
                }
            }
        }
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
            activitiesSheet = exporter.addSheet('Activities', activityHeaders)
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
