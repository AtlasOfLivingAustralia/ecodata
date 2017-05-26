package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.ReportingService
import au.org.ala.ecodata.UserService
import au.org.ala.ecodata.metadata.OutputDataPropertiesBuilder
import grails.util.Holders
import pl.touk.excel.export.getters.PropertyGetter
import pl.touk.excel.export.multisheet.AdditionalSheet

import java.text.SimpleDateFormat

/**
 * Basic support for exporting data based on a selection of content.
 */
class TabbedExporter {

    MetadataService metadataService = Holders.grailsApplication.mainContext.getBean("metadataService")
    UserService userService = Holders.grailsApplication.mainContext.getBean("userService")
    ReportingService reportingService =  Holders.grailsApplication.mainContext.getBean("reportingService")

    static String DATE_CELL_FORMAT = "dd/MM/yyyy"
    Map<String, AdditionalSheet> sheets
    List<String> tabsToExport
    XlsExporter exporter
    Map<String, Object> documentMap
    TimeZone timeZone

    public TabbedExporter(XlsExporter exporter, List<String> tabsToExport = [], Map<String, Object> documentMap = [:], TimeZone timeZone = TimeZone.default) {
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

    AdditionalSheet getSheet(String name, List<String> headers) {
        if (!sheets[name]) {
            sheets[name] = exporter.addSheet(name, headers)
        }
        sheets[name]
    }

    protected Map outputProperties(name) {
        def model = metadataService.annotatedOutputDataModel(name)

        def headers = []
        def properties = []
        model.each {
            if (it.dataType == 'list') {
                it.columns.each { col ->
                    properties << it.name + '.' + col.name
                    headers << col.label
                }
            } else if (it.dataType in ['photoPoints', 'matrix', 'masterDetail', 'geoMap']) {
                // not supported, do nothing.
            } else if (it.dataType == 'stringList') {
                if (it.constraints) {
                    it.constraints.each { constraint ->
                        headers << it.description + ' - ' + constraint
                        properties << it.name + '['+constraint+']'
                    }

                }
                else {
                    properties << it.name
                    headers << it.label ?: it.description
                }
            }
            else {
                properties << it.name
                headers << it.label ?: it.description
            }
        }
        List propertyGetters = properties.collect { new OutputDataPropertiesBuilder(it, model, documentMap, timeZone) }
        [headers: headers, propertyGetters: propertyGetters]
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
        SimpleDateFormat format = new SimpleDateFormat(DATE_CELL_FORMAT)
        List data = []
        entity.reports?.each { report ->

            Map reportDetails = entity + [reportName:report.name, fromDate:report.fromDate, toDate:report.toDate, progress:report.progress]
            reportDetails.activityCount = reportingService.getActivityCountForReport(report)
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

                data << reportDetails
            }
            else if (report.toDate <= new Date()) {
                reportDetails.reportStatus = 'Unpublished (no action – never been submitted)'
                reportDetails.delta = 0

                data << reportDetails
            }
        }
        sheet.add(data, reportSummaryProperties, row + 1)
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
