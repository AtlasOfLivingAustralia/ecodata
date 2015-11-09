package au.org.ala.ecodata.converter

import net.sf.json.groovy.JsonSlurper
import spock.lang.Specification

class MasterDetailConverterSpec extends Specification {

    def "should delegate the conversion to the appropriate converter for the detail data"() {
        setup:
        String data = """{
                      "activityId": "activity1",
                      "name": "Multiple Sightings",
                      "data": {
                        "multipleSightings": [
                          {
                            "userId": "user1",
                            "individualCount": "1",
                            "decimalLatitude": "2.1",
                            "decimalLongitude": "3.1"
                          },
                          {
                            "userId": "user11",
                            "individualCount": "11",
                            "decimalLatitude": "12.1",
                            "decimalLongitude": "13.1"
                          }
                        ]
                      }
                    }"""

        Map metadata = [name: "multipleSightings", master: [:], detail: [dataType: "singleSighting"]]

        when:
        List<Map> result = new MasterDetailConverter().convert(new JsonSlurper().parseText(data).data, metadata)

        then:
        result.size() == 2
        result[0].individualCount == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
        result[0].userId == "user1"

        result[1].individualCount == 11
        result[1].decimalLatitude == 12.1
        result[1].decimalLongitude == 13.1
        result[1].userId == "user11"
    }
}
