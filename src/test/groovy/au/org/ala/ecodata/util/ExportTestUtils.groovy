package au.org.ala.ecodata.util

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.ExcelImportService
import au.org.ala.ecodata.FormSection
import grails.converters.JSON
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellReference

class ExportTestUtils {

    static ActivityForm createActivityForm(String name, int formVersion, String... templateFileName) {
        ActivityForm activityForm = new ActivityForm(name: name, formVersion: formVersion)
        templateFileName.each {
            Map formTemplate = getJsonResource(it)
            activityForm.sections << new FormSection(name: formTemplate.modelName, template: formTemplate)
        }

        activityForm
    }

    static List readSheet(File outputFile, String sheet, List properties, ExcelImportService excelImportService) {
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

    static List readRow(int index, Sheet sheet) {
        Row row = sheet.getRow(index)
        row.cellIterator().collect { Cell cell ->
            if (cell.cellType == CellType.NUMERIC) {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    cell.getDateCellValue()
                }
                else {
                    cell.getNumericCellValue()
                }
            }
            else {
                cell.getStringCellValue()
            }
        }
    }

    static Map getJsonResource(name) {
        JSON.parse(new File("src/test/resources/${name}.json").newInputStream(), 'UTF-8')
    }

    static Workbook readWorkbook(File outputFile) {
        Workbook workbook = null
        outputFile.withInputStream { fileIn ->
            workbook = WorkbookFactory.create(fileIn)
        }
        workbook
    }
}
