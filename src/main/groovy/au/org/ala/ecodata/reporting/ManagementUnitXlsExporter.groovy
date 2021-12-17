package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.Report
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Generate management unit reports
 */
class ManagementUnitXlsExporter extends TabbedExporter {

    static Log log = LogFactory.getLog(ManagementUnitXlsExporter.class)

    // Avoids name clashes for fields that appear in activitites and projects (such as name / description)
    private static final String  REPORT_PREFIX = 'report_'

    List<String> reportProperties = ['reportId', 'reportName', 'reportDescription', 'fromDate', 'toDate', 'financialYear']
    List<String> activityHeaders = ['Activity Type','Activity Description','Activity Progress', 'Activity Last Updated' ]
    List<String> activityProperties =  ['type','description','progress', 'lastUpdated']
    List<String> commonActivityHeadersSummary =  ["Management Unit ID",'Management Unit Name', 'Report ID', 'Report name', 'Report Description', 'From Date', 'To Date', 'Financial Year', 'Current Report Status', 'Date of status change', 'Changed by']
    List<String> commonActivityHeaders =  commonActivityHeadersSummary + activityHeaders
    List<String> commonActivityPropertiesSummary = ["managementUnitId",'managementUnitName', REPORT_PREFIX+'reportId', REPORT_PREFIX+'reportName', REPORT_PREFIX+'reportDescription', REPORT_PREFIX+'fromDate', REPORT_PREFIX+'toDate', REPORT_PREFIX+'financialYear', REPORT_PREFIX+'reportStatus', REPORT_PREFIX+'dateChanged', REPORT_PREFIX+'changedBy']
    List<String> commonActivityProperties = commonActivityPropertiesSummary +
            activityProperties.collect {
                    ACTIVITY_DATA_PREFIX+it
                }

    ManagementUnitXlsExporter( XlsExporter exporter) {
        super(exporter, [], [:], TimeZone.default)
    }

    void export(List<Map> managementUnits, boolean isSummary = false) {
        if(managementUnits.size() > 0) {
            managementUnits.each { Map mu ->
                mu.activities.each { Map activity ->
                    Report report = mu.reports.find {it.activityId == activity.activityId}
                    if (report){
                        activity['managementUnitId'] = mu.managementUnitId
                        activity['managementUnitName'] = mu.name

                        Map reportData = getReportSummaryInfo(report)
                        reportProperties.each { String prop ->
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
            createEmptySheet("Management Unit Reports")
        }
    }

    private void exportReport(Map activity, boolean isSummary = false){
        Map activityCommonData = convertActivityData(activity)
        if (isSummary) {
            exportActivity(commonActivityHeadersSummary, commonActivityPropertiesSummary, activityCommonData, activity, false, isSummary)
        } else {
            exportActivity(commonActivityHeaders, commonActivityProperties, activityCommonData, activity, false)
        }

    }

    /**
     * Add activty prefix to each property to avoid name conflicts
     * Combine Management unit info as well
     * @param managementUnit
     * @param activity
     * @return
     */
    private Map convertActivityData(Map activity) {
        String activityDataPrefix = ACTIVITY_DATA_PREFIX
        activity.collectEntries{k,v ->
            if (!k.startsWith('managementUnit') && !k.startsWith('report'))
                [activityDataPrefix+k, v]
            else
                [k, v]
        }
    }


}
