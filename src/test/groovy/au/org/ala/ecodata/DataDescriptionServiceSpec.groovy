package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.apache.poi.ss.usermodel.DateUtil
import org.grails.plugins.testing.GrailsMockMultipartFile

class DataDescriptionServiceSpec extends MongoSpec implements ServiceUnitTest<DataDescriptionService>, DataTest {

    ExcelImportService excelImportService = Mock(ExcelImportService)

    def setup() {
        cleanupData()
        service.excelImportService = excelImportService
        mockDomain(DataDescription)
    }


    private void cleanupData() {
        DataDescription.findAll().each{it.delete(flush:true)}
    }

    void "DataDescription can be parsed from the spreadsheet and can be saved to the database"() {
        given:
        InputStream excel = getClass().getResourceAsStream('/metadata.xlsx')

        when:
        def dataDescriptions = service.importData(excel)

        then:
        1 * excelImportService.mapSheet(_, _) >> [[excelExportedColumn:"project_id", excelExportedDescription:"ID of the project",lastUpdated:44739.0, excelExportedStatus:"As is"], [excelExportedColumn:"management_unit", excelExportedDescription:"Id of the management unit",lastUpdated:44739.0, excelExportedStatus:"As is"]]
        dataDescriptions == true

    }

    void "Persisting of data will not be processed because of empty spreadsheet"() {
        given:
        InputStream excel = getClass().getResourceAsStream('/metadata.xlsx')

        when:
        def dataDescriptions = service.importData(excel)

        then:
        1 * excelImportService.mapSheet(_, _) >> []
        dataDescriptions == false

    }

    void "Check if the file is excel type"() {
        given:
        InputStream excel = getClass().getResourceAsStream('/metadata.xlsx')
        GrailsMockMultipartFile file = new GrailsMockMultipartFile('excel', 'metadata.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', excel)

        when:
        def result = service.isValidFileType(file.getContentType())

        then:
        result == true
    }

    def "Given excel date this can be converted to java.util.Date"() {
        given:
        Double date = 44739.0

        expect:
        DateUtil.getJavaDate(date).toInstant().toString() == '2022-06-26T14:00:00Z'
    }
}
