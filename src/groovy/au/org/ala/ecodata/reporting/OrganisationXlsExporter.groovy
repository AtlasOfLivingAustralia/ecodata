package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.XlsxExporter
import pl.touk.excel.export.multisheet.AdditionalSheet
/**
 * Exports organisation related information to an Excel spreadsheet
 */
class OrganisationXlsExporter extends TabbedExporter {

    static Log log = LogFactory.getLog(OrganisationXlsExporter.class)

    List<String> commonOrganisationHeaders = ['Organisation ID', 'Name']
    List<String> commonOrganisationProperties = ['organisationId', 'name']

    List<String> organisationHeaders = commonOrganisationHeaders + ['Acronym', 'Description', 'State']
    List<String> organisationProperties = commonOrganisationProperties + ['acronym', 'description', 'state']

    List<String> reportHeaders = commonOrganisationHeaders + ['Report', 'From Date', 'To Date', 'Data Entry Progress', 'Action', 'Action Date', 'Actioned By', 'Weekdays since last action', 'Comment']
    List<String> reportProperties = commonOrganisationProperties + ['reportName', 'fromDate', 'toDate', 'progress', 'reportStatus', 'dateChanged', 'changedBy', 'delta', 'comment']

    List<String> reportSummaryHeaders = commonOrganisationHeaders + ['Report', 'Stage from', 'Stage to', 'Data Entry Progress', 'Current Report Status', 'Date of action', 'No. weekdays since previous action', 'Actioned By: user number', 'Actioned by: user name']
    List<String> reportSummaryProperties = commonOrganisationProperties + ['reportName', 'fromDate', 'toDate', 'progress', 'reportStatus', 'dateChanged', 'delta', 'changedBy', 'changedByName']

    List<String> reportDataHeaders = commonOrganisationHeaders + ['Report', 'Stage from', 'Stage to', 'Data Entry Progress', 'Report Status']
    List<String> reportDataProperties = commonOrganisationProperties + ['reportName', 'fromDate', 'toDate', 'progress', 'publicationStatus']

    public OrganisationXlsExporter(XlsxExporter exporter, List<String> tabsToExport, Map<String, Object> documentMap) {
        super(exporter, tabsToExport, documentMap)

    }

    public void export(Map organisation) {
        exportOrganisation(organisation)
        exportReports(organisation)
        exportReportSummary(organisation)
        exportPerformanceAssessmentReport(organisation)
    }

    private void exportOrganisation(Map organisation) {
        String orgSheetName = 'Organisations'
        if (shouldExport(orgSheetName)) {
            AdditionalSheet sheet = getSheet(orgSheetName, organisationHeaders)
            int row = sheet.getSheet().lastRowNum

            List properties = new ArrayList(organisationProperties)

            sheet.add([organisation], properties, row + 1)
        }
    }

    private void exportReports(Map organisation) {
        String reportSheetName = "Reports"
        if (shouldExport(reportSheetName)) {
            AdditionalSheet sheet = getSheet(reportSheetName, reportHeaders)
            exportReports(sheet, organisation, reportProperties)
        }
    }

    private void exportReportSummary(Map organisation) {
        String reportSummaryName = "Report Summary"
        if (shouldExport(reportSummaryName)) {
            AdditionalSheet sheet = getSheet(reportSummaryName, reportSummaryHeaders)
            exportReportSummary(sheet, organisation, reportSummaryProperties)
        }
    }


    private void exportPerformanceAssessmentReport(Map organisation) {

        String reportOutput = "Performance Self Assessment"

        if (shouldExport(reportOutput)) {
            OutputModelProcessor processor = new OutputModelProcessor()
            Map config = outputProperties(reportOutput)

            List properties = reportDataProperties + config.propertyGetters
            List headers = reportDataHeaders + config.headers
            AdditionalSheet sheet = getSheet(reportOutput, headers)
            int row = sheet.getSheet().lastRowNum

            OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModel(reportOutput))

            List results = []
            organisation.reports.each {report ->

                if (report.type == "Performance Management Framework - Self Assessment") {
                    Map outputData = [data:report.data]
                    if (outputData) {
                        List reportData = processor.flatten(outputData, outputModel, false)

                        results += reportData.collect { it + organisation + [reportName:report.name, fromDate:report.fromDate, toDate:report.toDate, progress: report.progress, publicationStatus: report.publicationStatus] }

                    }
                }
            }


            sheet.add([results], properties, row + 1)
        }
    }
}
