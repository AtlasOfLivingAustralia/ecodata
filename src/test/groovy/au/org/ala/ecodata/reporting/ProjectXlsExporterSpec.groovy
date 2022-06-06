package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.util.ExportTestUtils
import grails.util.Holders
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

import java.time.ZoneId

/**
 * Spec for the ProjectXlsExporter
 */
class ProjectXlsExporterSpec extends Specification implements GrailsUnitTest {

    def projectService = Mock(ProjectService)
    def metadataService = Mock(MetadataService)
    def userService = Mock(UserService)
    def reportingService = Mock(ReportingService)
    def xlsExporter
    ManagementUnitService managementUnitService = Stub(ManagementUnitService)
    ProjectXlsExporter projectXlsExporter
    ExcelImportService excelImportService
    ActivityFormService activityFormService = Mock(ActivityFormService)
    OrganisationService organisationService = Mock(OrganisationService)
    ProgramService programService = Mock(ProgramService)

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
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [], [], managementUnitService, [:], true, organisationService, programService)
        projectXlsExporter.activityFormService = activityFormService
        projectXlsExporter.metadataService = Mock(MetadataService)
        excelImportService = new ExcelImportService()
        managementUnitService.get("mu1") >> new ManagementUnit(managementUnitId:"mu1", name:"Management Unit 1")
    }

    void teardown() {
        outputFile.delete()
    }

    void "project details can be exported"() {
        setup:
        String sheet = 'Projects'
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId: '1234', workOrderId: 'work order 1', status: "active", contractStartDate: '2019-06-30T14:00:00Z', contractEndDate: '2022-06-30T14:00:00Z', funding: 1000, managementUnitId:"mu1"])
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, sheet, projectXlsExporter.projectHeaders, excelImportService)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Work order id'] == 'work order 1'
        results[0]['Contracted Start Date'] == '2019-06-30T14:00:00Z'
        results[0]['Contracted End Date'] == '2022-06-30T14:00:00Z'
        results[0]['Funding'] == 1000
        results[0]['Management Unit'] == "Management Unit 1"
        results[0]['Status'] == "active"

    }


    void "project details can be exported with Termination Reason"() {
        setup:
        String sheet = 'Projects'
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId: '1234', workOrderId: 'work order 1', contractStartDate: '2019-06-30T14:00:00Z', contractEndDate: '2022-06-30T14:00:00Z', funding: 1000, managementUnitId:"mu1", status: "Terminated", terminationReason: "Termination Reason"])
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, "Projects", projectXlsExporter.projectHeaders, excelImportService)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Work order id'] == 'work order 1'
        results[0]['Contracted Start Date'] == '2019-06-30T14:00:00Z'
        results[0]['Contracted End Date'] == '2022-06-30T14:00:00Z'
        results[0]['Funding'] == 1000
        results[0]['Management Unit'] == "Management Unit 1"
        results[0]["Status"] == "Terminated"
        results[0]["Termination Reason"] == "Termination Reason"

    }

    void "Projects don't have to have a managemeent unit id to be exported correctly"() {
        setup:
        String sheet = 'Projects'
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId: '1234', workOrderId: 'work order 1', internalOrderId:'1234567890', contractStartDate: '2019-06-30T14:00:00Z', status: "active", contractEndDate: '2022-06-30T14:00:00Z', funding: 1000])
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, sheet, projectXlsExporter.projectHeaders, excelImportService)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Internal order number'] == '1234567890'
        results[0]['Work order id'] == 'work order 1'
        results[0]['Contracted Start Date'] == '2019-06-30T14:00:00Z'
        results[0]['Contracted End Date'] == '2022-06-30T14:00:00Z'
        results[0]['Funding'] == 1000
        results[0]['Management Unit'] == ""

    }

    void "Dataset data can be exported"() {
        setup:
        String sheet = "Dataset"
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = projectDataSet()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, "Data_set_Summary", projectXlsExporter.datasetHeader, excelImportService)
        results.size() == 1
        results[0]['Project ID'] == '1dda8202-cbf1-45d8-965c-9b93306aaeaf'
        results[0]["What primary or secondary investment priorities or assets does this dataset relate to?"] == "Testing, Other"
        results[0]['Describe the method used to collect the data in detail'] == 'Testing'
        results[0]['Dataset Title'] == "Testing Data Set"
        results[0]["Primary source of data (organisation or individual that owns or maintains the dataset)"] == "na"
        results[0]["Other Investment Priority"] == "Other Priorities, other priorities"
        results[0]["Progress"] == "started"
        results[0]["Is this data being collected for reporting against short or medium term outcome statements?"] == "Short-term outcome statement"
    }

    void "RLP outcomes data can be exported"() {
        setup:
        String sheet = "RLP_Outcomes"
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = rlpProject()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, "RLP Outcomes", projectXlsExporter.rlpOutcomeHeaders, excelImportService)
        results.size() == 5
        results[1]['Outcome'] == 'More primary producers preserve natural capital while also improving productivity and profitability,More primary producers adopt risk management practices to improve their sustainability and resilience,More primary producers and agricultural communities are experimenting with adaptive or transformative NRM practices, systems and approaches that link and contribute to building drought resilience,Partnerships and engagement is built between stakeholders responsible for managing natural resources'
        results[1]["Type of outcomes"] == "Other Outcomes"
        results[0]["Type of outcomes"] == "Primary outcome"
        results[0]['Outcome'] == "5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation."
        results[0]["Investment Priority"] == "Hillslope erosion, Wind erosion"
    }

    void "RLP Merit Baseline exported to XSLS"() {
        setup:
        String sheet = 'MERI_Baseline'
        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, sheet, projectXlsExporter.baselineHeaders, excelImportService)
        results[0]['Baseline'] == 'Test'
        results[0]['Baseline Method'] == 'Test'
        results[1]['Baseline'] == 'Test2'
        results[1]['Baseline Method'] == 'Test1'
    }

    void "MERI plan assets can be exported to XLSX"() {
        setup:
        String sheet = 'MERI_Project Assets'
        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        outputFile.withInputStream { fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            Sheet testSheet = workbook.getSheet(sheet)
            testSheet.physicalNumberOfRows == 3

            Cell assetCell = testSheet.getRow(0).find { it.stringCellValue == 'Asset' }
            Cell categoryCell = testSheet.getRow(0).find { it.stringCellValue == 'Category' }
            testSheet.getRow(1).getCell(assetCell.getColumnIndex()).stringCellValue == 'Asset 1'
            testSheet.getRow(1).getCell(categoryCell.getColumnIndex()).stringCellValue == 'Category 1'

        }

    }

    void "Electorate Coord data can be exported"() {
        setup:
        String sheet = "Electorate Coord"
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, "Electorate Coord", projectXlsExporter.electorateCoordHeaders, excelImportService)
        results.size() == 1
        results[0]['Primary State'] == 'ACT'
        results[0]['Primary Electorate'] == 'Canberra'
        results[0]['Other States'] == 'NSW'
        results[0]['Other Electorates'] == 'Taylor'
        results[0]['Internal order number'] == '1234-1'
        results[0]['Internal order number 2'] == '1234-2'
        results[0]['GO ID'] == 'g-1'
        results[0]['Work order id'] == 'w-1'
        results[0]['Tech One Project Code'] == 't-1'
        results[0]['Tech One Project Code 2'] == 't-2'
    }

    void "Native Species Threat can be exported"() {
        setup:
        String sheet = "MERI_Native Species Threat"
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, "Native Species Threat", projectXlsExporter.nativeThreatsHeaders, excelImportService)
        results.size() == 1
        results[0]['Could this control approach pose a threat to Native Animals/Plants or Biodiversity?'] == 'Yes'
        results[0]['Details'] == 'Test yes details'
    }

    void "Pest Control Methods can be exported"() {
        setup:
        String sheet = "MERI_Pest Control Methods"
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:], organisationService, programService)
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = ExportTestUtils.readSheet(outputFile, "Pest Control Methods", projectXlsExporter.pestControlMethodsHeaders, excelImportService)
        results.size() == 1
        results[0]['Type of method'] == 'Natural'
        results[0]['Has it been successful?'] == 'Yes'
        results[0]['Are there any current control methods for this pest?'] == 'Test'
        results[0]['Details'] == 'Test'
    }

    void "RLP Merit approvals exported to XSLS"() {
        setup:
        String sheet = 'MERI_Approvals'
        Map recentApproval = [
                approvalDate     : "2018-10-23T23:47:28.263Z",
                approvedBy       : "Test User",
                comment          : "Test purpose",
                changeOrderNumber: "Test 2"
        ]
        List approvals = [recentApproval]

        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        1 * projectService.getMostRecentMeriPlanApproval(_) >> recentApproval
        1 * projectService.getMeriPlanApprovalHistory(_) >> approvals

        outputFile.withInputStream { fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            Sheet testSheet = workbook.getSheet(sheet)
            testSheet.physicalNumberOfRows == 2

            Cell approvedDateCell = testSheet.getRow(0).find { it.stringCellValue == 'Date / Time Approved' }
            approvedDateCell != null
            testSheet.getRow(1).getCell(approvedDateCell.getColumnIndex()).stringCellValue == '2018-10-23T23:47:28.263Z'

            Cell conDateCell = testSheet.getRow(0).find { it.stringCellValue == 'Change Order Numbers' }
            conDateCell != null
            testSheet.getRow(1).getCell(conDateCell.getColumnIndex()).stringCellValue == 'Test 2'

        }


    }

    void "Activities can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "singleNestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("singleSampleNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
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
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.afterNestedList", "notes"]

        and: "The second header row contains the version the property was introduced in"
         ExportTestUtils.readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
         ExportTestUtils.readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "After list", "Notes"]


        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "single.0.value1", "", "single notes"]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "single.1.value1", "", ""]

    }

    void "String lists can be expanded into a column per value"() {
        setup:
        String activityToExport = "String lists"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "dataModelWithStringList")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleDataModelWithStringList")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

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
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "stringList1[c1]", "stringList1[c2]", "stringList1[c3]", "list.stringList2[c4]", "list.stringList2[c5]", "list.stringList2[c6]", "list.afterNestedList", "notes"]

        and: "The second header row contains the version the property was introduced in"
         ExportTestUtils.readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1,1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
         ExportTestUtils.readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "String list 1 - c1", "String list 1 - c2", "String list 1 - c3",  "String list 2 - c4",  "String list 2 - c5",  "String list 2 - c6", "After list", "Notes"]


        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "c1", "", "c3", "c4", "c5", "", "", "single notes"]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "c1", "", "c3", "", "", "", "single.1.value1", ""]

    }


    void "Activities with deeply nested data can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "nestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There are 3 header rows and 5 data rows"
        activitySheet.physicalNumberOfRows == 8

        and: "The header row contains the labels from the activity form"
        List headers =  ExportTestUtils.readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.afterNestedList", "notes"]
         ExportTestUtils.readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1,1]
         ExportTestUtils.readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "3", "0.value1", "0.0.value2", "", "notes"]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "", "0.1.value2", "", ""]
        List dataRow3 =  ExportTestUtils.readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "1.value1", "1.0.value2", "", ""]
        List dataRow4 =  ExportTestUtils.readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "1.1.value2", "", ""]
        List dataRow5 =  ExportTestUtils.readRow(7, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "", "1.2.value2", "", ""]

    }

    void "Activities with 3 levels of nested data can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "deeplyNestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleDeeplyNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There are 3 header rows and 6 data rows"
        activitySheet.physicalNumberOfRows == 9

        and: "The header row contains the labels from the activity form"
        List headers =  ExportTestUtils.readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.nestedList.nestedNestedList.value3", "list.afterNestedList", "notes"]
         ExportTestUtils.readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1, 1, 1, 1, 1, 1]
         ExportTestUtils.readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "Value 3", "After list", "Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "3", "0.value1", "0.0.value2", "3", "", "notes"]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "0.value1", "0.0.value2", "4", "", ""]
        List dataRow3 =  ExportTestUtils.readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "0.value1", "0.1.value2", "", "", ""]
        List dataRow4 =  ExportTestUtils.readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "1.value1", "1.0.value2", "", "", ""]
        List dataRow5 =  ExportTestUtils.readRow(7, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "1.value1", "1.1.value2", "", "", ""]
        List dataRow6 =  ExportTestUtils.readRow(8, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow6 == ["", "", "1.value1", "1.2.value2", "", "", ""]

    }

    void "Data created from different versions of the same activity form will be exported to the same sheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "nestedDataModel")
        ActivityForm activityForm_v2 = ExportTestUtils.createActivityForm(activityToExport, 2, "nestedDataModel_v2")
        Map project = project()
        project.activities = [
                [type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleNestedDataModel")]],
                [type: activityToExport, name: activityToExport, formVersion: activityForm_v2.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleNestedDataModel_v2")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findActivityForm(activityToExport, 2) >> activityForm_v2
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm, activityForm_v2]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There are 3 header rows and 5 data rows"
        activitySheet.physicalNumberOfRows == 12

        and: "The header row contains the labels from the activity form"
        List headers =  ExportTestUtils.readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.afterNestedList", "notes", "list.nestedList.value3", "extraNotes"]
         ExportTestUtils.readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1,1, 2, 2]
         ExportTestUtils.readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes", "Value 3", "Extra Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "3", "0.value1", "0.0.value2", "", "notes", "", ""]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "", "0.1.value2", "", "", "", ""]
        List dataRow3 =  ExportTestUtils.readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "1.value1", "1.0.value2", "", "", "", ""]
        List dataRow4 =  ExportTestUtils.readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "1.1.value2", "", "", "", ""]
        List dataRow5 =  ExportTestUtils.readRow(7, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "", "1.2.value2", "", "", "", ""]

        List dataRow7 =  ExportTestUtils.readRow(8, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow7 == ["", "3", "0.value1", "", "", "notes", "0.0.value3", "extra notes"]
        List dataRow8 =  ExportTestUtils.readRow(9, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow8 == ["", "", "", "", "", "", "0.1.value3", ""]
        List dataRow9 =  ExportTestUtils.readRow(10, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow9 == ["", "", "1.value1", "", "", "", "1.0.value3", ""]
        List dataRow10 =  ExportTestUtils.readRow(11, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow10 == ["", "", "", "", "", "", "1.1.value3", ""]
    }

    void "Versioning of values in constraints are handled correctly"() {
        setup:
        String activityToExport = "String lists"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "dataModelWithStringList")
        ActivityForm activityForm_v2 = ExportTestUtils.createActivityForm(activityToExport, 2, "dataModelWithStringListv2")

        Map project = project()
        project.activities = [
                [type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleDataModelWithStringList")]],
                [type: activityToExport, name: activityToExport, formVersion: activityForm_v2.formVersion, outputs: [ExportTestUtils.getJsonResource("sampleDataModelWithStringListv2")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findActivityForm(activityToExport, 2) >> activityForm_v2

        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm, activityForm_v2]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There is a header row and 2 data rows"
        activitySheet.physicalNumberOfRows == 7

        and: "The first header row contains the property names from the activity form"
        List headers =  ExportTestUtils.readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "stringList1[c1]", "stringList1[c2]", "stringList1[c3]", "list.stringList2[c4]", "list.stringList2[c5]", "list.stringList2[c6]", "list.afterNestedList", "notes", "stringList1[c4]"]

        and: "The second header row contains the version the property was introduced in"
         ExportTestUtils.readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1,1,1,1,1,1,2]

        and: "The third header row contains the labels from the activity form"
         ExportTestUtils.readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "String list 1 - c1", "String list 1 - c2", "String list 1 - c3",  "String list 2 - c4",  "String list 2 - c5",  "String list 2 - c6", "After list", "Notes", "String list 1 - c4"]


        and: "The data in the subsequent rows matches the data in the activity v1"
        List dataRow1 =  ExportTestUtils.readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "c1", "", "c3", "c4", "c5", "", "", "single notes", ""]
        List dataRow2 =  ExportTestUtils.readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "c1", "", "c3", "", "", "", "single.1.value1", "", ""]

        and: "The data in the subsequent rows matches the data in the activity v2"
        List dataRow3 =  ExportTestUtils.readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "33", "", "", "c3", "", "c5", "", "", "single notes", "c4"]
        List dataRow4 =  ExportTestUtils.readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "", "c3", "", "", "", "single.1.value1", "", "c4"]

    }

    void "Each form section / output can be exported to a separate tab"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "singleNestedDataModel", "nestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion,
                               outputs: [ExportTestUtils.getJsonResource("singleSampleNestedDataModel"),
                                         ExportTestUtils.getJsonResource("sampleNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        2 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There are two sheets exported, one per form section"
        workbook.numberOfSheets == 2
        Sheet section1 = workbook.getSheetAt(0)
        Sheet section2 = workbook.getSheetAt(1)
        section1.sheetName == "Single Nested lis...nual Report"
        section2.sheetName == "Nested lists RLP Annual Report"

        and: "There are 3 header rows and data rows for each section"
        section1.physicalNumberOfRows == 5
        section2.physicalNumberOfRows == 8

        and: "The first header row contains the property names from the activity form"
        List headers1 =  ExportTestUtils.readRow(0, section1)
        headers1 == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.afterNestedList", "notes"]
        List headers2 =  ExportTestUtils.readRow(0, section2)
        headers2 == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.afterNestedList", "notes"]

        and: "The second header row contains the version the property was introduced in"
         ExportTestUtils.readRow(1, section1) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1]
         ExportTestUtils.readRow(1, section2) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
         ExportTestUtils.readRow(2, section1) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "After list", "Notes"]
         ExportTestUtils.readRow(2, section2) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes"]


        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, section1).subList(projectXlsExporter.commonActivityHeaders.size(), headers1.size())
        dataRow1 == ["", "33", "single.0.value1", "", "single notes"]
        List dataRow2 =  ExportTestUtils.readRow(4, section1).subList(projectXlsExporter.commonActivityHeaders.size(), headers1.size())
        dataRow2 == ["", "", "single.1.value1", "", ""]

        and: "The data in the second form section output rows matches the data in the activity"
        List s2dataRow1 =  ExportTestUtils.readRow(3, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow1 == ["", "3", "0.value1", "0.0.value2", "", "notes"]
        List s2dataRow2 =  ExportTestUtils.readRow(4, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow2 == ["", "", "", "0.1.value2", "", ""]
        List s2dataRow3 =  ExportTestUtils.readRow(5, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow3 == ["", "", "1.value1", "1.0.value2", "", ""]
        List s2dataRow4 =  ExportTestUtils.readRow(6, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow4 == ["", "", "", "1.1.value2", "", ""]
        List s2dataRow5 =  ExportTestUtils.readRow(7, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow5 == ["", "", "", "1.2.value2", "", ""]

    }

    void "Each form section / output can optionally be exported to the same tab"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = ExportTestUtils.createActivityForm(activityToExport, 1, "singleNestedDataModel", "nestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion,
                               outputs: [ExportTestUtils.getJsonResource("singleSampleNestedDataModel"),
                                         ExportTestUtils.getJsonResource("sampleNestedDataModel")]]]

        when:
        projectXlsExporter.formSectionPerTab = false
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is only one sheet exported containing both form sections"
        workbook.numberOfSheets == 1
        Sheet sheet = workbook.getSheetAt(0)
        sheet.sheetName == "RLP Annual Report"

        and: "There are 3 header rows and 8 data rows"
        sheet.physicalNumberOfRows == 10

        and: "The first header row contains the namespaced property names from the activity form"
        List headers =  ExportTestUtils.readRow(0, sheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["Single Nested lists.outputNotCompleted", "Single Nested lists.number1", "Single Nested lists.list.value1", "Single Nested lists.list.afterNestedList", "Single Nested lists.notes", "Nested lists.outputNotCompleted", "Nested lists.number1", "Nested lists.list.value1", "Nested lists.list.nestedList.value2", "Nested lists.list.afterNestedList", "Nested lists.notes"]

        and: "The second header row contains the version the property was introduced in"
         ExportTestUtils.readRow(1, sheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1, 1, 1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
         ExportTestUtils.readRow(2, sheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "After list", "Notes", "Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 =  ExportTestUtils.readRow(3, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "single.0.value1", "", "single notes", "", "", "", "", "", ""]
        List dataRow2 =  ExportTestUtils.readRow(4, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == [ "", "", "single.1.value1", "", "", "", "", "", "", "", ""]
        List dataRow3 =  ExportTestUtils.readRow(5, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "", "", "", "", "3", "0.value1", "0.0.value2", "", "notes"]
        List dataRow4 =  ExportTestUtils.readRow(6, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "", "", "", "", "", "0.1.value2", "", ""]
        List dataRow5 =  ExportTestUtils.readRow(7, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "", "", "", "", "", "1.value1", "1.0.value2", "", ""]
        List dataRow6 =  ExportTestUtils.readRow(8, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow6 == ["", "", "", "", "","", "", "", "1.1.value2", "", ""]
        List dataRow7 =  ExportTestUtils.readRow(9, sheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow7 == ["", "", "", "", "","", "", "", "1.2.value2", "", ""]

    }

    def "A summary of project activities can be outputted"() {
        setup:

        Map project = project()
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")))
        calendar.set(Calendar.YEAR, 2020)
        calendar.set(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        Date endDate = calendar.getTime()

        calendar.set(Calendar.YEAR, 2019)
        calendar.set(Calendar.MONTH, 9)
        Date startDate = calendar.getTime()

        project.activities = [
                [activityId:'a1', type: "Activity 1", description: "Activity 1 description", formVersion: 1, plannedStartDate:startDate, plannedEndDate: endDate,outputs: [ExportTestUtils.getJsonResource("singleSampleNestedDataModel"),  ExportTestUtils.getJsonResource("sampleNestedDataModel")]]]
        project.reports = [
                new Report([reportId:'r1', activityType:'Activity 1', activityId:'a1', type:"Activity", generatedBy:"Test config", name:"Report 1", fromDate: startDate, toDate:endDate, description:"Report 1 description"])
        ]
        when:
        projectXlsExporter.tabsToExport = ["Activity Summary"]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        workbook.numberOfSheets == 1
        Sheet summarySheet = workbook.getSheet("Activity Summary")
        summarySheet.physicalNumberOfRows == 2
        List summaryRow =  ExportTestUtils.readRow(1, summarySheet)
        List activityInfo = summaryRow.subList(projectXlsExporter.commonProjectHeaders.size(), summaryRow.size())
        activityInfo == ['a1', '', startDate, endDate, startDate, endDate, '2019/2020', 'Report 1', 'Test config', 'Activity 1 description', 'Activity 1', '', '', 'Unpublished (no action – never been submitted)', '']

    }



    private Map project() {
        new groovy.json.JsonSlurper().parseText(projectJson)
    }

    private Map rlpProject(){
        new groovy.json.JsonSlurper().parseText(rlpPlan)
    }
    private Map projectDataSet() {
        new groovy.json.JsonSlurper().parseText(projectDataSet)
    }

    private String projectJson = """
        {
              "alaHarvest" : false, 
              "associatedProgram" : "",
              "associatedSubProgram" : "", 
              "status": "active",
              "countries" : [], 
              "custom" : { 
                  "details" : { 
                      "partnership" : { 
                          "description" : "", 
                          "rows" : [
                              { 
                                  "data3" : "", 
                                  "data2" : "", 
                                  "data1" : "" 
                              } 
                          ] 
                      }, 
                      "projectEvaluationApproach" : "Test", 
                      "implementation" : { 
                          "description" : "Test methodology" 
                      }, 
                      "obligations" : "", 
                      "policies" : "", 
                      "description" : "TBA - this is a temporary description", 
                      "baseline" : { 
                          "description" : "This is a baseline", 
                          "rows" : [  
                              { 
                                  "method" : "Test", 
                                  "baseline" : "Test" 
                              },
                              { 
                                  "method" : "Test1", 
                                  "baseline" : "Test2" 
                              } 
                          ]
                      }, 
                      "rationale" : "Test rational", 
                      "caseStudy" : true, 
                      "lastUpdated" : "2019-06-06T06:07:27Z", 
                      "priorities" : { 
                          "description" : "", 
                          "rows" : [
                              { 
                                  "data3" : "Test", 
                                  "data2" : "Test", 
                                  "data1" : "Test" 
                              } 
                          ]
                      }, 
                      "serviceIds" : [ 
                          1,  
                          2, 
                          3, 
                          4, 
                          5, 
                          34, 
                          6, 
                          7, 
                          8, 
                          10,
                          9,
                          11,
                          12,
                          13,
                          14,
                          15,
                          16,
                          17,
                          18,
                          19,
                          20,
                          21,
                          22,
                          23, 
                          24,
                          25, 
                          26, 
                          27, 
                          28, 
                          35, 
                          29, 
                          30, 
                          31, 
                          32, 
                          33
                      ], 
                      "outcomes" : { 
                          "secondaryOutcomes" : [
                              { 
                                  "assets" : [
                                      "Soil carbon" 
                                  ], 
                                  "description" : "By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation." 
                              },
                              { 
                                  "assets" : [
                                      "Natural Temperate Grassland of the South Eastern Highlands" 
                                  ], 
                                  "description" : "By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities." 
                              } 
                          ], 
                          "shortTermOutcomes" : [
                              { 
                                  "assets" : [ 
                                      "Asset 1", 
                                      "Assert2" 
                                  ], 
                                  "description" : "Test" 
                              },
                              { 
                                  "assets" : [
                                      "Asset3" 
                                  ], 
                                  "description" : "Test 2" 
                              },
                              { 
                                  "assets" : [], 
                                  "description" : "sfasdf" 
                              } 
                          ], 
                          "midTermOutcomes" : [
                              { 
                                  "assets" : [ 
                                      "Asset 1", 
                                      "Assert2" 
                                  ], 
                                  "description" : "Test" 
                              }, 
                              { 
                                  "assets" : [], 
                                  "description" : "" 
                              } 
                          ], 
                          "primaryOutcome" : { 
                              "assets" : [
                                  "Climate change adaptation", 
                                  "Market traceability" 
                              ], 
                              "description" : "By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production." 
                          } 
                      }, 
                      "keq" : { 
                          "description" : "", 
                          "rows" : [ 
                              { 
                                  "data3" : "", 
                                  "data2" : "Test", 
                                  "data1" : "*** This is a monitoring indictor ** " 
                              } 
                          ]
                      }, 
                      "threats" : { 
                          "description" : "", 
                          "rows" : [ 
                              { 
                                  "threat" : "Test this is another EDIT", 
                                  "intervention" : "Test" 
                              } 
                          ] 
                      }, 
                      "objectives" : { 
                          "rows1" : [
                              { 
                                  "assets" : [], 
                                  "description" : "" 
                              } 
                          ], 
                          "rows" : [
                              { 
                                  "data3" : "", 
                                  "data2" : "", 
                                  "data1" : "" 
                              } 
                          ]
                      }, 
                      "events" : [
                          { 
                              "funding" : "0", 
                              "name" : "", 
                              "description" : "", 
                              "scheduledDate" : "", 
                              "media" : "", 
                              "grantAnnouncementDate" : "", 
                              "type" : "" 
                          } 
                      ], 
                      "status" : "active", 
                      "budget" : { 
                          "overallTotal" : 0, 
                          "headers" : [
                              { 
                                  "data" : "2017/2018" 
                              }, 
                              { 
                                  "data" : "2018/2019" 
                              } 
                          ], 
                          "rows" : [
                              { 
                                  "costs" : [
                                      { 
                                          "dollar" : "0" 
                                      }, 
                                      { 
                                          "dollar" : "0" 
                                      } 
                                  ], 
                                  "rowTotal" : 0, 
                                  "description" : "", 
                                  "shortLabel" : "" 
                              } 
                          ], 
                          "columnTotal" : [ 
                              { 
                                  "data" : 0
                              }, 
                              { 
                                  "data" : 0
                              } 
                          ]
                      }, 
                      "threatToNativeSpecies" : { 
                          "description" : "", 
                          "rows" : [ 
                              { 
                                  "couldBethreatToSpecies" : "Yes", 
                                  "details" : "Test yes details" 
                              } 
                          ]
                      }, 
                      "threatControlMethod" : { 
                          "description" : "", 
                          "rows" : [ 
                              { 
                                  "currentControlMethod" : "Test", 
                                  "details" : "Test", 
                                  "hasBeenSuccessful" : "Yes", 
                                  "methodType" : "Natural" 
                              } 
                          ]
                      }, 
                      "assets": [ 
                         {  "description":"Asset 1", "category":"Category 1" }
                      ]
                  } 
              }, 
              "dateCreated" : "2018-06-14T04:22:13.057Z", 
              "description" : "TBA - this is a temporary description", 
              "ecoScienceType" : [], 
              "externalId" : "", 
              "fundingSource" : "RLP", 
              "funding" : 10000, 
              "grantId" : "RLP-Test-Program-Project-1", 
              "industries" : [], 
              "bushfireCategories" : [], 
              "isCitizenScience" : false, 
              "isExternal" : false, 
              "isMERIT" : true, 
              "isSciStarter" : false, 
              "lastUpdated" : "2019-08-13T05:17:48.686Z", 
              "manager" : "", 
              "name" : "Test Program - Project 1", 
              "orgIdSvcProvider" : "", 
              "organisationId" : "", 
              "organisationName" : "Test Org", 
              "origin" : "merit", 
              "outputTargets" : [
                  { 
                      "scoreId" : "0df7c177-2864-4a25-b420-2cf3c45ce749", 
                      "periodTargets" : [
                          { 
                              "period" : "2017/2018", 
                              "target" : "2" 
                          },  
                          { 
                              "period" : "2018/2019", 
                              "target" : "2" 
                          } 
                      ], 
                      "target" : "2" 
                  },
                  { 
                      "scoreId" : "69deaaf9-cdc2-439a-b684-4cffdc7f224e", 
                      "periodTargets" : [ 
                          { 
                              "period" : "2017/2018", 
                              "target" : "1" 
                          }, 
                          { 
                              "period" : "2018/2019", 
                              "target" : "4" 
                          } 
                      ], 
                      "target" : "4" 
                  },
                  { 
                      "scoreId" : "26a8213e-1770-4dc4-8f99-7e6302197504", 
                      "periodTargets" : [ 
                          { 
                              "period" : "2017/2018", 
                              "target" : "1" 
                          }, 
                          { 
                              "period" : "2018/2019", 
                              "target" : "1" 
                          } 
                      ], 
                      "target" : "2" 
                  }
              ], 
              "planStatus" : "not approved", 
              "plannedEndDate" : "2019-06-29T14:00:00.000Z", 
              "plannedStartDate" : "2017-08-01T14:00:00.000Z", 
              "programId" : "test_program", 
              "projectId" : "8693cbc5-6947-4614-9bd1-b22ef44bc8fd", 
              "projectType" : "works", 
              "promoteOnHomepage" : "no", 
              "risks" : { 
                  "overallRisk" : "Low", 
                  "rows" : [ 
                      { 
                          "consequence" : "Minor", 
                          "likelihood" : "Unlikely", 
                          "residualRisk" : "Low", 
                          "currentControl" : "Test", 
                          "description" : "Low", 
                          "threat" : "Work Health and Safety", 
                          "riskRating" : "Low" 
                      }, 
                      { 
                          "consequence" : "Moderate", 
                          "likelihood" : "Unlikely", 
                          "residualRisk" : "High", 
                          "currentControl" : "Test", 
                          "description" : "Test 2", 
                          "threat" : "Performance", 
                          "riskRating" : "Low" 
                      }, 
                      { 
                          "consequence" : "Minor", 
                          "likelihood" : "Possible", 
                          "residualRisk" : "Medium", 
                          "currentControl" : "yep", 
                          "description" : "lalala", 
                          "threat" : "People resources", 
                          "riskRating" : "Low" 
                      },  
                      { 
                          "consequence" : "High", 
                          "likelihood" : "Possible", 
                          "residualRisk" : "Medium", 
                          "currentControl" : "yrd", 
                          "description" : "\$", 
                          "threat" : "Financial", 
                          "riskRating" : "Medium" 
                      } 
                  ], 
                  "status" : "" 
              }, 
              "scienceType" : [], 
              "serviceProviderName" : "", 
              "managementUnitId" : "mu1", 
              "status" : "active", 
              "tags" : [], 
              "uNRegions" : [], 
              "externalIds" : [
                    {
                      "idType":"INTERNAL_ORDER_NUMBER",
                      "externalId": "1234-1"
                    },
                    {
                      "idType":"INTERNAL_ORDER_NUMBER",
                      "externalId": "1234-2"
                    },
                    {
                      "idType":"WORK_ORDER",
                      "externalId": "w-1"
                    },
                    {
                      "idType":"GRANT_AWARD",
                      "externalId": "g-1"
                    },
                    {
                      "idType":"TECH_ONE_CODE",
                      "externalId": "t-1"
                    },
                    {
                      "idType":"TECH_ONE_CODE",
                      "externalId": "t-2"
                    }
              ],
              
              "blog" : [], 
              "geographicInfo" : { 
                  "primaryState" : "ACT", 
                  "primaryElectorate" : "Canberra", 
                  "otherStates" : ["NSW"], 
                  "otherElectorates" : ["Taylor"], 
              } 
            }"""

    private String projectDataSet = "{\"origin\":\"merit\",\"promoteOnHomepage\":\"no\",\"name\":\"Building the drought resilience of East Gippsland’s beef and sheep farms\",\"funding\":0,\"isCitizenScience\":false,\"uNRegions\":[],\"industries\":[],\"tags\":[\"\"],\"isBushfire\":false,\"alaHarvest\":false,\"isMERIT\":true,\"status\":\"active\",\"isSciStarter\":false,\"isExternal\":false,\"projectId\":\"1dda8202-cbf1-45d8-965c-9b93306aaeaf\",\"grantId\":\"FDF-MU24-P1\",\"projectType\":\"works\",\"description\":\"Test\",\"externalId\":\"\",\"serviceProviderName\":\"\",\"organisationName\":\"RLP East Gippsland Catchment Management Authority\",\"internalOrderId\":\"TBA\",\"workOrderId\":\"TBA\",\"programId\":\"08335f58-63d0-42e1-a852-2ba5c3a083ed\",\"planStatus\":\"not approved\",\"abn\":\"\",\"associatedSubProgram\":\"Natural Resource Management - Landscape\",\"organisationId\":\"\",\"manager\":\"\",\"orgIdSvcProvider\":\"\",\"associatedProgram\":\"Future Drought Fund\",\"custom\":{\"dataSets\":[{\"owner\":\"na\",\"methodDescription\":\"Testing\",\"custodian\":\"na\",\"investmentPriorities\":[\"Testing\",\"Other\"],\"endDate\":\"2021-02-04T13:00:00Z\",\"methods\":[\"Hair, track, dung sampling\",\"Area sampling\"],\"format\":\"JSON\",\"published\":\"No\",\"sensitivities\":[\"Indigenous/cultural\",\"Commercially sensitive\"],\"type\":\"Baseline dataset associated with a project outcome\",\"collectionApp\":\"Test\",\"collectorType\":\"Specialist consultant\",\"qa\":\"Yes\",\"otherInvestmentPriority\":\"Other Priorities, other priorities\",\"progress\":\"started\",\"term\":\"Short-term outcome statement\",\"dataSetId\":\"967fd2e8-8621-49c2-99ac-861828f752ce\",\"name\":\"Testing Data Set\",\"measurementTypes\":[\"Adoption - climate and market demands\",\"Adoption - land resource management practices\"],\"storageType\":\"Cloud\",\"location\":\"test\",\"programOutcome\":\"5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.\",\"publicationUrl\":\"ttt\",\"startDate\":\"2021-02-03T13:00:00Z\",\"addition\":\"No\"}]}}"

    private String rlpPlan = "{\"bushfireCategories\":[],\"origin\":\"merit\",\"promoteOnHomepage\":\"no\",\"ecoScienceType\":[],\"countries\":[],\"name\":\"Building the drought resilience of East Gippsland’s beef and sheep farms\",\"funding\":0,\"isCitizenScience\":false,\"uNRegions\":[],\"industries\":[],\"tags\":[\"\"],\"isBushfire\":false,\"alaHarvest\":false,\"scienceType\":[],\"isMERIT\":true,\"status\":\"application\",\"isSciStarter\":false,\"isExternal\":false,\"projectId\":\"5a80f409-0b27-480a-9938-340aa48cc947\",\"grantId\":\"FDF-MU24-P1\",\"projectType\":\"works\",\"description\":\"The project will work with beef and sheep farmers to build their knowledge and understanding of the options for using different pasture/forage crop varieties to manage drought and build resilience into their farming operations.  A key strength of the project is that during the most recent drought there were local farmers who managed to retain groundcover and pastures, retain their core breeding stock, and will recover more quickly once drought conditions ease. This project will draw on those lessons to co-design local research trials of plant varieties and to demonstrate drought adaptation practices through peer learning. \\nThe project design and delivery is farmer-led, with local facilitation, through resourcing of existing trusted facilitators and contractors. It also involves collaboration with subject experts (consultants and researchers) to ensure the best science and measurable results are achieved from the project.\",\"externalId\":\"\",\"managementUnitId\":\"82c15908-04ab-4e49-bd43-9616a6dcb528\",\"serviceProviderName\":\"\",\"organisationName\":\"RLP East Gippsland Catchment Management Authority\",\"internalOrderId\":\"\",\"workOrderId\":\"TBA\",\"programId\":\"08335f58-63d0-42e1-a852-2ba5c3a083ed\",\"planStatus\":\"not approved\",\"abn\":\"\",\"associatedSubProgram\":\"\",\"custom\":{\"details\":{\"communityEngagement\":\"\",\"obligations\":\"\",\"policies\":\"\",\"description\":\"The project will work with beef and sheep farmers to build their knowledge and understanding of the options for using different pasture/forage crop varieties to manage drought and build resilience into their farming operations.  A key strength of the project is that during the most recent drought there were local farmers who managed to retain groundcover and pastures, retain their core breeding stock, and will recover more quickly once drought conditions ease. This project will draw on those lessons to co-design local research trials of plant varieties and to demonstrate drought adaptation practices through peer learning. \\nThe project design and delivery is farmer-led, with local facilitation, through resourcing of existing trusted facilitators and contractors. It also involves collaboration with subject experts (consultants and researchers) to ensure the best science and measurable results are achieved from the project.\",\"lastUpdated\":\"2021-02-04T23:57:56Z\",\"priorities\":{\"description\":\"\",\"rows\":[{\"data3\":\"The project will work with beef and sheep farmer\",\"data2\":\"The project will work with beef and sheep farmer\",\"data1\":\"The project will work with beef and sheep farmer\"}]},\"assets\":[{\"description\":\"\",\"category\":\"\"}],\"outcomes\":{\"secondaryOutcomes\":[{}],\"otherOutcomes\":[\"More primary producers preserve natural capital while also improving productivity and profitability\",\"More primary producers adopt risk management practices to improve their sustainability and resilience\",\"More primary producers and agricultural communities are experimenting with adaptive or transformative NRM practices, systems and approaches that link and contribute to building drought resilience\",\"Partnerships and engagement is built between stakeholders responsible for managing natural resources\"],\"shortTermOutcomes\":[{\"assets\":[],\"description\":\"Test\"}],\"midTermOutcomes\":[{\"assets\":[],\"description\":\"test\"}],\"primaryOutcome\":{\"assets\":[\"Hillslope erosion\",\"Wind erosion\"],\"description\":\"5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.\"}},\"programName\":\"Natural Resource Management - Landscape\",\"keq\":{\"description\":\"\",\"rows\":[{\"data3\":\"\",\"data2\":\"\",\"data1\":\"\"},{\"data3\":\"\",\"data2\":\"\",\"data1\":\"\"}]},\"threats\":{\"description\":\"\",\"rows\":[{\"threat\":\"\",\"intervention\":\"\"}]},\"adaptiveManagement\":\"\",\"events\":[{\"funding\":\"\",\"name\":\"\",\"description\":\"\",\"scheduledDate\":\"\",\"media\":\"\",\"grantAnnouncementDate\":\"\",\"type\":\"\"}],\"budget\":{\"overallTotal\":0,\"headers\":[{\"data\":\"2020/2021\"},{\"data\":\"2021/2022\"}],\"rows\":[{\"costs\":[{\"dollar\":\"0\"},{\"dollar\":\"0\"}],\"rowTotal\":0,\"activities\":[],\"description\":\"\",\"shortLabel\":\"\"}],\"columnTotal\":[{\"data\":0},{\"data\":0}]},\"partnership\":{\"description\":\"\",\"rows\":[{\"data3\":\"\",\"data2\":\"\",\"data1\":\"\"}]},\"projectEvaluationApproach\":\"The project will work with beef and sheep farmer\",\"implementation\":{\"description\":\"The project will work with beef and sheep farmer\"},\"baseline\":{\"description\":\"\",\"rows\":[{\"method\":\"The project will work with beef and sheep farmer\",\"baseline\":\"The project will work with beef and sheep farmer\"}]},\"rationale\":\"The project will work with beef and sheep farmer\",\"caseStudy\":false,\"relatedProjects\":\"\",\"serviceIds\":[2],\"activities\":{\"activities\":[]},\"name\":\"Building the drought resilience of East Gippsland’s beef and sheep farms\",\"objectives\":{\"rows1\":[{\"assets\":[],\"description\":\"\"}],\"rows\":[{\"data3\":\"\",\"data2\":\"The project will work with beef and sheep farmer\",\"data1\":\"The project will work with beef and sheep farmer\"}]},\"consultation\":\"\",\"status\":\"active\"}},\"outputTargets\":[{\"scoreId\":\"7abd62ba-2e44-4318-800b-b659c73dc12b\",\"targetDate\":\"2021-02-21\",\"periodTargets\":[{\"period\":\"2020/2021\",\"target\":\"50\"},{\"period\":\"2021/2022\",\"target\":\"50\"}],\"target\":\"100\"}]}"

}
