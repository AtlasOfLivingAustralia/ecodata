package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.groovy.JsonSlurper
import spock.lang.Specification

class SightingConverterSpec extends Specification {
    def "convert should create a single record with the relevant fields populated"() {
        setup:
        String data = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "user1",
                    "individualCount" : "1",
                    "decimalLatitude" : "2.1",
                    "decimalLongitude": "3.1"
            }
        }"""

        when:
        List<Map> result = new SingleSightingConverter().convert(new Activity(), new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].json.replaceAll("\\s", "") == data.replaceAll("\\s", "")
        result[0].individualCount == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
        result[0].userId == "user1"
    }

    def "convert should recognise locationLatitude and locationLongitude if decimalXYZ is not present"() {
        setup:
        String data = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "user1",
                    "individualCount" : "1",
                    "locationLatitude" : "2.1",
                    "locationLongitude": "3.1"
            }
        }"""

        when:
        List<Map> result = new SingleSightingConverter().convert(new Activity(), new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].json.replaceAll("\\s", "") == data.replaceAll("\\s", "")
        result[0].individualCount == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
        result[0].userId == "user1"
    }

    def "convert should handle numeric values of locationLatitude and locationLongitude"() {
        setup:
        String data = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "user1",
                    "individualCount" : "1",
                    "locationLatitude" : 2.1,
                    "locationLongitude": 3.1
            }
        }"""

        when:
        List<Map> result = new SingleSightingConverter().convert(new Activity(), new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].json.replaceAll("\\s", "") == data.replaceAll("\\s", "")
        result[0].individualCount == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
        result[0].userId == "user1"
    }

    def "convert should handle integer values of locationLatitude and locationLongitude"() {
        setup:
        String data = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "user1",
                    "individualCount" : "1",
                    "locationLatitude" : 2,
                    "locationLongitude": 3
            }
        }"""

        when:
        List<Map> result = new SingleSightingConverter().convert(new Activity(), new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].json.replaceAll("\\s", "") == data.replaceAll("\\s", "")
        result[0].individualCount == 1
        result[0].decimalLatitude == 2.0
        result[0].decimalLongitude == 3.0
        result[0].userId == "user1"
    }
}
