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
                    "individualCount" : "1"
            }
        }"""

        when:
        List<Map> result = new SingleSightingConverter().convert(new JsonSlurper().parseText(data).data)

        then:
        result.size() == 1
        result[0].individualCount == 1
        result[0].userId == "user1"
    }
}
