package au.org.ala.ecodata

import grails.testing.services.ServiceUnitTest
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import spock.lang.Specification

class ExcelImportServiceSpec extends Specification implements ServiceUnitTest<ExcelImportService> {

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
                sheet    : 'RLP  Community engagement',
                startRow : 1,
                columnMap: [
                        A: 'type',
                        B: 'count',
                        C: 'total',
                        G: 'industryType'
                ]
        ]
        List<Map> data = service.mapSheet(workbook, config)

        then:
        data.size() == 2

        data[0].type == 'Training / workshop events'
        data[0].count == 2
        data[0].total == 2
        data[0].industryType == 'Dryland agriculture'

        data[1].type == 'Field days'
        data[1].count == 1
        data[1].total == 3
        data[1].industryType == 'Dairy,Fisheries'

    }

    def "should remove empty objects from a nested Map"() {
        given:
        def input = [
                foo   : null,
                garply: ''
        ]

        when:
        def result = service.removeEmptyObjects(input)

        then:
        result == [
                foo:  null,
                garply: ''
        ]
    }

    def "should remove empty objects from a flat Map"() {
        given:
        def input = [
                foo: null,
                bar: null,
                baz: null,
                qux: null
        ]

        when:
        def result = service.removeEmptyObjects(input)

        then:
        result == [:]
    }

    def "should return an empty Map when the input is empty"() {
        given:
        def input = [:]

        when:
        def result = service.removeEmptyObjects(input)

        then:
        result == [:]
    }

    def "should return the original Map when there are no empty objects"() {
        given:
        def input = [
                foo: 1,
                bar: [baz: 2],
                qux: [:]
        ]

        when:
        def result = service.removeEmptyObjects(input)

        then:
        result == input
    }

    def "should return true when all values in a Map are empty"() {
        given:
        def input = [
                foo: null,
                bar: null,
                baz: null
        ]

        when:
        def result = service.allKeyValueOfObjectAreEmpty(input)

        then:
        result == false
    }

    def "should return false when not all values in a Map are empty"() {
        given:
        def input = [
                foo: null,
                bar: null,
                baz: null,
                qux: [
                        quux: [
                                corge: [
                                        grault: 'garply'
                                ]
                        ]
                ]
        ]

        when:
        def result = service.allKeyValueOfObjectAreEmpty(input)

        then:
        result == false
    }

    def "should return true when the input Map is empty"() {
        given:
        def input = [:]

        when:
        def result = service.allKeyValueOfObjectAreEmpty(input)

        then:
        result == true
    }

    def "should return true when the input Map is null"() {
        given:
        def input = null

        when:
        def result = service.allKeyValueOfObjectAreEmpty(input)

        then:
        result == false
    }
}
