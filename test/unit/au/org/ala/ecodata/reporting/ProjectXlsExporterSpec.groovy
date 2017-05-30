package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.ReportingService
import au.org.ala.ecodata.UserService
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellReference
import org.grails.plugins.excelimport.ExcelImportService
import spock.lang.Specification

/**
 * Spec for the ProjectXlsExporter
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([MetadataService, UserService, ReportingService])
class ProjectXlsExporterSpec extends Specification {

    def projectService = Mock(ProjectService)
    def xlsExporter
    ProjectXlsExporter projectXlsExporter
    ExcelImportService excelImportService
    File outputFile

    void setup() {
        outputFile = new File('test.xlsx')
        outputFile.deleteOnExit()
        xlsExporter = new XlsExporter(outputFile.name)

        excelImportService = new ExcelImportService()
    }

    void teardown() {
        outputFile.delete()
    }

    void "project details can be exported"() {
        setup:
        String sheet = 'Projects'
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [:])
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId:'1234'])
        xlsExporter.save()

        then:
        List<Map> results = readSheet(sheet, projectXlsExporter.projectProperties)
        results.size() == 1
        results[0]['projectId'] == '1234'

    }


    private List readSheet(String sheet, List properties) {
        def columnMap = [:]
        properties.eachWithIndex { prop, index ->
            def colString = CellReference.convertNumToColString(index)
            columnMap[colString] = prop
        }
        def config = [
                sheet:sheet,
                startRow:1,
                columnMap:columnMap
        ]
        outputFile.withInputStream {fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            excelImportService.convertColumnMapConfigManyRows(workbook, config)
        }

    }
}
