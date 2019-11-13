package au.org.ala.ecodata.reporting

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

    List<String> activityHeaders = ['Activity Type','Activity Description','Activity Progress', 'Activity Date Created','Activity Start Date','Activity End Date', 'Activity Last Updated' ]
    List<String> activityProperties =  ['type','description','progress', 'dateCreated','startDate','endDate', 'lastUpdated']
    List<String> commonActivityHeaders =  ["Managment Unit ID",'Management Unit Name', 'Report ID', 'Report name', 'Report Description'] + activityHeaders
    List<String> commonActivityProperties = ["managementUnitId",'managementUnitName', 'reportId', 'reportName', 'reportDesc'] +
            activityProperties.collect {
                    ACTIVITY_DATA_PREFIX+it
                }

    ManagementUnitXlsExporter( XlsExporter exporter) {
        super(exporter, [], [:], TimeZone.default)
    }

    void export(activities) {
        if(activities.size()>0){
            activities.each{
                exportReport(it)
            }
        }else{
            //Create a standard empty sheet to avoid malforamt xslx
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
