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
    private static final String ACTIVITY_DATA_PREFIX = 'activity_'
    private static final String  REPORT_PREFIX = 'report_'

    List<String> reportProperties = ['reportId', 'name', 'description', 'fromDate', 'toDate']
    List<String> activityHeaders = ['Activity Type','Activity Description','Activity Progress', 'Activity Last Updated' ]
    List<String> activityProperties =  ['type','description','progress', 'lastUpdated']
    List<String> commonActivityHeaders =  ["Management Unit ID",'Management Unit Name', 'Report ID', 'Report name', 'Report Description', 'From Date', 'To Date', 'Current Report Status', 'Date of status change', 'Changed by'] + activityHeaders
    List<String> commonActivityProperties = ["managementUnitId",'managementUnitName', REPORT_PREFIX+'reportId', REPORT_PREFIX+'name', REPORT_PREFIX+'description', REPORT_PREFIX+'fromDate', REPORT_PREFIX+'toDate', REPORT_PREFIX+'reportStatus', REPORT_PREFIX+'dateChanged', REPORT_PREFIX+'changedBy'] +
            activityProperties.collect {
                    ACTIVITY_DATA_PREFIX+it
                }

    ManagementUnitXlsExporter( XlsExporter exporter) {
        super(exporter, [], [:], TimeZone.default)
    }

    void export(List<Map> managementUnits) {
        if(managementUnits.size() > 0) {
            managementUnits.each { Map mu ->
                mu.activities.each { Map activity ->
                    Report report = mu.reports.find {it.activityId == activity.activityId}
                    if (report){
                        activity['managementUnitId'] = mu.managementUnitId
                        activity['managementUnitName'] = mu.name

                        Map reportData = [:]
                        reportProperties.each { String prop ->
                            reportData[REPORT_PREFIX + prop] = report[prop]
                        }
                        reportData.putAll(extractCurrentReportStatus(report).collectEntries { k, v -> [REPORT_PREFIX + k, v] })
                        activity.putAll(reportData)

                    }
                    exportReport(activity)
                }

            }
        }else{
            //Create a standard empty sheet to avoid malformed xslx
            createEmptySheet("Management Unit Reports")
        }
    }

    private void exportReport(Map activity){
        String activityType = activity.type
        Integer formVersion = activity.formVersion

        Map activityCommonData = convertActivityData(activity)
        String sheetName = activityType +  (formVersion? "_V" + formVersion: "")

        Map sheetData = buildOutputSheetData(activity)

        //Combine data from output with  header,getter and  common data
        List outputGetters = commonActivityProperties + sheetData.getters
        List headers = commonActivityHeaders + sheetData.headers
        List outputData = sheetData.data.collect { activityCommonData + it }

        AdditionalSheet outputSheet = createSheet(sheetName, headers)
        int outputRow = outputSheet.sheet.lastRowNum
        outputSheet.add(outputData, outputGetters, outputRow + 1)
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
