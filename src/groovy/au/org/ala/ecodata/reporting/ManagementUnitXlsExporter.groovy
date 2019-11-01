package au.org.ala.ecodata.reporting

import grails.util.Holders
import au.org.ala.ecodata.ActivityFormService
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet


class ManagementUnitXlsExporter extends TabbedExporter {

    static Log log = LogFactory.getLog(ManagementUnitXlsExporter.class)
    ActivityFormService activityFormService =  Holders.grailsApplication.mainContext.getBean("activityFormService")


    // Avoids name clashes for fields that appear in activitites and projects (such as name / description)
    private static final String ACTIVITY_DATA_PREFIX = 'activity_'

    List<String> activityHeaders = ['Activity Type','Activity Description','Activity Progress', 'Activity Date Created','Activity Start Date','Activity End Date', 'Activity Last Updated' ]
    List<String> activityProperties =  ['type','description','progress', 'dateCreated','startDate','endDate', 'lastUpdated']
    List<String> commonActivityHeaders =  ["Managment Unit ID",'Management Unit Name', 'Report ID', 'Report name', 'Report Description'] + activityHeaders
    List<String> commonActivityProperties = ["managementUnitId",'managementUnitName', 'reportId', 'reportName', 'reportDesc'] +
            activityProperties.collect {
                    ACTIVITY_DATA_PREFIX+it
                }


//    AdditionalSheet activitySheet
    Map<String, String> outputSheetNames = [:]
    Map<String, List<AdditionalSheet>> typedOutputSheets = [:]

    OutputModelProcessor processor = new OutputModelProcessor()

    public ManagementUnitXlsExporter( XlsExporter exporter) {
        super(exporter, [], [:], TimeZone.default)
    }

    public void export(activities) {
        //Map activitiesModel = metadataService.activitiesModel()
        Map activitiesModel = activityFormService.activitiesModel()
        exportOutputs(activities, activitiesModel)

    }


    private void exportOutputs(List activities, Map activitiesModel) {
        if (activities) {
            activitiesModel.outputs.each { outputConfig ->
                    activities.each { activity ->
 //                       if (activity.progress == "started")
                            exportOutput(outputConfig.name, activity)
                    }
            }
        }
    }
    /**
     * Need to convert raw activity to common activity data to avoid naming conflict
     * @param outputName
     * @param activity
     */

    private void exportOutput(String outputName, Map activity) {
        Map output = activity.outputs?.find{it.name == outputName}
        if (output) {
            List outputGetters = commonActivityProperties + outputProperties(outputName).propertyGetters

            Map common_data = convertActivityData(activity)

            List outputData = getOutputData(outputName, activity,common_data)

            AdditionalSheet outputSheet = getOutputSheet(outputName)
            int outputRow = outputSheet.sheet.lastRowNum
            outputSheet.add(outputData, outputGetters, outputRow + 1)
        }
    }

    private List getOutputData(String outputName, Map activity, Map commonData) {
        List flatData = []

        OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(outputName))
        Map outputData = activity.outputs?.find { it.name == outputName }
        if (outputData) {
            flatData = processor.flatten(outputData, outputModel, false)
            flatData = flatData.collect { commonData + it }
        }
        flatData
    }



    AdditionalSheet getOutputSheet(String outputName) {

        if (!typedOutputSheets[outputName]) {
            String name = XlsExporter.sheetName(outputName)

            // If the sheets are named similarly, they may end up the same after being changed to excel
            // tab compatible strings
            int i = 1
            while (outputSheetNames[name]) {
                name = name.substring(0, name.length()-1)
                name = name + Integer.toString(i)
            }

            outputSheetNames[name] = outputName
            List<String> headers = buildOutputHeaders(outputName)
            typedOutputSheets[outputName] = exporter.addSheet(name, headers)
        }
        typedOutputSheets[outputName]
    }



    List<String> buildOutputHeaders(String outputName) {
        List<String> outputHeaders = [] + commonActivityHeaders
        outputHeaders += outputProperties(outputName).headers
        outputHeaders
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
        Map activityBaseData = activity.collectEntries{k,v ->
            if (!k.startsWith('managementUnit') && !k.startsWith('report'))
                [activityDataPrefix+k, v]
            else
                [k, v]
        }
    }


}
