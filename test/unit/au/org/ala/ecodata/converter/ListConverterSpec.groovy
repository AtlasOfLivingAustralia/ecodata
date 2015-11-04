package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.groovy.JsonSlurper
import spock.lang.Specification

class ListConverterSpec extends Specification {
    def "convert should create records for each list item in the output data model with the json attribute set to the list item"() {
        setup:
        Map metadata = [
                record  : "true",
                name    : "actions",
                dataType: "list",
                columns : [
                        [
                                name: "col1"
                        ]
                ]
        ]

        String col1 = """{"col1": "action1"}"""
        String col2 = """{"col1": "action2"}"""
        String data = """{
            "activityId": "activity1",
            "name": "a",
            "data": {
                    "actions": [
                        ${col1},
                        ${col2}
                    ]
            }
        }"""

        when:
        List<Map> result = new ListConverter().convert(new Activity(), new JsonSlurper().parseText(data), metadata)

        then:
        result.size() == 2
        result[0].json.replaceAll("\\s", "") == col1.replaceAll("\\s", "")
        result[0].outputItemId == 0
        result[1].json.replaceAll("\\s", "") == col2.replaceAll("\\s", "")
        result[1].outputItemId == 1
    }

    def "convert should extract Record attributes from the dwcAttribute of each item in the data model"() {
        setup:
        Map metadata = [
                record  : "true",
                name    : "actions",
                dataType: "list",
                columns : [
                        [
                                name        : "col1",
                                dwcAttribute: "individualCount"
                        ],
                        [
                                name        : "col2",
                                dwcAttribute: "decimalLatitude"
                        ],
                        [
                                name        : "col3",
                                dwcAttribute: "decimalLongitude"
                        ],
                        [
                                name        : "col4",
                                dwcAttribute: "somethingElse"
                        ]
                ]
        ]

        String col1 = """{"col1": "1", "col2": "1.1", "col3": "1.11", "col4": "foo"}"""
        String col2 = """{"col1": "2", "col2": "2.1", "col3": "2.11", "col4": "bar"}"""
        String data = """{
            "activityId": "activity1",
            "name": "a",
            "data": {
                    "actions": [
                        ${col1},
                        ${col2}
                    ]
            }
        }"""

        when:
        List<Map> result = new ListConverter().convert(new Activity(), new JsonSlurper().parseText(data), metadata)

        then:
        result.size() == 2
        result[0].json.replaceAll("\\s", "") == col1.replaceAll("\\s", "")
        result[0].outputItemId == 0
        result[0].individualCount == "1"
        result[0].decimalLatitude == "1.1"
        result[0].decimalLongitude == "1.11"
        result[0].somethingElse == "foo"
        result[1].json.replaceAll("\\s", "") == col2.replaceAll("\\s", "")
        result[1].outputItemId == 1
        result[1].individualCount == "2"
        result[1].decimalLatitude == "2.1"
        result[1].decimalLongitude == "2.11"
        result[1].somethingElse == "bar"
    }
}
