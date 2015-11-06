package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.Output
import groovy.json.JsonSlurper
import spock.lang.Specification

class RecordConverterSpec extends Specification {

    JsonSlurper json = new JsonSlurper()

    def "convert should create a single record field set from an output model with 1 single-item data model"() {
        setup:
        Activity activity = new Activity()
        Output output = new Output()
        Map outputMetadata = [record: true, dataModel: [[dataType: "text", dwcAttribute: "someAttribute", name: "someField"]]]
        Map submittedData = [someField: "fieldValue"]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 1
        fieldsets[0].someAttribute == "fieldValue"
    }

    def "convert should create a single record field set with all fields from an output model with 1 single-item data model"() {
        setup:
        Activity activity = new Activity()
        Output output = new Output()
        Map outputMetadata = [record: true, dataModel: [
                [dataType: "text", dwcAttribute: "attribute1", name: "field1"],
                [dataType: "text", dwcAttribute: "attribute2", name: "field2"],
                [dataType: "number", dwcAttribute: "attribute3", name: "field3"]
        ]]
        Map submittedData = [field1: "fieldValue1", field2: "fieldValue2", field3: "fieldValue3"]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 1
        fieldsets[0].attribute1 == "fieldValue1"
        fieldsets[0].attribute2 == "fieldValue2"
        fieldsets[0].attribute3 == "fieldValue3"
    }

    def "convert should create a record field set with multiple fields for each item in an output model with a 'list' data model"() {
        setup:
        Activity activity = new Activity()
        Output output = new Output()

        String metadataJson = """
                            {
                              "record": "true",
                              "dataModel": [
                                {
                                  "dataType": "list",
                                  "name": "mylist",
                                  "columns": [
                                    {
                                      "dataType": "text",
                                      "name": "col1",
                                      "dwcAttribute": "attribute1"
                                    },
                                    {
                                      "dataType": "text",
                                      "name": "col2",
                                      "dwcAttribute": "attribute2"
                                    }
                                  ]
                                }
                              ]
                            }
                            """
        String submittedDataJson = """
                            {
                              "mylist": [
                                {
                                  "col1": "row1col1",
                                  "col2": "row1col2"
                                },
                                {
                                  "col1": "row2col1",
                                  "col2": "row2col2"
                                }
                              ]
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 2
        fieldsets[0].attribute1 == "row1col1"
        fieldsets[0].attribute2 == "row1col2"
        fieldsets[1].attribute1 == "row2col1"
        fieldsets[1].attribute2 == "row2col2"
    }

    def "convert should add all single-item dataModel values to each record field set in an output model with a 'list' data model"() {
        setup:
        Activity activity = new Activity()
        Output output = new Output()

        String metadataJson = """
                            {
                              "record": "true",
                              "dataModel": [
                                {
                                  "dataType": "list",
                                  "name": "mylist",
                                  "columns": [
                                    {
                                      "dataType": "text",
                                      "name": "col1",
                                      "dwcAttribute": "attribute1"
                                    },
                                    {
                                      "dataType": "text",
                                      "name": "col2",
                                      "dwcAttribute": "attribute2"
                                    }
                                  ]
                                },
                                {
                                  "dataType": "text",
                                  "name": "singleItemField1",
                                  "dwcAttribute": "attribute3"
                                },
                                {
                                  "dataType": "text",
                                  "name": "singleItemField2",
                                  "dwcAttribute": "attribute4"
                                }
                              ]
                            }
                            """
        String submittedDataJson = """
                            {
                              "mylist": [
                                {
                                  "col1": "row1col1",
                                  "col2": "row1col2"
                                },
                                {
                                  "col1": "row2col1",
                                  "col2": "row2col2"
                                }
                              ],
                              "singleItemField1": "singleItemValue1",
                              "singleItemField2": "singleItemValue2"
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 2
        fieldsets[0].attribute1 == "row1col1"
        fieldsets[0].attribute2 == "row1col2"
        fieldsets[0].attribute3 == "singleItemValue1"
        fieldsets[0].attribute4 == "singleItemValue2"
        fieldsets[1].attribute1 == "row2col1"
        fieldsets[1].attribute2 == "row2col2"
        fieldsets[1].attribute3 == "singleItemValue1"
        fieldsets[1].attribute4 == "singleItemValue2"
    }

    def "convert should populate the record field set with the related object ids"() {
        setup:
        Activity activity = new Activity(activityId: "activityId", projectActivityId: "projectActivityId", projectId: "projectId", userId: "user1")
        Output output = new Output(outputId: "outputId")
        Map outputMetadata = [record: true, dataModel: [[dataType: "text", dwcAttribute: "someAttribute", name: "someField"]]]
        Map submittedData = [someField: "fieldValue"]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 1
        fieldsets[0].activityId == "activityId"
        fieldsets[0].projectActivityId == "projectActivityId"
        fieldsets[0].projectId == "projectId"
        fieldsets[0].userId == "user1"
        fieldsets[0].outputId == "outputId"
    }

    def "convert should concatenate fields which appear in multiple components"() {
        setup:
        Activity activity = new Activity()
        Output output = new Output()

        String metadataJson = """
                            {
                              "record": "true",
                              "dataModel": [
                                {
                                  "dataType": "list",
                                  "name": "mylist",
                                  "columns": [
                                    {
                                      "dataType": "text",
                                      "name": "field1",
                                      "dwcAttribute": "dwc"
                                    }
                                  ]
                                },
                                {
                                  "dataType": "text",
                                  "name": "field2",
                                  "dwcAttribute": "dwc"
                                }
                              ]
                            }
                            """
        String submittedDataJson = """
                            {
                              "mylist": [
                                {
                                  "field1": "firstValue"
                                }
                              ],
                              "field2": "secondValue"
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when: "two different fields are mapped to the same DwC attribute"
        List<Map> fieldsets = RecordConverter.convertRecords(activity, output, submittedData, outputMetadata)

        then: "the two values should be concatenated together in the resulting record field set"
        fieldsets.size() == 1
        fieldsets[0].dwc == "firstValue;secondValue"
    }
}
