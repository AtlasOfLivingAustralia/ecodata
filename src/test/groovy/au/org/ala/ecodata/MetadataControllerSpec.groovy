package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class MetadataControllerSpec extends Specification implements ControllerUnitTest<MetadataController>, DataTest {

    MetadataService metadataService = Mock(MetadataService)

    def setup() {
        controller.metadataService = metadataService
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
    }

}
