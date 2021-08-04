package au.org.ala.ecodata.converter

import groovy.json.JsonSlurper
import spock.lang.Specification

class ListConverterSpec extends Specification {
    def "convert should create records for each list item in the output data model"() {
        setup:
        Map metadata = [
                record  : "true",
                name    : "actions",
                dataType: "list",
                columns : [
                        [
                                name    : "col1",
                                dataType: "text"
                        ],
                        [
                                dataType: "species",
                                name: "col2"
                        ]
                ]
        ]

        String col1 = """{"col1": "action1", "col2": {"outputSpeciesId": "species1"}}"""
        String col2 = """{"col1": "action2", "col2": {"outputSpeciesId": "species2"}}"""

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
        List<Map> result = new ListConverter().convert(new JsonSlurper().parseText(data).data, metadata)

        then:
        result.size() == 2
        result[0].outputItemId == 0
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
                                dwcAttribute: "individualCount",
                                dataType    : "text"
                        ],
                        [
                                name        : "col2",
                                dwcAttribute: "decimalLatitude",
                                dataType    : "text"
                        ],
                        [
                                name        : "col3",
                                dwcAttribute: "decimalLongitude",
                                dataType    : "text"
                        ],
                        [
                                name        : "col4",
                                dwcAttribute: "somethingElse",
                                dataType    : "text"
                        ],
                        [
                                name        : "col5",
                                dataType    : "species"
                        ]

                ]
        ]

        String col1 = """{"col1": "1", "col2": "1.1", "col3": "1.11", "col4": "foo", "col5": {"outputSpeciesId": "species1"}}"""
        String col2 = """{"col1": "2", "col2": "2.1", "col3": "2.11", "col4": "bar", "col5": {"outputSpeciesId": "species2"}}"""
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
        List<Map> result = new ListConverter().convert(new JsonSlurper().parseText(data).data, metadata)

        then:
        result.size() == 2
        result[0].outputItemId == 0
        result[0].individualCount == "1"
        result[0].decimalLatitude == "1.1"
        result[0].decimalLongitude == "1.11"
        result[0].somethingElse == "foo"
        result[1].outputItemId == 1
        result[1].individualCount == "2"
        result[1].decimalLatitude == "2.1"
        result[1].decimalLongitude == "2.11"
        result[1].somethingElse == "bar"
    }
}
