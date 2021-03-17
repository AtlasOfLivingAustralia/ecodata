package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import grails.converters.JSON
import grails.testing.web.GrailsWebUnitTest
import grails.util.Holders
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference
import spock.lang.Specification

/**
 * Spec for the ProjectXlsExporter
 */
class ProjectXlsExporterSpec extends Specification implements GrailsWebUnitTest {

    def projectService = Mock(ProjectService)
    def metadataService = Mock(MetadataService)
    def userService = Mock(UserService)
    def reportingService = Mock(ReportingService)
    def xlsExporter
    ManagementUnitService managementUnitService = Stub(ManagementUnitService)
    ProjectXlsExporter projectXlsExporter
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
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, managementUnitService)
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
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:])
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId: '1234', workOrderId: 'work order 1', status: "active", contractStartDate: '2019-06-30T14:00:00Z', contractEndDate: '2022-06-30T14:00:00Z', funding: 1000, managementUnitId:"mu1"])
        xlsExporter.save()

        then:
        List<Map> results = readSheet(sheet, projectXlsExporter.projectHeaders)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Internal order number'] == 'work order 1'
        results[0]['Contracted Start Date'] == '2019-06-30T14:00:00Z'
        results[0]['Contracted End Date'] == '2022-06-30T14:00:00Z'
        results[0]['Funding'] == 1000
        results[0]['Management Unit'] == "Management Unit 1"
        results[0]['Status'] == "active"

    }


    void "project details can be exported with Termination Reason"() {
        setup:
        String sheet = 'Projects'
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:])
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId: '1234', workOrderId: 'work order 1', contractStartDate: '2019-06-30T14:00:00Z', contractEndDate: '2022-06-30T14:00:00Z', funding: 1000, managementUnitId:"mu1", status: "Terminated", terminationReason: "Termination Reason"])
        xlsExporter.save()

        then:
        List<Map> results = readSheet("Projects", projectXlsExporter.projectHeaders)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Internal order number'] == 'work order 1'
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
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:])
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId: '1234', workOrderId: 'work order 1', contractStartDate: '2019-06-30T14:00:00Z', status: "active", contractEndDate: '2022-06-30T14:00:00Z', funding: 1000])
        xlsExporter.save()

        then:
        List<Map> results = readSheet(sheet, projectXlsExporter.projectHeaders)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Internal order number'] == 'work order 1'
        results[0]['Contracted Start Date'] == '2019-06-30T14:00:00Z'
        results[0]['Contracted End Date'] == '2022-06-30T14:00:00Z'
        results[0]['Funding'] == 1000
        results[0]['Management Unit'] == ""

    }

    void "Dataset data can be exported"() {
        setup:
        String sheet = "Dataset"
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:])
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = projectDataSet()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = readSheet("Data_set_Summary", projectXlsExporter.datasetHeader)
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
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], managementUnitService, [:])
        projectXlsExporter.metadataService = Mock(MetadataService)
        Map project = rlpProject()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        List<Map> results = readSheet("RLP Outcomes", projectXlsExporter.rlpOutcomeHeaders)
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
        List<String> properties = ['Baseline Method', 'Baseline']
        Map project = project()

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        outputFile.withInputStream { fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            Sheet testSheet = workbook.getSheet(sheet)
            testSheet.physicalNumberOfRows == 3

            Cell baselineCell = testSheet.getRow(0).find { it.stringCellValue == 'Baseline' }
            baselineCell != null
            testSheet.getRow(1).getCell(baselineCell.getColumnIndex()).stringCellValue == 'Test'

        }

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
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "singleNestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [getJsonResource("singleSampleNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There is a header row and 2 data rows"
        activitySheet.physicalNumberOfRows == 5

        and: "The first header row contains the property names from the activity form"
        List headers = readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.afterNestedList", "notes"]

        and: "The second header row contains the version the property was introduced in"
        readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
        readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "After list", "Notes"]


        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 = readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "single.0.value1", "", "single notes"]
        List dataRow2 = readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "single.1.value1", "", ""]

    }

    void "String lists can be expanded into a column per value"() {
        setup:
        String activityToExport = "String lists"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "dataModelWithStringList")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [getJsonResource("sampleDataModelWithStringList")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There is a header row and 2 data rows"
        activitySheet.physicalNumberOfRows == 5

        and: "The first header row contains the property names from the activity form"
        List headers = readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "stringList1[c1]", "stringList1[c2]", "stringList1[c3]", "list.stringList2[c4]", "list.stringList2[c5]", "list.stringList2[c6]", "list.afterNestedList", "notes"]

        and: "The second header row contains the version the property was introduced in"
        readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1,1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
        readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "String list 1 - c1", "String list 1 - c2", "String list 1 - c3",  "String list 2 - c4",  "String list 2 - c5",  "String list 2 - c6", "After list", "Notes"]


        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 = readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "c1", "", "c3", "c4", "c5", "", "", "single notes"]
        List dataRow2 = readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "c1", "", "c3", "", "", "", "single.1.value1", ""]

    }


    void "Activities with deeply nested data can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "nestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [getJsonResource("sampleNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There are 3 header rows and 5 data rows"
        activitySheet.physicalNumberOfRows == 8

        and: "The header row contains the labels from the activity form"
        List headers = readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.afterNestedList", "notes"]
        readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1,1]
        readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 = readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "3", "0.value1", "0.0.value2", "", "notes"]
        List dataRow2 = readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "", "0.1.value2", "", ""]
        List dataRow3 = readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "1.value1", "1.0.value2", "", ""]
        List dataRow4 = readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "1.1.value2", "", ""]
        List dataRow5 = readRow(7, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "", "1.2.value2", "", ""]

    }

    void "Activities with 3 levels of nested data can be exported as a spreadsheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "deeplyNestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [getJsonResource("sampleDeeplyNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

        then:
        1 * activityFormService.findActivityForm(activityToExport, 1) >> activityForm
        1 * activityFormService.findVersionedActivityForm(activityToExport) >> [activityForm]

        and: "There is a single sheet exported with the name identifying the activity type and form version"
        workbook.numberOfSheets == 1
        Sheet activitySheet = workbook.getSheet(activityToExport)

        and: "There are 3 header rows and 6 data rows"
        activitySheet.physicalNumberOfRows == 9

        and: "The header row contains the labels from the activity form"
        List headers = readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.nestedList.nestedNestedList.value3", "list.afterNestedList", "notes"]
        readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1, 1, 1, 1, 1, 1]
        readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "Value 3", "After list", "Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 = readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "3", "0.value1", "0.0.value2", "3", "", "notes"]
        List dataRow2 = readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "0.value1", "0.0.value2", "4", "", ""]
        List dataRow3 = readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "0.value1", "0.1.value2", "", "", ""]
        List dataRow4 = readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "1.value1", "1.0.value2", "", "", ""]
        List dataRow5 = readRow(7, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "1.value1", "1.1.value2", "", "", ""]
        List dataRow6 = readRow(8, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow6 == ["", "", "1.value1", "1.2.value2", "", "", ""]

    }

    void "Data created from different versions of the same activity form will be exported to the same sheet"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "nestedDataModel")
        ActivityForm activityForm_v2 = createActivityForm(activityToExport, 2, "nestedDataModel_v2")
        Map project = project()
        project.activities = [
                [type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [getJsonResource("sampleNestedDataModel")]],
                [type: activityToExport, name: activityToExport, formVersion: activityForm_v2.formVersion, outputs: [getJsonResource("sampleNestedDataModel_v2")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

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
        List headers = readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.afterNestedList", "notes", "list.nestedList.value3", "extraNotes"]
        readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1,1, 2, 2]
        readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes", "Value 3", "Extra Notes"]

        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 = readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "3", "0.value1", "0.0.value2", "", "notes", "", ""]
        List dataRow2 = readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "", "0.1.value2", "", "", "", ""]
        List dataRow3 = readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "", "1.value1", "1.0.value2", "", "", "", ""]
        List dataRow4 = readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "1.1.value2", "", "", "", ""]
        List dataRow5 = readRow(7, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow5 == ["", "", "", "1.2.value2", "", "", "", ""]

        List dataRow7 = readRow(8, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow7 == ["", "3", "0.value1", "", "", "notes", "0.0.value3", "extra notes"]
        List dataRow8 = readRow(9, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow8 == ["", "", "", "", "", "", "0.1.value3", ""]
        List dataRow9 = readRow(10, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow9 == ["", "", "1.value1", "", "", "", "1.0.value3", ""]
        List dataRow10 = readRow(11, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow10 == ["", "", "", "", "", "", "1.1.value3", ""]
    }

    void "Versioning of values in constraints are handled correctly"() {
        setup:
        String activityToExport = "String lists"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "dataModelWithStringList")
        ActivityForm activityForm_v2 = createActivityForm(activityToExport, 2, "dataModelWithStringListv2")

        Map project = project()
        project.activities = [
                [type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion, outputs: [getJsonResource("sampleDataModelWithStringList")]],
                [type: activityToExport, name: activityToExport, formVersion: activityForm_v2.formVersion, outputs: [getJsonResource("sampleDataModelWithStringListv2")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

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
        List headers = readRow(0, activitySheet)
        headers == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "stringList1[c1]", "stringList1[c2]", "stringList1[c3]", "list.stringList2[c4]", "list.stringList2[c5]", "list.stringList2[c6]", "list.afterNestedList", "notes", "stringList1[c4]"]

        and: "The second header row contains the version the property was introduced in"
        readRow(1, activitySheet) == projectXlsExporter.commonActivityHeaders.collect{''} + [1,1,1,1,1,1,1,1,1,1,2]

        and: "The third header row contains the labels from the activity form"
        readRow(2, activitySheet) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "String list 1 - c1", "String list 1 - c2", "String list 1 - c3",  "String list 2 - c4",  "String list 2 - c5",  "String list 2 - c6", "After list", "Notes", "String list 1 - c4"]


        and: "The data in the subsequent rows matches the data in the activity v1"
        List dataRow1 = readRow(3, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow1 == ["", "33", "c1", "", "c3", "c4", "c5", "", "", "single notes", ""]
        List dataRow2 = readRow(4, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow2 == ["", "", "c1", "", "c3", "", "", "", "single.1.value1", "", ""]

        and: "The data in the subsequent rows matches the data in the activity v2"
        List dataRow3 = readRow(5, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow3 == ["", "33", "", "", "c3", "", "c5", "", "", "single notes", "c4"]
        List dataRow4 = readRow(6, activitySheet).subList(projectXlsExporter.commonActivityHeaders.size(), headers.size())
        dataRow4 == ["", "", "", "", "c3", "", "", "", "single.1.value1", "", "c4"]

    }

    void "Each form section / output will be exported to a separate tab"() {
        setup:
        String activityToExport = "RLP Annual Report"
        ActivityForm activityForm = createActivityForm(activityToExport, 1, "singleNestedDataModel", "nestedDataModel")
        Map project = project()
        project.activities = [[type: activityToExport, name: activityToExport, formVersion: activityForm.formVersion,
                               outputs: [getJsonResource("singleSampleNestedDataModel"),
                                         getJsonResource("sampleNestedDataModel")]]]

        when:
        projectXlsExporter.tabsToExport = [activityToExport]
        projectXlsExporter.export(project)
        xlsExporter.save()

        Workbook workbook = readWorkbook()

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
        List headers1 = readRow(0, section1)
        headers1 == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.afterNestedList", "notes"]
        List headers2 = readRow(0, section2)
        headers2 == projectXlsExporter.commonActivityHeaders.collect{''} + ["outputNotCompleted", "number1", "list.value1", "list.nestedList.value2", "list.afterNestedList", "notes"]

        and: "The second header row contains the version the property was introduced in"
        readRow(1, section1) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1]
        readRow(1, section2) == projectXlsExporter.commonActivityHeaders.collect{''} + [1, 1,1,1,1,1]

        and: "The third header row contains the labels from the activity form"
        readRow(2, section1) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "After list", "Notes"]
        readRow(2, section2) == projectXlsExporter.commonActivityHeaders + ["Not applicable", "Number 1", "Value 1", "Value 2", "After list", "Notes"]


        and: "The data in the subsequent rows matches the data in the activity"
        List dataRow1 = readRow(3, section1).subList(projectXlsExporter.commonActivityHeaders.size(), headers1.size())
        dataRow1 == ["", "33", "single.0.value1", "", "single notes"]
        List dataRow2 = readRow(4, section1).subList(projectXlsExporter.commonActivityHeaders.size(), headers1.size())
        dataRow2 == ["", "", "single.1.value1", "", ""]

        and: "The data in the second form section output rows matches the data in the activity"
        List s2dataRow1 = readRow(3, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow1 == ["", "3", "0.value1", "0.0.value2", "", "notes"]
        List s2dataRow2 = readRow(4, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow2 == ["", "", "", "0.1.value2", "", ""]
        List s2dataRow3 = readRow(5, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow3 == ["", "", "1.value1", "1.0.value2", "", ""]
        List s2dataRow4 = readRow(6, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow4 == ["", "", "", "1.1.value2", "", ""]
        List s2dataRow5 = readRow(7, section2).subList(projectXlsExporter.commonActivityHeaders.size(), headers2.size())
        s2dataRow5 == ["", "", "", "1.2.value2", "", ""]

    }

//    void "Each form section / output will be exported to a separate tab"() {
//
//    }


    private List readRow(int index, Sheet sheet) {
        Row row = sheet.getRow(index)
        row.cellIterator().collect { Cell cell ->
            if (cell.cellType == CellType.NUMERIC) {
                cell.getNumericCellValue()
            }
            else {
                cell.getStringCellValue()
            }
        }
    }

    private Workbook readWorkbook() {
        Workbook workbook = null
        outputFile.withInputStream { fileIn ->
            workbook = WorkbookFactory.create(fileIn)
        }
        workbook
    }

    private ActivityForm createActivityForm(String name, int formVersion, String... templateFileName) {
        ActivityForm activityForm = new ActivityForm(name: name, formVersion: formVersion)
        templateFileName.each {
            Map formTemplate = getJsonResource(it)
            activityForm.sections << new FormSection(name: formTemplate.modelName, template: formTemplate)
        }

        activityForm
    }

    private List readSheet(String sheet, List properties) {
        def columnMap = [:]
        properties.eachWithIndex { prop, index ->
            def colString = CellReference.convertNumToColString(index)
            columnMap[colString] = prop
        }
        def config = [
                sheet    : sheet,
                startRow : 1,
                columnMap: columnMap
        ]
        outputFile.withInputStream { fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            excelImportService.mapSheet(workbook, config)
        }

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


    private Map getJsonResource(name) {
        JSON.parse(new File("src/test/resources/${name}.json").newInputStream(), 'UTF-8')
    }

    private String projectJson = "{\n" +
            "    \"alaHarvest\" : false,\n" +
            "    \"associatedProgram\" : \"\",\n" +
            "    \"associatedSubProgram\" : \"\",\n" +
            "    \"status\": \"active\",\n"+
            "    \"countries\" : [],\n" +
            "    \"custom\" : {\n" +
            "        \"details\" : {\n" +
            "            \"partnership\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"\",\n" +
            "                        \"data2\" : \"\",\n" +
            "                        \"data1\" : \"\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"projectEvaluationApproach\" : \"Test\",\n" +
            "            \"implementation\" : {\n" +
            "                \"description\" : \"Test methodology\"\n" +
            "            },\n" +
            "            \"obligations\" : \"\",\n" +
            "            \"policies\" : \"\",\n" +
            "            \"description\" : \"TBA - this is a temporary description\",\n" +
            "            \"baseline\" : {\n" +
            "                \"description\" : \"This is a baseline\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"method\" : \"Test\",\n" +
            "                        \"baseline\" : \"Test\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"method\" : \"Test1\",\n" +
            "                        \"baseline\" : \"Test2\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"rationale\" : \"Test rational\",\n" +
            "            \"caseStudy\" : true,\n" +
            "            \"lastUpdated\" : \"2019-06-06T06:07:27Z\",\n" +
            "            \"priorities\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"Test\",\n" +
            "                        \"data2\" : \"Test\",\n" +
            "                        \"data1\" : \"Test\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"serviceIds\" : [ \n" +
            "                1, \n" +
            "                2, \n" +
            "                3, \n" +
            "                4, \n" +
            "                5, \n" +
            "                34, \n" +
            "                6, \n" +
            "                7, \n" +
            "                8, \n" +
            "                10, \n" +
            "                9, \n" +
            "                11, \n" +
            "                12, \n" +
            "                13, \n" +
            "                14, \n" +
            "                15, \n" +
            "                16, \n" +
            "                17, \n" +
            "                18, \n" +
            "                19, \n" +
            "                20, \n" +
            "                21, \n" +
            "                22, \n" +
            "                23, \n" +
            "                24, \n" +
            "                25, \n" +
            "                26, \n" +
            "                27, \n" +
            "                28, \n" +
            "                35, \n" +
            "                29, \n" +
            "                30, \n" +
            "                31, \n" +
            "                32, \n" +
            "                33\n" +
            "            ],\n" +
            "            \"outcomes\" : {\n" +
            "                \"secondaryOutcomes\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Soil carbon\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Natural Temperate Grassland of the South Eastern Highlands\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities.\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"shortTermOutcomes\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Asset 1\", \n" +
            "                            \"Assert2\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"Test\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Asset3\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"Test 2\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [],\n" +
            "                        \"description\" : \"sfasdf\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"midTermOutcomes\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Asset 1\", \n" +
            "                            \"Assert2\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"Test\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [],\n" +
            "                        \"description\" : \"\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"primaryOutcome\" : {\n" +
            "                \"primaryOutcome\" : {\n" +
            "                    \"assets\" : [ \n" +
            "                        \"Climate change adaptation\", \n" +
            "                        \"Market traceability\"\n" +
            "                    ],\n" +
            "                    \"description\" : \"By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production.\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"keq\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"\",\n" +
            "                        \"data2\" : \"Test\",\n" +
            "                        \"data1\" : \"*** This is a monitoring indictor ** \"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"threats\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"threat\" : \"Test this is another EDIT\",\n" +
            "                        \"intervention\" : \"Test\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"objectives\" : {\n" +
            "                \"rows1\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [],\n" +
            "                        \"description\" : \"\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"\",\n" +
            "                        \"data2\" : \"\",\n" +
            "                        \"data1\" : \"\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"events\" : [ \n" +
            "                {\n" +
            "                    \"funding\" : \"0\",\n" +
            "                    \"name\" : \"\",\n" +
            "                    \"description\" : \"\",\n" +
            "                    \"scheduledDate\" : \"\",\n" +
            "                    \"media\" : \"\",\n" +
            "                    \"grantAnnouncementDate\" : \"\",\n" +
            "                    \"type\" : \"\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"status\" : \"active\",\n" +
            "            \"budget\" : {\n" +
            "                \"overallTotal\" : 0,\n" +
            "                \"headers\" : [ \n" +
            "                    {\n" +
            "                        \"data\" : \"2017/2018\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"data\" : \"2018/2019\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"costs\" : [ \n" +
            "                            {\n" +
            "                                \"dollar\" : \"0\"\n" +
            "                            }, \n" +
            "                            {\n" +
            "                                \"dollar\" : \"0\"\n" +
            "                            }\n" +
            "                        ],\n" +
            "                        \"rowTotal\" : 0,\n" +
            "                        \"description\" : \"\",\n" +
            "                        \"shortLabel\" : \"\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"columnTotal\" : [ \n" +
            "                    {\n" +
            "                        \"data\" : 0\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"data\" : 0\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"dateCreated\" : \"2018-06-14T04:22:13.057Z\",\n" +
            "    \"description\" : \"TBA - this is a temporary description\",\n" +
            "    \"ecoScienceType\" : [],\n" +
            "    \"externalId\" : \"\",\n" +
            "    \"fundingSource\" : \"RLP\",\n" +
            "    \"funding\" : 10000,\n" +
            "    \"grantId\" : \"RLP-Test-Program-Project-1\",\n" +
            "    \"industries\" : [],\n" +
            "    \"bushfireCategories\" : [],\n" +
            "    \"isCitizenScience\" : false,\n" +
            "    \"isExternal\" : false,\n" +
            "    \"isMERIT\" : true,\n" +
            "    \"isSciStarter\" : false,\n" +
            "    \"lastUpdated\" : \"2019-08-13T05:17:48.686Z\",\n" +
            "    \"manager\" : \"\",\n" +
            "    \"name\" : \"Test Program - Project 1\",\n" +
            "    \"orgIdSvcProvider\" : \"\",\n" +
            "    \"organisationId\" : \"\",\n" +
            "    \"organisationName\" : \"Test Org\",\n" +
            "    \"origin\" : \"merit\",\n" +
            "    \"outputTargets\" : [ \n" +
            "        {\n" +
            "            \"scoreId\" : \"0df7c177-2864-4a25-b420-2cf3c45ce749\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : \"2\"\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"2\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"69deaaf9-cdc2-439a-b684-4cffdc7f224e\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"4\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"26a8213e-1770-4dc4-8f99-7e6302197504\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"c464b652-be5e-4658-b62f-02bf1a80bcf8\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"50\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"3cbf653f-f74c-4066-81d2-e3f78268185c\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"300\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"0f9ef068-b2f9-4e6f-9ab5-521857b036f4\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"300\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"e48faf01-72eb-479c-be9b-d2d71d254fa4\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"482bdf4e-6f7a-4bdf-80d5-d619ac7cdf50\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"8025b157-44d7-4283-bc1c-f40fb9b99501\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"600\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"a3afea6e-711c-4ef2-bb20-6d2630b7ee93\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"12\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"757d6c9e-ec24-486f-a128-acc9bfb87830\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"600\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"b7c067e3-6ae7-4e76-809a-312165b75f94\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"60\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"d1c10295-05e5-4265-a5f1-8a5683af2efe\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"011a161f-7275-4b5e-986e-3fe4640d0265\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"500\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"c2dc6f91-ccb1-412e-99d0-a842a4ac4b03\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"def4e2af-dcad-4a15-8336-3765e6671f08\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"c46842b6-d7b6-4917-b56f-f1b0594663fa\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"199\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"2d877a91-6312-4c44-9ae1-2494ea3e43db\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"d4ba13a1-00c8-4e7f-8463-36b6ea37eee6\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"4bcab901-879a-402d-83f3-01528c6c86a5\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"1\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"45994b98-21f1-4927-a03e-3d940ac75116\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"100\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"6eaa061c-b77b-4440-8e8f-7ebaa2ff6207\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"1000\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"0e887410-a3c5-49ca-a6f5-0f2f6fae30db\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"200\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"725d9365-0889-4355-8a7f-a21ef260c468\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"450\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"0f11a699-6063-4e91-96ca-53e45cf26b80\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"900\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"3c2c4aaa-fd5f-43d8-a72f-3567e6dea6f4\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"50\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"ed30b80b-7bb9-4c04-9949-093df64d124c\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"5000\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"4cbcb2b5-45cd-42dc-96bf-a9a181a4865b\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"3090\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"f38fbd9e-d208-4750-96ce-3c032ad37684\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"500\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"dea1ff8b-f4eb-4987-8073-500bbbf97fcd\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"500\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"4f747371-fa5f-4200-ae37-6cd59d268fe8\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"685d61e9-2ebd-4198-a83a-ac7a2fc1477a\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"91387f2b-258d-4325-aa60-828d1acf6ac6\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"3\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"ba3d0a20-1e4d-404a-9907-b95239499c2f\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"28dd9736-b66a-4ab4-9111-504d5cffba88\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"planStatus\" : \"not approved\",\n" +
            "    \"plannedEndDate\" : \"2019-06-29T14:00:00.000Z\",\n" +
            "    \"plannedStartDate\" : \"2017-08-01T14:00:00.000Z\",\n" +
            "    \"programId\" : \"test_program\",\n" +
            "    \"projectId\" : \"8693cbc5-6947-4614-9bd1-b22ef44bc8fd\",\n" +
            "    \"projectType\" : \"works\",\n" +
            "    \"promoteOnHomepage\" : \"no\",\n" +
            "    \"risks\" : {\n" +
            "        \"overallRisk\" : \"Low\",\n" +
            "        \"rows\" : [ \n" +
            "            {\n" +
            "                \"consequence\" : \"Minor\",\n" +
            "                \"likelihood\" : \"Unlikely\",\n" +
            "                \"residualRisk\" : \"Low\",\n" +
            "                \"currentControl\" : \"Test\",\n" +
            "                \"description\" : \"Low\",\n" +
            "                \"threat\" : \"Work Health and Safety\",\n" +
            "                \"riskRating\" : \"Low\"\n" +
            "            }, \n" +
            "            {\n" +
            "                \"consequence\" : \"Moderate\",\n" +
            "                \"likelihood\" : \"Unlikely\",\n" +
            "                \"residualRisk\" : \"High\",\n" +
            "                \"currentControl\" : \"Test\",\n" +
            "                \"description\" : \"Test 2\",\n" +
            "                \"threat\" : \"Performance\",\n" +
            "                \"riskRating\" : \"Low\"\n" +
            "            }, \n" +
            "            {\n" +
            "                \"consequence\" : \"Minor\",\n" +
            "                \"likelihood\" : \"Possible\",\n" +
            "                \"residualRisk\" : \"Medium\",\n" +
            "                \"currentControl\" : \"yep\",\n" +
            "                \"description\" : \"lalala\",\n" +
            "                \"threat\" : \"People resources\",\n" +
            "                \"riskRating\" : \"Low\"\n" +
            "            }, \n" +
            "            {\n" +
            "                \"consequence\" : \"High\",\n" +
            "                \"likelihood\" : \"Possible\",\n" +
            "                \"residualRisk\" : \"Medium\",\n" +
            "                \"currentControl\" : \"yrd\",\n" +
            "                \"description\" : \"\$\",\n" +
            "                \"threat\" : \"Financial\",\n" +
            "                \"riskRating\" : \"Medium\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"status\" : \"\"\n" +
            "    },\n" +
            "    \"scienceType\" : [],\n" +
            "    \"serviceProviderName\" : \"\",\n" +
            "    \"managementUnitId\" : \"mu1\",\n" +
            "    \"status\" : \"active\",\n" +
            "    \"tags\" : [],\n" +
            "    \"uNRegions\" : [],\n" +
            "    \"workOrderId\" : \"1234565\",\n" +
            "    \"blog\" : []\n" +
            "}"

    private String projectDataSet = "{\"origin\":\"merit\",\"promoteOnHomepage\":\"no\",\"name\":\"Building the drought resilience of East Gippsland’s beef and sheep farms\",\"funding\":0,\"isCitizenScience\":false,\"uNRegions\":[],\"industries\":[],\"tags\":[\"\"],\"isBushfire\":false,\"alaHarvest\":false,\"isMERIT\":true,\"status\":\"active\",\"isSciStarter\":false,\"isExternal\":false,\"projectId\":\"1dda8202-cbf1-45d8-965c-9b93306aaeaf\",\"grantId\":\"FDF-MU24-P1\",\"projectType\":\"works\",\"description\":\"Test\",\"externalId\":\"\",\"serviceProviderName\":\"\",\"organisationName\":\"RLP East Gippsland Catchment Management Authority\",\"internalOrderId\":\"TBA\",\"workOrderId\":\"TBA\",\"programId\":\"08335f58-63d0-42e1-a852-2ba5c3a083ed\",\"planStatus\":\"not approved\",\"abn\":\"\",\"associatedSubProgram\":\"Natural Resource Management - Landscape\",\"organisationId\":\"\",\"manager\":\"\",\"orgIdSvcProvider\":\"\",\"associatedProgram\":\"Future Drought Fund\",\"custom\":{\"dataSets\":[{\"owner\":\"na\",\"methodDescription\":\"Testing\",\"custodian\":\"na\",\"investmentPriorities\":[\"Testing\",\"Other\"],\"endDate\":\"2021-02-04T13:00:00Z\",\"methods\":[\"Hair, track, dung sampling\",\"Area sampling\"],\"format\":\"JSON\",\"published\":\"No\",\"sensitivities\":[\"Indigenous/cultural\",\"Commercially sensitive\"],\"type\":\"Baseline dataset associated with a project outcome\",\"collectionApp\":\"Test\",\"collectorType\":\"Specialist consultant\",\"qa\":\"Yes\",\"otherInvestmentPriority\":\"Other Priorities, other priorities\",\"progress\":\"started\",\"term\":\"Short-term outcome statement\",\"dataSetId\":\"967fd2e8-8621-49c2-99ac-861828f752ce\",\"name\":\"Testing Data Set\",\"measurementTypes\":[\"Adoption - climate and market demands\",\"Adoption - land resource management practices\"],\"storageType\":\"Cloud\",\"location\":\"test\",\"programOutcome\":\"5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.\",\"publicationUrl\":\"ttt\",\"startDate\":\"2021-02-03T13:00:00Z\",\"addition\":\"No\"}]}}"

    private String rlpPlan = "{\"bushfireCategories\":[],\"origin\":\"merit\",\"promoteOnHomepage\":\"no\",\"ecoScienceType\":[],\"countries\":[],\"name\":\"Building the drought resilience of East Gippsland’s beef and sheep farms\",\"funding\":0,\"isCitizenScience\":false,\"uNRegions\":[],\"industries\":[],\"tags\":[\"\"],\"isBushfire\":false,\"alaHarvest\":false,\"scienceType\":[],\"isMERIT\":true,\"status\":\"application\",\"isSciStarter\":false,\"isExternal\":false,\"projectId\":\"5a80f409-0b27-480a-9938-340aa48cc947\",\"grantId\":\"FDF-MU24-P1\",\"projectType\":\"works\",\"description\":\"The project will work with beef and sheep farmers to build their knowledge and understanding of the options for using different pasture/forage crop varieties to manage drought and build resilience into their farming operations.  A key strength of the project is that during the most recent drought there were local farmers who managed to retain groundcover and pastures, retain their core breeding stock, and will recover more quickly once drought conditions ease. This project will draw on those lessons to co-design local research trials of plant varieties and to demonstrate drought adaptation practices through peer learning. \\nThe project design and delivery is farmer-led, with local facilitation, through resourcing of existing trusted facilitators and contractors. It also involves collaboration with subject experts (consultants and researchers) to ensure the best science and measurable results are achieved from the project.\",\"externalId\":\"\",\"managementUnitId\":\"82c15908-04ab-4e49-bd43-9616a6dcb528\",\"serviceProviderName\":\"\",\"organisationName\":\"RLP East Gippsland Catchment Management Authority\",\"internalOrderId\":\"\",\"workOrderId\":\"TBA\",\"programId\":\"08335f58-63d0-42e1-a852-2ba5c3a083ed\",\"planStatus\":\"not approved\",\"abn\":\"\",\"associatedSubProgram\":\"\",\"custom\":{\"details\":{\"communityEngagement\":\"\",\"obligations\":\"\",\"policies\":\"\",\"description\":\"The project will work with beef and sheep farmers to build their knowledge and understanding of the options for using different pasture/forage crop varieties to manage drought and build resilience into their farming operations.  A key strength of the project is that during the most recent drought there were local farmers who managed to retain groundcover and pastures, retain their core breeding stock, and will recover more quickly once drought conditions ease. This project will draw on those lessons to co-design local research trials of plant varieties and to demonstrate drought adaptation practices through peer learning. \\nThe project design and delivery is farmer-led, with local facilitation, through resourcing of existing trusted facilitators and contractors. It also involves collaboration with subject experts (consultants and researchers) to ensure the best science and measurable results are achieved from the project.\",\"lastUpdated\":\"2021-02-04T23:57:56Z\",\"priorities\":{\"description\":\"\",\"rows\":[{\"data3\":\"The project will work with beef and sheep farmer\",\"data2\":\"The project will work with beef and sheep farmer\",\"data1\":\"The project will work with beef and sheep farmer\"}]},\"assets\":[{\"description\":\"\",\"category\":\"\"}],\"outcomes\":{\"secondaryOutcomes\":[{}],\"otherOutcomes\":[\"More primary producers preserve natural capital while also improving productivity and profitability\",\"More primary producers adopt risk management practices to improve their sustainability and resilience\",\"More primary producers and agricultural communities are experimenting with adaptive or transformative NRM practices, systems and approaches that link and contribute to building drought resilience\",\"Partnerships and engagement is built between stakeholders responsible for managing natural resources\"],\"shortTermOutcomes\":[{\"assets\":[],\"description\":\"Test\"}],\"midTermOutcomes\":[{\"assets\":[],\"description\":\"test\"}],\"primaryOutcome\":{\"assets\":[\"Hillslope erosion\",\"Wind erosion\"],\"description\":\"5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.\"}},\"programName\":\"Natural Resource Management - Landscape\",\"keq\":{\"description\":\"\",\"rows\":[{\"data3\":\"\",\"data2\":\"\",\"data1\":\"\"},{\"data3\":\"\",\"data2\":\"\",\"data1\":\"\"}]},\"threats\":{\"description\":\"\",\"rows\":[{\"threat\":\"\",\"intervention\":\"\"}]},\"adaptiveManagement\":\"\",\"events\":[{\"funding\":\"\",\"name\":\"\",\"description\":\"\",\"scheduledDate\":\"\",\"media\":\"\",\"grantAnnouncementDate\":\"\",\"type\":\"\"}],\"budget\":{\"overallTotal\":0,\"headers\":[{\"data\":\"2020/2021\"},{\"data\":\"2021/2022\"}],\"rows\":[{\"costs\":[{\"dollar\":\"0\"},{\"dollar\":\"0\"}],\"rowTotal\":0,\"activities\":[],\"description\":\"\",\"shortLabel\":\"\"}],\"columnTotal\":[{\"data\":0},{\"data\":0}]},\"partnership\":{\"description\":\"\",\"rows\":[{\"data3\":\"\",\"data2\":\"\",\"data1\":\"\"}]},\"projectEvaluationApproach\":\"The project will work with beef and sheep farmer\",\"implementation\":{\"description\":\"The project will work with beef and sheep farmer\"},\"baseline\":{\"description\":\"\",\"rows\":[{\"method\":\"The project will work with beef and sheep farmer\",\"baseline\":\"The project will work with beef and sheep farmer\"}]},\"rationale\":\"The project will work with beef and sheep farmer\",\"caseStudy\":false,\"relatedProjects\":\"\",\"serviceIds\":[2],\"activities\":{\"activities\":[]},\"name\":\"Building the drought resilience of East Gippsland’s beef and sheep farms\",\"objectives\":{\"rows1\":[{\"assets\":[],\"description\":\"\"}],\"rows\":[{\"data3\":\"\",\"data2\":\"The project will work with beef and sheep farmer\",\"data1\":\"The project will work with beef and sheep farmer\"}]},\"consultation\":\"\",\"status\":\"active\"}},\"outputTargets\":[{\"scoreId\":\"7abd62ba-2e44-4318-800b-b659c73dc12b\",\"targetDate\":\"2021-02-21\",\"periodTargets\":[{\"period\":\"2020/2021\",\"target\":\"50\"},{\"period\":\"2021/2022\",\"target\":\"50\"}],\"target\":\"100\"}]}"

}
