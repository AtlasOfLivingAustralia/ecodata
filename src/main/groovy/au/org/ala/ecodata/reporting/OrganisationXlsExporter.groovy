package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.DateUtil
import au.org.ala.ecodata.Report
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.XlsxExporter
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Exports organisation related information to an Excel spreadsheet
 */
class OrganisationXlsExporter extends TabbedExporter {

    static Log log = LogFactory.getLog(OrganisationXlsExporter.class)
    // Avoids name clashes for fields that appear in organisation and activities
    private static final String  REPORT_PREFIX = 'report_'

    List<String> commonOrganisationHeaders = ['Organisation ID', 'Name']
    List<String> commonOrganisationProperties = ['organisationId', 'name']

    List<String> organisationHeaders = commonOrganisationHeaders + ['Acronym', 'Description', 'State']
    List<String> organisationProperties = commonOrganisationProperties + ['acronym', 'description', 'state']

    List<String> reportHeaders = commonOrganisationHeaders + ['Report', 'From Date', 'To Date', 'Data Entry Progress', 'Action', 'Action Date', 'Actioned By', 'Weekdays since last action', 'Comment', 'Categories']
    List<String> reportProperties = commonOrganisationProperties + ['reportName', 'fromDate', 'toDate', 'progress', 'reportStatus', 'dateChanged', 'changedBy', 'delta', 'comment', 'categories']

    List<String> reportSummaryHeaders = commonOrganisationHeaders + ['Report', 'Stage from', 'Stage to', 'Data Entry Progress', 'Current Report Status', 'Date of action', 'No. weekdays since previous action', 'Actioned By: user number', 'Actioned by: user name']
    List<String> reportSummaryProperties = commonOrganisationProperties + ['reportName', 'fromDate', 'toDate', 'progress', 'reportStatus', 'dateChanged', 'delta', 'changedBy', 'changedByName']

    List<String> reportDataHeaders = commonOrganisationHeaders + ['Report', 'Stage from', 'Stage to', 'Data Entry Progress', 'Report Status']
    List<String> reportDataProperties = commonOrganisationProperties + ['reportName', 'fromDate', 'toDate', 'progress', 'publicationStatus']

    List<String> reportPropertiesMinimumSet = ['reportId', 'reportName', 'reportDescription', 'fromDate', 'toDate', 'financialYear']
    List<String> activityHeaders = ['Activity Type','Activity Description','Activity Progress', 'Activity Last Updated' ]
    List<String> activityProperties =  ['type','description','progress', 'lastUpdated']
    List<String> commonActivityHeadersSummary =  ["Organisation ID",'Organisation Name','Organisation ABN', 'Report ID', 'Report name', 'Report Description', 'From Date', 'To Date', 'Financial Year', 'Current Report Status', 'Date of status change', 'Changed by']
    List<String> commonActivityHeaders =  commonActivityHeadersSummary + activityHeaders
    List<String> commonActivityPropertiesSummary = ["organisationId",'organisationName','organisationAbn', REPORT_PREFIX+'reportId', REPORT_PREFIX+'reportName', REPORT_PREFIX+'reportDescription', REPORT_PREFIX+'fromDate', REPORT_PREFIX+'toDate', REPORT_PREFIX+'financialYear', REPORT_PREFIX+'reportStatus', REPORT_PREFIX+'dateChanged', REPORT_PREFIX+'changedBy']
    List<String> commonActivityProperties = commonActivityPropertiesSummary +
            activityProperties.collect {
                ACTIVITY_DATA_PREFIX+it
            }

    public OrganisationXlsExporter(XlsxExporter exporter, List<String> tabsToExport, Map<String, Object> documentMap) {
        super(exporter, tabsToExport, documentMap)

    }

    void export(List<Map> orgs, boolean isSummary = false) {
        if(orgs.size() > 0) {
            orgs.each { Map org ->
                org.activities.each { Map activity ->
                    Report report = org.reports.find {it.activityId == activity.activityId}
                    if (report){
                        activity['organisationId'] = org.organisationId
                        activity['organisationName'] = org.name
                        activity['organisationAbn'] = org.abn

                        Map reportData = getReportSummaryInfo(report)
                        reportPropertiesMinimumSet.each { String prop ->
                            reportData[REPORT_PREFIX + prop] = reportData[prop]
                        }
                        reportData.putAll(extractCurrentReportStatus(report).collectEntries { k, v -> [REPORT_PREFIX + k, v] })
                        activity.putAll(reportData)

                    }
                    exportReport(activity, isSummary)

                }
            }
        }else{
            //Create a standard empty sheet to avoid malformed xslx
            createEmptySheet("Organisations")
        }
    }

    public void export(Map organisation) {
        exportOrganisation(organisation)
        exportReports(organisation)
        exportReportSummary(organisation)
        exportPerformanceAssessmentReport(organisation)
    }

    protected Map getReportSummaryInfo(Report report) {
        [stage:report?.name, reportName:report?.name, reportDescription:report?.description, reportId:report?.reportId, reportType:report?.generatedBy, fromDate:report?.fromDate, toDate:report?.toDate, financialYear: report ? DateUtil.getFinancialYearBasedOnEndDate(report.toDate) : ""]
    }

    private void exportReport(Map activity, boolean isSummary = false){
        Map activityCommonData = convertActivityData(activity)
        if (isSummary) {
            Map commonData = commonActivityData(activityCommonData, activity)
            exportActivityOrOutput(commonActivityHeaders, commonActivityProperties, activity.type, [], commonData, [[:]])
        } else {
            exportActivity(commonActivityHeaders, commonActivityProperties, activityCommonData, activity, false)
        }
    }

    /**
     * Add activity prefix to each property to avoid name conflicts
     * Combine Organisation info as well
     * @param activity
     * @return
     */
    private Map convertActivityData(Map activity) {
        activity.collectEntries{k,v ->
            if (!k.startsWith('organisation') && !k.startsWith('report'))
                [ACTIVITY_DATA_PREFIX+k, v]
            else
                [k, v]
        }
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

            List results = []
            Map reportHeaderData = buildOutputSheetData([type:reportOutput, formVersion:1, outputs:[]])

            organisation.reports.each {report ->

                if (report.type == "Performance Management Framework - Self Assessment") {

                    Map reportData = buildOutputSheetData([type:reportOutput, formVersion:1, outputs:[[name:reportOutput, data:report.data]]])
                    if (reportData.data) {
                        results += reportData.data.collect { it + organisation + [reportName:report.name, fromDate:report.fromDate, toDate:report.toDate, progress: report.progress, publicationStatus: report.publicationStatus] }
                    }
                }
            }

            AdditionalSheet sheet = getSheet(reportOutput, reportHeaderData.headers)
            int row = sheet.getSheet().lastRowNum
            sheet.add([results], reportHeaderData.getters, row + 1)
        }
    }
}
