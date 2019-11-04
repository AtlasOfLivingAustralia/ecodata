package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.FormSection
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

    OutputModelProcessor processor = new OutputModelProcessor()

    ManagementUnitXlsExporter( XlsExporter exporter) {
        super(exporter, [], [:], TimeZone.default)
    }

    public void export(activities) {
        if(activities.size()>0){
            activities.each{
                exportReport(it)
            }
        }else{
            AdditionalSheet outputSheet = createSheet("Sheet1", commonActivityHeaders)
        }

    }

    private void exportReport(Map activity){
        String activityType = activity.type
        int formVersion = activity.formVersion
        ActivityForm activityForm = activityFormService.findActivityForm(activityType, formVersion)
        Map activityCommonData = convertActivityData(activity)

        activity.outputs.each{ output->
            FormSection formSection = activityForm.getFormSection(output.name)
            if(formSection && formSection.template){
                String sheetName = output.name + "_V" + formVersion
                OutputMetadata outputModel = new OutputMetadata(formSection.template)

                Map outputProperty = buildOutputProperties(outputModel)
                List outputGetters = commonActivityProperties + outputProperty.propertyGetters
                List headers = commonActivityHeaders + outputProperty.headers

                List outputData = getOutputData(outputModel, output, activityCommonData)

                AdditionalSheet outputSheet = createSheet(sheetName, headers)
                int outputRow = outputSheet.sheet.lastRowNum
                outputSheet.add(outputData, outputGetters, outputRow + 1)
            }else{
                log.error("Cannot find template of " + output.name)
            }
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
        Map activityBaseData = activity.collectEntries{k,v ->
            if (!k.startsWith('managementUnit') && !k.startsWith('report'))
                [activityDataPrefix+k, v]
            else
                [k, v]
        }
    }


}
