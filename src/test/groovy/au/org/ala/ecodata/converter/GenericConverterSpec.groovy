package au.org.ala.ecodata.converter


import groovy.json.JsonSlurper
import spock.lang.Specification

class GenericConverterSpec extends Specification {

    def "convert should return a single record"() {
        setup:
        Map data =  [field1: "val1", field2: "val2", userId: "user1"]

        when:
        List<Map> result = new GenericFieldConverter().convert(data)

        then:
        result.size() == 1
    }


    def "convert should return latitude and longitude from location values"() {
        setup:
        Map data = [locationLatitude : "2.1", locationLongitude: "3.1"]

        when:
        List<Map> result = new GenericFieldConverter().convert(data)

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
    }

    def "convert should return latitude and longitude from decimal values"() {
        setup:
        Map data = [decimalLatitude : "2.1", decimalLongitude: "3.1"]

        when:
        List<Map> result = new GenericFieldConverter().convert(data)

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
    }

    def "convert should handle numeric values of locationLatitude and locationLongitude"() {
        setup:
        String data = """{
                "locationLatitude" : 2.1,
                "locationLongitude": 3.1
        }"""

        when:
        List<Map> result = new GenericFieldConverter().convert(new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
    }

    def "convert should handle integer values of locationLatitude and locationLongitude"() {
        setup:
        String data = """{
            "locationLatitude" : 2,
            "locationLongitude": 3
        }"""

        when:
        List<Map> result = new GenericFieldConverter().convert(new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.0
        result[0].decimalLongitude == 3.0
    }

    def "convert should handle expression to evaluate value of dwc attribute"() {
        setup:
        Map data = [
            "juvenile" : 2,
            "adult": 3
        ]

        Map metadata = [
                name: "juvenile",
                dwcAttribute: "individualCount",
                dwcExpression: "['context']['metadata']['description'] + ' ' + (['juvenile'] + ['adult'])",
                dwcDefault: 0,
                description: "Total number of individuals"
        ]
        GenericFieldConverter converter = new GenericFieldConverter()
        Map context = [:]

        when:
        List<Map> result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == "Total number of individuals 5"
    }

    def "convert should return expression if binding not found and no default value"() {
        setup:
        Map data = [
                "juvenile" : 2
        ]

        Map metadata = [
                name: "juvenile",
                dwcAttribute: "individualCount",
                dwcExpression: "['juvenile'] + ['adult']",
                returnType: "java.lang.Integer",
                description: "Total number of individuals"
        ]
        GenericFieldConverter converter = new GenericFieldConverter()

        when:
        List<Map> result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == "['juvenile'] + ['adult']"

        when:
        data = [
                "juvenile" : 2
        ]
        metadata.dwcDefault = 1
        result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == 1
    }

    def "expression should check for null value"() {
        setup:
        Map data = [
                "juvenile" : 2,
                adult: null
        ]

        Map metadata = [
                name: "juvenile",
                dwcAttribute: "individualCount",
                dwcExpression: "(['juvenile'] != null ? ['juvenile'] : 0)  + (['adult'] != null ? ['adult'] : 0)",
                returnType: "java.lang.Integer",
                dwcDefault: 0,
                description: "Total number of individuals"
        ]
        GenericFieldConverter converter = new GenericFieldConverter()

        when:
        List<Map> result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == 2
    }
}
