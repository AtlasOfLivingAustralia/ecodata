package au.org.ala.ecodata

import au.org.ala.ecodata.util.ExportTestUtils
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import spock.lang.Specification

class MetadataControllerSpec extends Specification implements ControllerUnitTest<MetadataController>, DataTest {

    MetadataService metadataService = Mock(MetadataService)
    File outputFile
    ExcelImportService excelImportService


    def setup() {
        controller.metadataService = metadataService
        outputFile = File.createTempFile('test', '.xlsx')
        excelImportService = new ExcelImportService()
    }

    def "Get excel template that can be populated with output data and uploaded"() {
        setup:
        String outputName = 'test'
        def model = [modelName:'test', dataModel:[[dataType:'text',name:'testField']],
                     viewModel:[[type:"row", items:[source:'testField',type:'text', preLabel:'testField']]]]
        def annotatedModel = [[dataType:'text',name:'testField', preLabel:'testField', label:'testField',source:'testField',type:'text']]

        when:
        params.type = outputName
        params.listName = null
        params.expandList = 'true'
        controller.excelOutputTemplate()

        then:
        1 * metadataService.getOutputDataModelByName(outputName) >> model
        1 * metadataService.annotatedOutputDataModel(outputName, true) >> annotatedModel
        response.status == HttpStatus.SC_OK
        response.contentType == "application/vnd.ms-excel"
        response.headerNames.contains("Content-disposition")
        response.header("Content-disposition") == 'attachment; filename="test.xlsx";'

        OutputStream outStream = new FileOutputStream(outputFile)
        outStream.setBytes(response.contentAsByteArray)
        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)
        Sheet sheet = workbook.getSheet(outputName)
        sheet.physicalNumberOfRows == 1 //header row
        sheet.head().getPhysicalNumberOfCells() == 1 //one field
        sheet.head().getCell(0).toString() == 'testField'

    }

}
