package au.org.ala.ecodata

import grails.testing.services.ServiceUnitTest
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import spock.lang.Specification

class ExcelImportServiceSpec extends Specification  implements ServiceUnitTest<ExcelImportService>  {

    def setup() {
    }

    def cleanup() {
    }

    void "test simple export"() {
        setup:
        InputStream input = new File("src/test/resources/Community_stakeholder_engagement.xlsx").newInputStream()
        Workbook workbook = WorkbookFactory.create(input)

        when:
        Map config = [
                sheet:'RLP  Community engagement',
                startRow:1,
                columnMap:[
                        A:'type',
                        B:'count',
                        C:'total',
                        G:'industryType'
                ]
        ]
        List<Map> data = service.mapSheet(workbook, config)

        then:
        data.size() == 2

        data[0].type  == 'Training / workshop events'
        data[0].count == 2
        data[0].total == 2
        data[0].industryType == 'Dryland agriculture'

        data[1].type  == 'Field days'
        data[1].count == 1
        data[1].total == 3
        data[1].industryType == 'Dairy,Fisheries'

    }
}
