package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.ActivityFormService
import au.org.ala.ecodata.DateUtil
import au.org.ala.ecodata.ExcelImportService
import au.org.ala.ecodata.FormSection
import au.org.ala.ecodata.ManagementUnit
import au.org.ala.ecodata.ManagementUnitService
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.ReportingService
import au.org.ala.ecodata.UserService
import au.org.ala.ecodata.util.ExportTestUtils
import grails.util.Holders
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class ManagementUnitXlsExporterSpec extends Specification implements GrailsUnitTest {

    def metadataService = Mock(MetadataService)
    def userService = Mock(UserService)
    def reportingService = Mock(ReportingService)
    def xlsExporter
    ManagementUnitService managementUnitService = Stub(ManagementUnitService)
    ManagementUnitXlsExporter managementUnitXlsExporter
    ExcelImportService excelImportService
    ActivityFormService activityFormService = Mock(ActivityFormService)

    File outputFile

    void setup() {
        Holders.grailsApplication = grailsApplication
        defineBeans {
            metadataService(MetadataService)
            userService(UserService)
            reportingService(ReportingService)
            activityFormService(ActivityFormService)
        }
        outputFile = File.createTempFile('test', '.xlsx')
        String name = outputFile.absolutePath
        outputFile.delete() // The exporter will attempt to load the file if it exists, but we want a random file name.
        xlsExporter = new XlsExporter(name)
        managementUnitXlsExporter = new ManagementUnitXlsExporter(xlsExporter)
        managementUnitXlsExporter.activityFormService = activityFormService
        managementUnitXlsExporter.metadataService = Mock(MetadataService)
        excelImportService = new ExcelImportService()
        managementUnitService.get("mu1") >> new ManagementUnit(managementUnitId:"mu1", name:"Management Unit 1")
    }

    void teardown() {
        outputFile.delete()
    }

    void "Management unit reports can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "Core Services Annual Report"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "singleNestedDataModel")
        Map mu = managementUnit()
        Date startDate = DateUtil.parse('2021-03-31T13:00:00Z')
        Date endDate = DateUtil.parse('2021-06-30T14:00:00Z')
        mu.activities = [[type: activityToExport, description: activityToExport, progress:'finished', lastUpdated:'2021-08-12T14:00:00Z', formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("singleSampleNestedDataModel")]]]
        mu.reports = [new Report([reportId:'r1', name:'Report 1', description:'Report 1 description', fromDate:startDate, toDate:endDate, publicationStatus:'published'])]

        when:
        managementUnitXlsExporter.tabsToExport = [activityToExport]
        managementUnitXlsExporter.export([mu])
        xlsExporter.save()

        Workbook workbook = ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There is a header row and 2 data rows"
        activitySheet.physicalNumberOfRows == 5

        and: "The first header row contains the property names from the activity form"
        List headers =  ExportTestUtils.readRow(0, activitySheet)
        headers == managementUnitXlsExporter.commonActivityHeaders.collect{''} + ["Single Nested lists.outputNotCompleted", "Single Nested lists.number1", "Single Nested lists.list.value1", "Single Nested lists.list.afterNestedList", "Single Nested lists.notes"]

        and: "The second header row contains the version the property was introduced in"
        ExportTestUtils.readRow(1, activitySheet) == managementUnitXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
        ExportTestUtils.readRow(2, activitySheet) == managementUnitXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "After list", "Notes"]

        and: "The management unit and report data is included"
        List muData = ExportTestUtils.readRow(3, activitySheet).subList(0, managementUnitXlsExporter.commonActivityHeaders.size())
        muData == ['mu1', 'Test MU', 'r1', 'Report 1', 'Report 1 description', startDate, endDate, '2020/2021', 'Unpublished (no action – never been submitted)', '', '', 'Core Services Annual Report', 'Core Services Annual Report', 'finished', '2021-08-12T14:00:00Z']

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(managementUnitXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "single.0.value1", "", "single notes"]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(managementUnitXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "single.1.value1", "", ""]

    }

    void "Management unit summary reports can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "Core Services Annual Report"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "singleNestedDataModel")
        Map mu = managementUnit()
        Date startDate = DateUtil.parse('2021-03-31T13:00:00Z')
        Date endDate = DateUtil.parse('2021-06-30T14:00:00Z')
        mu.activities = [[type: activityToExport, description: activityToExport, progress:'finished', lastUpdated:'2021-08-12T14:00:00Z', formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("singleSampleNestedDataModel")]]]
        mu.reports = [new Report([reportId:'r1', name:'Report 1', description:'Report 1 description', fromDate:startDate, toDate:endDate, publicationStatus:'published'])]

        when:
        managementUnitXlsExporter.tabsToExport = [activityToExport]
        managementUnitXlsExporter.export([mu], true)
        xlsExporter.save()

        Workbook workbook = ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        0 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There is a header row and 2 data rows"
        activitySheet.physicalNumberOfRows == 5

        and: "The first header row contains the property names from the activity form"
        List headers =  ExportTestUtils.readRow(0, activitySheet)
        headers == managementUnitXlsExporter.commonActivityHeadersSummary.collect{''}

        and: "The second header row contains the version the property was introduced in"
        ExportTestUtils.readRow(1, activitySheet) == managementUnitXlsExporter.commonActivityHeadersSummary.collect{''}

        and: "The third header row contains the labels from the activity form"
        ExportTestUtils.readRow(2, activitySheet) == managementUnitXlsExporter.commonActivityHeadersSummary

        and: "The management unit and report data is included"
        List muData = ExportTestUtils.readRow(3, activitySheet).subList(0, managementUnitXlsExporter.commonActivityHeadersSummary.size())
        muData == ['mu1', 'Test MU', 'r1', 'Report 1', 'Report 1 description', startDate, endDate, '2020/2021', 'Unpublished (no action – never been submitted)', '', '']

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(managementUnitXlsExporter.commonActivityHeadersSummary.size(), headers.size())
        dataRow1 == []
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(managementUnitXlsExporter.commonActivityHeadersSummary.size(), headers.size())
        dataRow2 == []

    }

    private ActivityForm createActivityForm(String name, int formVersion, String... templateFileName) {
        ActivityForm activityForm = new ActivityForm(name: name, formVersion: formVersion)
        templateFileName.each {
            Map formTemplate = ExportTestUtils.getJsonResource(it)
            activityForm.sections << new FormSection(name: formTemplate.modelName, template: formTemplate)
        }

        activityForm
    }

    private Map managementUnit() {
        [name:'Test MU', managementUnitId:'mu1']
    }
}
