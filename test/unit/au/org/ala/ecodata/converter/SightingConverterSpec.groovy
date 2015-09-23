package au.org.ala.ecodata.converter

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
        List<Map> result = new SingleSightingConverter().convert(new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].json.replaceAll("\\s", "") == data.replaceAll("\\s", "")
        result[0].individualCount == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
        result[0].userId == "user1"
    }
}
