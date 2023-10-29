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

    def "should not remove none of primitive types from Map"() {
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
                bar: [a: null, b: null]
        ]

        when:
        def result = service.removeEmptyObjects(input)

        then:
        result == [foo: null]
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
        result == true
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

    def "convertDotNotationToObject: should convert dot notation keys to nested objects"() {
        given:
        def json = [:]

        when:
        service.convertDotNotationToObject(json, "foo.bar.baz", "value")

        then:
        json == [foo: [bar: [baz: "value"]]]
    }

    def "convertDotNotationToObject: should update existing inner key values"() {
        given:
        def json = [foo: [bar: [baz: "oldValue"]]]

        when:
        service.convertDotNotationToObject(json, "foo.bar.baz", "newValue")

        then:
        json == [foo: [bar: [baz: "newValue"]]]
    }

    def "convertDotNotationToObject: should add new inner keys to existing maps"() {
        given:
        def json = [foo: [bar: [:]]]

        when:
        service.convertDotNotationToObject(json, "foo.bar.baz", "value")

        then:
        json == [foo: [bar: [baz: "value"]]]
    }

    def "getDataHeaders: should return the headers for a given sheet"() {
        given:
        def workbook = WorkbookFactory.create(new File("src/test/resources/bulk_import_example.xlsx").newInputStream())
        def sheet = workbook.getSheetAt(0)

        when:
        def headers = service.getDataHeaders(sheet)

        then:
        headers == [A: 'serial', B: 'a', C: 'b.c', D: 'd.e', E:'d.f.name', F:'d.f.scientificName', G:'d.f.commonName', H:'d.f.guid', I:'g.h', J:'g.i.name', K:'g.i.scientificName', L:'g.i.commonName', M:'g.i.guid']
    }
}
