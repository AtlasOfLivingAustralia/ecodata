package au.org.ala.ecodata.reporting
import au.org.ala.ecodata.metadata.ConstantGetter
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Exports project, site, activity and output data to a Excel spreadsheet.
 */
class ProjectXlsExporter extends ProjectExporter {

    static Log log = LogFactory.getLog(ProjectXlsExporter.class)

    def projectHeaders = ['Project ID', 'Grant ID', 'External ID', 'Organisation', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Funding']

    def projectProperties = ['projectId', 'grantId', 'externalId', 'organisationName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'funding']

    def siteHeaders = ['Site ID', 'Name', 'Description', 'lat', 'lon']
    def siteProperties = ['siteId', 'name', 'description', 'lat', 'lon']
    def activityHeaders = ['Project ID','Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Description', 'Activity Type', 'Theme', 'Status']
    def activityProperties = ['projectId', 'activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'description', 'type', 'mainTheme', 'progress']

    def XlsExporter exporter

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet activitiesSheet

    Map<String, List<AdditionalSheet>> outputSheets = [:]

    public ProjectXlsExporter(XlsExporter exporter) {
        this.exporter = exporter
    }

    public void export(Map project) {

        OutputModelProcessor processor = new OutputModelProcessor()
        projectSheet()
        sitesSheet()
        activitiesSheet()

        int row = projectSheet.getSheet().lastRowNum
        projectSheet.add([project], projectProperties, row+1)

        if (project.sites) {
            def sites = project.sites.collect {
                def centre = it.extent?.geometry?.centre
                [siteId:it.siteId, name:it.name, description:it.description, lat:centre?centre[1]:"", lon:centre?centre[0]:""]
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
                    outputsByType[output.name] << processor.flatten(output, outputModel )
                }
            }

            outputsByType.each { outputName, data ->
                def config = outputProperties(outputName)
                if (config.headers) {
                    def expandedHeaders = ['Project ID', 'Activity ID', *config.headers]
                    if (!outputSheets[outputName]) {
                        outputSheets[outputName] = exporter.addSheet(outputName, expandedHeaders)
                    }
                    AdditionalSheet outputSheet = outputSheets[outputName]
                    row = outputSheet.sheet.lastRowNum


                    def getters = [new ConstantGetter('projectId', project.projectId), 'activityId', *config.propertyGetters]
                    outputSheet.add(data, getters, row+1)
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
}
