package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.apache.poi.ss.usermodel.DateUtil

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
        1 * excelImportService.mapSheet(_, _) >> [[name:"yzy350_v1", description:"admin",startDate:44739.0,endDate:45107.0, grantId:"DD-01-D1"], [name:"yzy350_v2", description:"admin",startDate:44739.0,endDate:45107.0, grantId:"DD-02-D2"]]
        dataDescriptions == true

    }

    def "Given excel date this can be converted to java.util.Date"() {
        given:
        Double date = 44739.0

        expect:
        DateUtil.getJavaDate(date).toInstant().toString() == '2022-06-26T14:00:00Z'
    }
}
