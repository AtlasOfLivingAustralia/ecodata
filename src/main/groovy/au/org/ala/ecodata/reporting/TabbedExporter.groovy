package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.metadata.OutputDataGetter
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.getters.PropertyGetter
import pl.touk.excel.export.multisheet.AdditionalSheet

import java.text.SimpleDateFormat

/**
 * Basic support for exporting data based on a selection of content.
 */
class TabbedExporter {

    static Log log = LogFactory.getLog(TabbedExporter.class)

    MetadataService metadataService = Holders.grailsApplication.mainContext.getBean("metadataService")
    UserService userService = Holders.grailsApplication.mainContext.getBean("userService")
    ReportingService reportingService =  Holders.grailsApplication.mainContext.getBean("reportingService")
    ActivityFormService activityFormService = Holders.grailsApplication.mainContext.getBean("activityFormService")
    OutputModelProcessor processor = new OutputModelProcessor()

    static String DATE_CELL_FORMAT = "dd/MM/yyyy"
    Map<String, AdditionalSheet> sheets
    List<String> tabsToExport
    XlsExporter exporter
    Map<String, Object> documentMap
    TimeZone timeZone
    // These fields map full activity names to shortened names that are compatible with Excel tabs.
    protected Map<String, String> activitySheetNames = [:]
    protected Map<String, List<AdditionalSheet>> typedActivitySheets = [:]

    /** Cache of key: activity type, value: export configuration for that activity */
    protected Map<String, Map> activityExportConfig = [:]

    TabbedExporter(XlsExporter exporter, List<String> tabsToExport = [], Map<String, Object> documentMap = [:], TimeZone timeZone = TimeZone.default) {
        this.exporter = exporter
        this.sheets = new HashMap<String, AdditionalSheet>()
        this.tabsToExport = tabsToExport != null ? tabsToExport : []
        this.documentMap = documentMap
        this.timeZone = timeZone
        exporter.setDateCellFormat(DATE_CELL_FORMAT)
    }

    public setDateFormat(String dateFormat) {
        exporter.setDateCellFormat(dateFormat)
    }

    boolean shouldExport(String sheetName) {
        return !tabsToExport || tabsToExport.contains(sheetName)
    }

    /**
     * Check if one in sheetnames should exported, then export lists
     * Implemented for merit plan
     * @param sheetNames
     * @return
     */
    boolean shouldExport(String[] sheetNames) {
        return !tabsToExport || sheetNames.any{ sheetName -> tabsToExport.contains(sheetName)}
    }

    AdditionalSheet getSheet(String name, List<String> headers) {
        if (!sheets[name]) {
            sheets[name] = exporter.addSheet(name, headers)
        }
        sheets[name]
    }

    /** Matches the status string supplied for a Report (which is determined via the status change history) */
    protected String translatePublicationStatus(String status) {

        String translated
        switch (status) {
            case Report.REPORT_APPROVED:
                translated = 'Approved'
                break
            case Report.REPORT_NOT_APPROVED:
                translated = 'Returned'
                break
            case Report.REPORT_SUBMITTED:
                translated = 'Submitted'
                break
            default:
                translated = 'Unpublished (no action – never been submitted)'
                break

        }
        translated
    }

    private boolean isExportableType(Map dataNode) {
        !(dataNode.dataType in ['photoPoints', 'matrix', 'masterDetail', 'list'])
    }

    /** For compatibility with BioCollect CSProjectXlsExporter */
    Map getHeadersAndPropertiesForOutput(OutputMetadata outputMetadata) {
        List<Map> config = buildOutputExportConfiguration(outputMetadata, null)
        [headers: config.collect{it.header}, propertyGetters: config.collect{it.getter}]
    }

    /**
     * Flatten output data only
     *
     * @param outputModel
     * @param output
     * @return
     */
    private List getOutputData(OutputMetadata  outputModel, Map output) {
        List flatData = []
        if (output) {
            flatData = processor.flatten2(output, outputModel)
        }
        flatData
    }

    protected List getActivityExportConfig(String activityType) {
        String key = activityType

        if (!activityExportConfig[key]) {
            ActivityForm[] forms = activityFormService.findVersionedActivityForm(activityType)
            if (forms) {
                forms = forms.sort{it.formVersion}
                for (ActivityForm form in forms) {
                    List<Map> versionedConfig = buildActivityExportConfiguration(form)
                    if (!activityExportConfig[key]) {
                        activityExportConfig[key] = versionedConfig
                    }
                    else {
                        mergeActivityHeadersAndGetters(key, versionedConfig)
                    }
                }
            } else {
                log.warn("Cannot export activity of type: ${activityType} - no form found")
            }
        }
        activityExportConfig[key]
    }

    /** Merges headers and getters from multiple form versions into the cache */
    private void mergeActivityHeadersAndGetters(String key, List<Map> versionedConfig) {
        List currentConfig = activityExportConfig[key]

        for (int i=0; i<versionedConfig.size(); i++) {
            if (!currentConfig.find{it.property == versionedConfig[i].property}) {
                currentConfig << versionedConfig[i]
            }
        }
    }

    private List<Map> buildActivityExportConfiguration(ActivityForm activityForm) {
        List config = []
        activityForm.sections.each { FormSection section ->
            OutputMetadata outputModel = new OutputMetadata(section.template)

            List outputConfig = buildOutputExportConfiguration(outputModel)
            Map commonProperties = [section: section.name, formVersion: activityForm.formVersion]
            config += outputConfig.collect{it + commonProperties }
        }
        config
    }

    private List<Map> buildOutputExportConfiguration(OutputMetadata outputMetadata) {

        List<Map> fieldConfiguration = []
        fieldConfiguration << [
                header:"Not applicable",
                property:'outputNotCompleted',
                getter:new OutputDataGetter("outputNotCompleted", [dataType:'boolean', name:"outputNotCompleted"], documentMap, timeZone)]

        outputMetadata.modelIterator { String path, Map viewNode, Map dataNode ->
            if (isExportableType(dataNode)) {
                if (dataNode.dataType == 'stringList' && dataNode.constraints) {
                    dataNode.constraints.each { constraint ->
                        String header = outputMetadata.getLabel(viewNode, dataNode) + ' - ' + constraint
                        String propertyPath = path + '[' + constraint + ']'
                        fieldConfiguration << [
                                header:header,
                                property:propertyPath,
                                getter:new OutputDataGetter(path, dataNode, documentMap, timeZone)]
                    }
                }
                else {
                    fieldConfiguration << [
                            header:outputMetadata.getLabel(viewNode, dataNode),
                            property:path,
                            getter:new OutputDataGetter(path, dataNode, documentMap, timeZone)]
                }
            }
        }
        fieldConfiguration
    }

    /**
     * Generate data model of a given output type from activity for excel sheet, if outputname is given
     * Otherwise generate data models of all outputs
     *
     * including, headers, data cell reader and data itself
     * @param activity
     * @param outputName  About the output name
     * @param tabPerVersion true if there should be a different tab for each version of an activity
     * @return  Sheet headers, Getter of data model, data itself
     */
    protected buildOutputSheetData(Map activity,String outputName=null){

        List results = getActivityExportConfig(activity.type)
        List headers = results.collect{it.header}
        List outputGetters = results.collect{ it.getter}

        return [headers: headers, getters: outputGetters, data: prepareActivityDataForExport(activity, outputName)]
    }

    protected List prepareActivityDataForExport(Map activity, String outputName = null) {
        List outputData = []
        ActivityForm activityForm = activityFormService.findActivityForm(activity.type, activity.formVersion)

        activity.outputs.each{ output->
            if ( !outputName || outputName == output.name )  {
                FormSection formSection = activityForm?.getFormSection(output.name)
                if (formSection && formSection.template) {
                    OutputMetadata outputModel = new OutputMetadata(formSection.template)
                    outputData += getOutputData(outputModel, output)
                } else {
                    log.error("Cannot find template of " + output.name)
                }
            }
        }
        outputData
    }

    /**
     *
     * These fields map full activity names to shortened names that are compatible with Excel tabs.
     * If it has the tab already, create a new tab with index
     *
     * @param sheetName
     * @param headers
     * @return
     */
    protected AdditionalSheet createSheet(String sheetName, List headers) {

        if (!typedActivitySheets[sheetName]) {
            String name = XlsExporter.sheetName(sheetName)

            // If the sheets are named similarly, they may end up the same after being changed to excel
            // tab compatible strings
            int i = 1
            while (activitySheetNames[name]) {
                name = name.substring(0, name.length()-3)
                name = name + "("+Integer.toString(i)+")"
                i++
            }

            activitySheetNames[name] = sheetName
            typedActivitySheets[sheetName] = exporter.addSheet(name, headers)
        }
        typedActivitySheets[sheetName]
    }

    protected AdditionalSheet createEmptySheet(){
        createSheet('Sheet 1',[])
    }


    protected void exportReports(AdditionalSheet sheet, Map entity, List<String> reportProperties) {
        int row = sheet.getSheet().lastRowNum
        SimpleDateFormat format = new SimpleDateFormat(DATE_CELL_FORMAT)
        List data = []
        entity.reports?.each { report ->
            Map statusCounts = [:].withDefault { 1 }
            Map previousChange = null
            report.statusChangeHistory?.eachWithIndex { change, i ->
                String statusChange = change.status
                if (change.category) {
                    statusChange = change.category + ' ' + change.status
                }
                int count = statusCounts[statusChange]
                statusCounts[change.status] = count + 1
                String noTimeStr = format.format(change.dateChanged)
                Date noTime = format.parse(noTimeStr)
                int delta = previousChange ? Report.weekDaysBetween(previousChange.dateChanged, change.dateChanged) : 0
                previousChange = entity + [reportName: report.name, fromDate: report.fromDate, toDate: report.toDate, progress:report.progress, reportStatus: statusChange + " " + count, changedBy: change.changedBy, dateChanged: noTime, delta: delta, comment: change.comment]
                data << previousChange
            }
        }
        sheet.add(data, reportProperties, row + 1)
    }

    protected void exportReportSummary(AdditionalSheet sheet, Map entity, List<String> reportSummaryProperties) {
        int row = sheet.getSheet().lastRowNum

        List data = []
        entity.reports?.each { report ->

            Map reportDetails = entity + [reportName:report.name, fromDate:report.fromDate, toDate:report.toDate, progress:report.progress]
            reportDetails.activityCount = reportingService.getActivityCountForReport(report)
            reportDetails.putAll(extractCurrentReportStatus(report))
            data << reportDetails
        }
        sheet.add(data, reportSummaryProperties, row + 1)
    }

    protected Map extractCurrentReportStatus(Report report) {
        Map reportDetails = [:]
        SimpleDateFormat format = new SimpleDateFormat(DATE_CELL_FORMAT)
        if (report.statusChangeHistory) {
            int numChanges = report.statusChangeHistory.size()
            def change = report.statusChangeHistory[numChanges-1]
            String noTimeStr = format.format(change.dateChanged)
            Date noTime = format.parse(noTimeStr)
            int delta = 0
            if (numChanges > 1) {
                def previousChange = report.statusChangeHistory[numChanges-2]
                delta = Report.weekDaysBetween(previousChange.dateChanged, change.dateChanged)

            }
            reportDetails.reportStatus = change.status
            reportDetails.changedBy = change.changedBy
            if (change.changedBy) {
                reportDetails.changedByName = userService.lookupUserDetails(change.changedBy).displayName
            }
            reportDetails.dateChanged = noTime
            reportDetails.delta = delta
        }
        else if (report.toDate <= new Date()) {
            reportDetails.reportStatus = 'Unpublished (no action – never been submitted)'
            reportDetails.delta = 0
        }
        reportDetails
    }

    protected void exportList(String tab, Map project, List data, List headers, List properties) {
        if (shouldExport(tab) && data) {
            AdditionalSheet sheet = getSheet(tab, headers)
            int row = sheet.getSheet().lastRowNum
            List augmentedList = data?.collect {
                it.putAll(project)
                it
            }
            sheet.add(augmentedList, properties, row+1)
        }
    }

    static class LengthLimitedGetter extends PropertyGetter<String, String> {
        /** Excel cells can hold a maximum of 32767 characters */
        static final int MAX_CELL_LENGTH = 32767

        LengthLimitedGetter(String propertyName) {
            super(propertyName)
        }
        String format(String value) {
            if (value && value.length() > MAX_CELL_LENGTH) {
                value = value.substring(0, MAX_CELL_LENGTH)
            }
            return value
        }
    }

    static class StringToDoublePropertyGetter extends PropertyGetter<Object, Number> {

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
