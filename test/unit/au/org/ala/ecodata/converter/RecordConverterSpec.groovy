package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.Output
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.ProjectActivity
import au.org.ala.ecodata.Site
import groovy.json.JsonSlurper
import spock.lang.Specification

class RecordConverterSpec extends Specification {

    JsonSlurper json = new JsonSlurper()

    def "convert should create a single record field set from an output model with 1 single-item data model"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
        Activity activity = new Activity()
        Output output = new Output()
        Map outputMetadata = [record: true, dataModel: [[dataType: "text", dwcAttribute: "attribute1", name: "someField"], [dataType: "species", name: "species"]]]
        Map submittedData = [ someField: "fieldValue", species: ["outputSpeciesId": "anhotherid"]]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 1
        fieldsets[0].attribute1 == "fieldValue"
    }

    def "convert should create a single record field set with all fields from an output model with 1 single-item data model"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
        Activity activity = new Activity()
        Output output = new Output()
        Map outputMetadata = [record: true, dataModel: [
                [dataType: "text", dwcAttribute: "attribute1", name: "field1"],
                [dataType: "text", dwcAttribute: "attribute2", name: "field2"],
                [dataType: "species", dwcAttribute: "attribute3", name: "field3"]
        ]]
        Map submittedData = [field1: "fieldValue1", field2: "fieldValue2", field3: ["outputSpeciesId": "anotherid"]]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 1
        fieldsets[0].attribute1 == "fieldValue1"
        fieldsets[0].attribute2 == "fieldValue2"
        fieldsets[0].outputSpeciesId == "anotherid"
    }

    def "convert should create a two record field sets with all fields from an output model with 1 single-item data model and two species fields"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
        Activity activity = new Activity()
        Output output = new Output()
        Map outputMetadata = [record: true, dataModel: [
                [dataType: "text", dwcAttribute: "attribute1", name: "field1"],
                [dataType: "text", dwcAttribute: "attribute2", name: "field2"],
                [dataType: "species", dwcAttribute: "attribute3", name: "field3"],
                [dataType: "species", dwcAttribute: "attribute4", name: "field4"]
        ]]
        Map submittedData = [field1: "fieldValue1", field2: "fieldValue2", field3: ["outputSpeciesId": "anotheridField3"], field4: ["outputSpeciesId": "anotheridField4"]]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 2
        fieldsets[0].attribute1 == "fieldValue1"
        fieldsets[0].attribute2 == "fieldValue2"
        fieldsets[0].outputSpeciesId == "anotheridField3"

        fieldsets[1].attribute1 == "fieldValue1"
        fieldsets[1].attribute2 == "fieldValue2"
        fieldsets[1].outputSpeciesId == "anotheridField4"

    }


    def "convert should create a record field set with multiple fields for each item in an output model with a 'list' data model"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
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
                                      "dataType": "species",
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
                                  "col2": {"outputSpeciesId": "anotherid1"}
                                },
                                {
                                  "col1": "row2col1",
                                  "col2": {"outputSpeciesId": "anotherid2"}
                                }
                              ]
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 2
        fieldsets[0].attribute1 == "row1col1"
        fieldsets[0].outputSpeciesId == "anotherid1"
        fieldsets[1].attribute1 == "row2col1"
        fieldsets[1].outputSpeciesId == "anotherid2"
    }

    def "convert should create a record field set with multiple fields for each species per item in an output model with a 'list' data model"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
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
                                      "dataType": "species",
                                      "name": "col2",
                                      "dwcAttribute": "attribute2"
                                    },
                                    {
                                      "dataType": "species",
                                      "name": "col3",
                                      "dwcAttribute": "attribute3"
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
                                  "col2": {"outputSpeciesId": "anotheridRow1col2"},
                                  "col3": {"outputSpeciesId": "anotheridRow1col3"}
                                },
                                {
                                  "col1": "row2col1",
                                  "col2": {"outputSpeciesId": "anotheridRow2col2"},
                                  "col3": {"outputSpeciesId": "anotheridRow2col3"}
                                }
                              ]
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 4
        fieldsets[0].attribute1 == "row1col1"
        fieldsets[0].outputSpeciesId == "anotheridRow1col2"
        fieldsets[0].outputItemId == 0
        fieldsets[1].attribute1 == "row1col1"
        fieldsets[1].outputSpeciesId == "anotheridRow1col3"
        fieldsets[1].outputItemId == 1
        fieldsets[2].attribute1 == "row2col1"
        fieldsets[2].outputSpeciesId == "anotheridRow2col2"
        fieldsets[2].outputItemId == 2
        fieldsets[3].attribute1 == "row2col1"
        fieldsets[3].outputSpeciesId == "anotheridRow2col3"
        fieldsets[3].outputItemId == 3
    }


    def "convert should add all single-item dataModel values to each record field set in an output model with a 'list' data model"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
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
                                      "dataType": "species",
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
                                  "dataType": "species",
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
                                  "col2": {"outputSpeciesId": "row1col2"}
                                },
                                {
                                  "col1": "row2col1",
                                  "col2": {"outputSpeciesId": "row2col2"}
                                }
                              ],
                              "singleItemField1": "singleItemValue1",
                              "singleItemField2": {"outputSpeciesId": "singleItemValue2"}
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 3

        fieldsets[0].attribute1 == null
        fieldsets[0].attribute2 == null
        fieldsets[0].attribute3 == "singleItemValue1"
        fieldsets[0].outputSpeciesId == "singleItemValue2"


        fieldsets[1].attribute1 == "row1col1"
        fieldsets[1].outputSpeciesId == "row1col2"
        fieldsets[1].attribute3 == "singleItemValue1"
        fieldsets[2].attribute1 == "row2col1"
        fieldsets[2].outputSpeciesId == "row2col2"
        fieldsets[2].attribute3 == "singleItemValue1"
    }

    def "convert should populate the record field set with the related object ids"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
        Activity activity = new Activity(activityId: "activityId", projectActivityId: "projectActivityId", projectId: "projectId", userId: "user1")
        Output output = new Output(outputId: "outputId")
        Map outputMetadata = [record: true, dataModel: [[dataType: "species", dwcAttribute: "someAttribute", name: "someField"]]]
        Map submittedData = [someField: [outputSpeciesId: "someFieldId"]]

        when:
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then:
        fieldsets.size() == 1
        fieldsets[0].activityId == "activityId"
        fieldsets[0].projectActivityId == "projectActivityId"
        fieldsets[0].projectId == "projectId"
        fieldsets[0].userId == "user1"
        fieldsets[0].outputId == "outputId"
    }

    def "convert should override fields which appear in multiple components"() {
        setup:
        Project project = new Project()
        Site site = new Site()
        ProjectActivity projectActivity = new ProjectActivity()
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
                                    },
                                    {
                                      "dataType": "species",
                                      "name" : "innerSpeciesField"
                                    }
                                  ]
                                },
                                {
                                  "dataType": "text",
                                  "name": "field2",
                                  "dwcAttribute": "dwc"
                                },
                                {
                                  "dataType": "species",
                                  "name" : "speciesField"
                                }

                              ]
                            }
                            """
        String submittedDataJson = """
                            {
                              "mylist": [
                                {
                                  "field1": "secondValue",
                                  "innerSpeciesField" : {"outputSpeciesId": "InnerspeciesFieldIdValue"}
                                }
                              ],
                              "field2": "firstValue",
                              "speciesField" : {"outputSpeciesId": "speciesFieldIdValue"}
                            }
                            """

        Map outputMetadata = json.parseText(metadataJson) as Map
        Map submittedData = json.parseText(submittedDataJson) as Map

        when: "two different fields are mapped to the same DwC attribute"
        List<Map> fieldsets = RecordConverter.convertRecords(project, site, projectActivity, activity, output, submittedData, outputMetadata)

        then: "two records will be created, the second record will override value in general record in the resulting record field set"
        fieldsets.size() == 2
        fieldsets[0].dwc == "firstValue"
        fieldsets[1].dwc == "secondValue"
    }
}
