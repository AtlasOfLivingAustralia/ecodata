package au.org.ala.ecodata

import grails.test.mixin.TestFor
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ExcelImportService)
class ExcelImportServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        setup:
        InputStream input = getClass().getResourceAsStream("/resources/Community_stakeholder_engagement.xlsx")
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
