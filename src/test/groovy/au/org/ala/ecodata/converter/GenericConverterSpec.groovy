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



}
