package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

import static au.org.ala.ecodata.Status.DELETED

class OutputServiceSpec extends IntegrationSpec {

    OutputService outputService
    MetadataService metadataService
    RecordService recordService
    def grailsApplication

    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }

        metadataService = Mock(MetadataService)
        recordService = Mock(RecordService)
        outputService.metadataService = metadataService
        outputService.recordService = recordService
    }

    void "test create output with no parent activity"() {
        when:
        def response = outputService.create(data: [prop2: 'prop2'])
        then:
        response.error != null
        response.status == 'error'
    }

    void "test create output"() {
        setup:
        def activityId = 'a test activity id'
        Activity activity = new Activity(activityId: activityId, type: 'Revegetation', description: 'A test activity')
        activity.save(flush: true, failOnError: true)

        when:
        def response = outputService.create(activityId: activityId, data: [prop2: 'prop2'])
        def outputId = response.outputId
        then:
        outputId != null
        response.status == 'ok'

        when: "retrieving the saved output"
        Output savedOutput = Output.findByOutputId(outputId)

        then:
        savedOutput.outputId == outputId
        savedOutput.activityId == activityId
        savedOutput['data']['prop2'] == 'prop2'
    }

    def "createOrUpdateRecordsForOutput should create associated Record objects"() {
        setup:
        metadataService.getOutputDataModelByName(_) >> [record: true, dataModel: [[dataType: "doesNotMatter"], [dataType: "doesNotMatter"]]]

        Output output = new Output(outputId: "output1")
        Activity activity = new Activity(activityId: "activity1", projectActivityId: "projAct1", projectId: "project1", userId: "user1")
        Map properties = [data: [userId: "666"]]

        when:
        outputService.createOrUpdateRecordsForOutput(activity, output, properties)

        then:
        1 * outputService.recordService.createRecord(_) >> { argument ->
            assert argument.activityId[0] == "activity1"
            assert argument.outputId[0] == "output1"
            assert argument.projectActivityId[0] == "projAct1"
            assert argument.projectId[0] == "project1"
            assert argument.userId[0] == "user1"

            [null, null]
        }
    }


    def "createOrUpdateRecordsForOutput should create associated Record objects accordingly"() {
        setup:

        Map outputMetadataModel =
                [
                        "record"   : true,
                        "modelName": "MDBA_iconic_species_sightings",
                        "dataModel": [[
                                              "dataType"    : "date",
                                              "description" : "The date on which the sighting was made.",
                                              "name"        : "surveyDate",
                                              "dwcAttribute": "eventDate",
                                              "validate"    : "required"
                                      ], [
                                              "dataType"    : "time",
                                              "description" : "The time at which the sighting was made.",
                                              "name"        : "surveyStartTime",
                                              "dwcAttribute": "eventTime"
                                      ], [
                                              "dataType"    : "text",
                                              "description" : "General remarks about the survey event, including any characteristic site features, conditions, etc.",
                                              "name"        : "notes",
                                              "dwcAttribute": "eventRemarks"
                                      ], [
                                              "dataType"    : "text",
                                              "description" : "The name of the person who is attributed with making the sighting.",
                                              "name"        : "recordedBy",
                                              "dwcAttribute": "recordedBy"
                                      ], [
                                              "dataType"    : "geoMap",
                                              "name"        : "location",
                                              "columns"     : [[
                                                                       "source"      : "locationLatitude",
                                                                       "dwcAttribute": "verbatimLatitude"
                                                               ], [
                                                                       "source"      : "locationLongitude",
                                                                       "dwcAttribute": "verbatimLongitude"
                                                               ]
                                              ],
                                              "dwcAttribute": "verbatimCoordinates",
                                              "validate"    : "required"
                                      ], [
                                              "dataType"    : "species",
                                              "description" : "The species name of the plant, animal or fungus observed.",
                                              "name"        : "species1",
                                              "dwcAttribute": "scientificName",
                                              "validate"    : "required"
                                      ], [
                                              "dataType"    : "number",
                                              "description" : "The number of individuals or colonies (for certain insects).",
                                              "name"        : "individualCount1",
                                              "dwcAttribute": "individualCount",
                                              "validate"    : "integer,min[0]"
                                      ], [
                                              "dataType"   : "text",
                                              "description": "How certain are you that you have correctly identified your sighting? Only choose 'certain' if you are 100% sure.",
                                              "name"       : "identificationConfidence1",
                                              "constraints": ["Certain", "Uncertain"]
                                      ], [
                                              "dataType"    : "text",
                                              "description" : "Select appropriate category if the sighting is an animal and it is possible to identify gender.",
                                              "name"        : "sex1",
                                              "constraints" : ["Male", "Female", "Male and female", "Unknown"],
                                              "dwcAttribute": "sex"
                                      ], [
                                              "dataType"   : "text",
                                              "description": "Indicate the health status of the organism sighted.",
                                              "name"       : "status1",
                                              "constraints": ["Alive", "Dead", "Sick or injured"]
                                      ], [
                                              "dataType"    : "text",
                                              "description" : "Observation notes about the record.",
                                              "name"        : "comments1",
                                              "dwcAttribute": "notes"
                                      ], [
                                              "dataType"   : "text",
                                              "description": "Indicate whether you have seen this species at this location previously.",
                                              "name"       : "previousSightings1",
                                              "constraints": ["Up to 1 week ago", "1 Week to 1 month ago", "1 Month to 6 months ago", "1 Year or longer ago", "Never seen here before"]
                                      ], [
                                              "dataType"   : "image",
                                              "description": "Upload a photo taken of the species at the time of the record. This is essential for verification of the record.",
                                              "name"       : "sightingPhoto1",
                                              "validate"   : "required"
                                      ], [
                                              "dataType": "list",
                                              "name"    : "multiSightingTable",
                                              "columns" : [[
                                                                   "dataType"    : "species",
                                                                   "description" : "The species name of the plant, animal or fungus observed.",
                                                                   "name"        : "species2",
                                                                   "dwcAttribute": "scientificName"
                                                           ], [
                                                                   "dataType"    : "number",
                                                                   "description" : "The number of individuals or colonies (for certain insects).",
                                                                   "name"        : "individualCount2",
                                                                   "dwcAttribute": "individualCount",
                                                                   "validate"    : "integer,min[0]"
                                                           ], [
                                                                   "dataType"   : "text",
                                                                   "description": "How certain are you that you have correctly identified your sighting? Only choose 'certain' if you are 100% sure.",
                                                                   "name"       : "identificationConfidence2",
                                                                   "constraints": ["Certain", "Uncertain"]
                                                           ], [
                                                                   "dataType"    : "text",
                                                                   "description" : "Select appropriate category if the sighting is an animal and it is possible to identify gender.",
                                                                   "name"        : "sex2",
                                                                   "constraints" : ["Male", "Female", "Male and female", "Unknown"],
                                                                   "dwcAttribute": "sex"
                                                           ], [
                                                                   "dataType"   : "text",
                                                                   "description": "Indicate the health status of the organism sighted.",
                                                                   "name"       : "status2",
                                                                   "constraints": ["Alive", "Dead", "Sick or injured"]
                                                           ], [
                                                                   "dataType"    : "text",
                                                                   "description" : "Observation notes about the record.",
                                                                   "name"        : "comments2",
                                                                   "dwcAttribute": "notes"
                                                           ], [
                                                                   "dataType"   : "text",
                                                                   "description": "Indicate whether you have seen this species at this location previously.",
                                                                   "name"       : "previousSightings2",
                                                                   "constraints": ["Up to 1 week ago", "1 Week to 1 month ago", "1 Month to 6 months ago", "1 Year or longer ago", "Never seen here before"]
                                                           ], [
                                                                   "dataType"   : "image",
                                                                   "description": "Upload a photo taken of the species at the time of the record. This is essential for verification of the record.",
                                                                   "name"       : "sightingPhoto2"
                                                           ]
                                              ]
                                      ]
                        ],

                ]




        metadataService.getOutputDataModelByName(_) >> outputMetadataModel

        Output output = new Output(outputId: "output1")
        Activity activity = new Activity(activityId: "activity1", projectActivityId: "projAct1", projectId: "project1", userId: "user1")
        Map properties =

                [
                        "activityId"                              : "activity1",
                        "name"                                    : "Basin Champions - Iconic Species",
                        "data"                                    : [
                                "identificationConfidence1": "Certain",
                                "status1"                  : "Alive",
                                "surveyStartTime"          : "12:10 PM",
                                "multiSightingTable"       : [

                                        [
                                                "previousSightings2"       : "1 Week to 1 month ago",
                                                "identificationConfidence2": "Uncertain",
                                                "status2"                  : "Dead",
                                                "individualCount2"         : "2",
                                                "sex2"                     : "Female",
                                                "comments2"                : "More Species Sightings First",
                                                "sightingPhoto2"           : [[
                                                                                      "filepath"     : "2016-03",
                                                                                      "status"       : "active",
                                                                                      "outputId"     : "298eae02-d33c-4431-8d43-917087c38ccf",
                                                                                      "contentType"  : "image/png",
                                                                                      "type"         : "image",
                                                                                      "formattedSize": "3.54 KB",
                                                                                      "activityId"   : "e8590fe5-768e-463c-b2c2-37a547d4df87",
                                                                                      "dateTaken"    : "2016-03-01T13:00:00.000Z",
                                                                                      "filesize"     : 3538,
                                                                                      "name"         : "2.png",
                                                                                      "role"         : "surveyImage",
                                                                                      "filename"     : "2.png",
                                                                                      "notes"        : "",
                                                                                      "identifier"   : "http://devt.ala.org.au:8087/biocollect/image?id=2.png",
                                                                                      "documentId"   : "7282ad1d-a346-4c1c-a2bb-00b75bc5b8de",
                                                                                      "attribution"  : ""
                                                                              ]
                                                ],
                                                "species2"                 : [
                                                        "guid"           : "urn:lsid:biodiversity.org.au:afd.taxon:a1fbd43e-2093-4675-bdd2-783afb82aef8",
                                                        "outputSpeciesId": "16cb0ae5-fdd4-46d3-a753-925b6abb4c43",
                                                        "name"           : "Abcandonopsis Karanovic, 2004"
                                                ]
                                        ],
                                        [
                                                "previousSightings2"       : "1 Month to 6 months ago",
                                                "identificationConfidence2": "Certain",
                                                "status2"                  : "Sick or injured",
                                                "individualCount2"         : "3",
                                                "sex2"                     : "Male and female",
                                                "comments2"                : "More Species Sightings Second",
                                                "sightingPhoto2"           : [[
                                                                                      "filepath"     : "2016-03",
                                                                                      "status"       : "active",
                                                                                      "outputId"     : "298eae02-d33c-4431-8d43-917087c38ccf",
                                                                                      "contentType"  : "image/png",
                                                                                      "type"         : "image",
                                                                                      "formattedSize": "3.19 KB",
                                                                                      "activityId"   : "e8590fe5-768e-463c-b2c2-37a547d4df87",
                                                                                      "dateTaken"    : "2016-03-02T13:00:00.000Z",
                                                                                      "filesize"     : 3189,
                                                                                      "name"         : "3.png",
                                                                                      "role"         : "surveyImage",
                                                                                      "filename"     : "3.png",
                                                                                      "notes"        : "",
                                                                                      "identifier"   : "http://devt.ala.org.au:8087/biocollect/image?id=3.png",
                                                                                      "documentId"   : "3f7f2ac7-a776-4a76-8051-30bd7c3cc8a2",
                                                                                      "attribution"  : ""
                                                                              ]
                                                ],
                                                "species2"                 : [
                                                        "guid"           : "urn:lsid:biodiversity.org.au:apni.taxon:759444",
                                                        "outputSpeciesId": "a1b7d9ec-f5da-49bb-9918-3494e0ef6ad0",
                                                        "name"           : "Sida sp. B (C.Dunlop 1739)"
                                                ]
                                        ], [
                                                "previousSightings2"       : "1 Year or longer ago",
                                                "identificationConfidence2": "Uncertain",
                                                "status2"                  : "Alive",
                                                "individualCount2"         : "4",
                                                "sex2"                     : "Unknown",
                                                "comments2"                : "More Species Sightings Third",
                                                "sightingPhoto2"           : [[
                                                                                      "filepath"     : "2016-03",
                                                                                      "status"       : "active",
                                                                                      "outputId"     : "298eae02-d33c-4431-8d43-917087c38ccf",
                                                                                      "contentType"  : "image/png",
                                                                                      "type"         : "image",
                                                                                      "formattedSize": "2.33 KB",
                                                                                      "activityId"   : "e8590fe5-768e-463c-b2c2-37a547d4df87",
                                                                                      "dateTaken"    : "2016-03-04T01:29:16Z",
                                                                                      "filesize"     : 2325,
                                                                                      "name"         : "4.png",
                                                                                      "role"         : "surveyImage",
                                                                                      "filename"     : "4.png",
                                                                                      "notes"        : "",
                                                                                      "identifier"   : "http://devt.ala.org.au:8087/biocollect/image?id=4.png",
                                                                                      "documentId"   : "738ba822-81cb-4e9a-a54d-17b719c6aef8",
                                                                                      "attribution"  : ""
                                                                              ]
                                                ],
                                                "species2"                 : [
                                                        "guid"           : "urn:lsid:catalogueoflife.org:taxon:dbc96dea-29c1-102b-9a4a-00304854f820:col20120124",
                                                        "outputSpeciesId": "94276a62-fa80-489b-bb62-dbd8ad09ac99",
                                                        "name"           : "Deflexula pacifica"
                                                ]
                                        ]
                                ],
                                "species1"                 : [
                                        "guid"           : "urn:lsid:biodiversity.org.au:afd.taxon:0fb1b639-10cf-4360-afa2-6f97a4491273",
                                        "outputSpeciesId": "1717aab2-cc1d-46ef-8f3b-56ca77be1d89",
                                        "name"           : "Tigrana"
                                ],
                                "locationLongitude"        : 146.10350847244263,
                                "comments1"                : "Single Species Sighting Comment",
                                "sightingPhoto1"           : [[
                                                                      "filepath"     : "2016-03",
                                                                      "status"       : "active",
                                                                      "outputId"     : "298eae02-d33c-4431-8d43-917087c38ccf",
                                                                      "contentType"  : "image/png",
                                                                      "type"         : "image",
                                                                      "formattedSize": "9.07 KB",
                                                                      "activityId"   : "e8590fe5-768e-463c-b2c2-37a547d4df87",
                                                                      "dateTaken"    : "2016-02-29T13:00:00.000Z",
                                                                      "filesize"     : 9072,
                                                                      "name"         : "1.png",
                                                                      "role"         : "surveyImage",
                                                                      "filename"     : "1.png",
                                                                      "notes"        : "",
                                                                      "identifier"   : "http://devt.ala.org.au:8087/biocollect/image?id=1.png",
                                                                      "documentId"   : "a3190c14-a18d-493d-bfcd-e226331815ec",
                                                                      "attribution"  : ""
                                                              ]
                                ],
                                "sex1"                     : "Male",
                                "previousSightings1"       : "Up to 1 week ago",
                                "locationLatitude"         : -41.68385191046434,
                                "notes"                    : "Notes general",
                                "surveyDate"               : "2016-03-03T13:00:00Z",
                                "individualCount1"         : "1"
                        ],
                        "multiSightingTableTableDataUploadOptions": [
                                "uploadTemplateId"  : "multiSightingTabletemplate-upload",
                                "formData"          : [
                                        "listName": "multiSightingTable",
                                        "type"    : "Basin Champions - Iconic Species"
                                ],
                                "downloadTemplateId": "multiSightingTabletemplate-download",
                                "url"               : "/biocollect/bioActivity/extractDataFromExcelTemplate?pActivityId=710df61d-56e9-48f5-8671-8ea7b25a3ad6"
                        ],
                        "appendTableRows"                         : true,
                        "multiSightingTableTableDataUploadVisible": false,
                        "outputNotCompleted"                      : false
                ]

        when:
        outputService.createOrUpdateRecordsForOutput(activity, output, properties)

        then:
        outputService.recordService.createRecord(_) >> { argument ->

            //Let's cover the basics
            assert argument.activityId[0] == "activity1"
            assert argument.outputId[0] == "output1"
            assert argument.projectActivityId[0] == "projAct1"
            assert argument.projectId[0] == "project1"
            assert argument.userId[0] == "user1"

            // Still common but we need to ensure they comply
            assert argument?.json[0] == null
//            argument.decimalLatitude[0] == -41.68385191046434
//            argument.decimalLongitude[0] == 146.10350847244263

            // And then the particular information
            argument.scientificName[0] == "Tigrana"
            argument.multimedia.filename[0] == "1.png"
            argument.individualCount[0] == 1

            [null, null]
        }

        then:
        outputService.recordService.createRecord(_) >> { argument ->

            //Let's cover the basics
            assert argument.activityId[0] == "activity1"
            assert argument.outputId[0] == "output1"
            assert argument.projectActivityId[0] == "projAct1"
            assert argument.projectId[0] == "project1"
            assert argument.userId[0] == "user1"

            // Still common but we need to ensure they comply
            assert argument?.json[0] == null
//            argument.decimalLatitude[0] == -41.68385191046434
//            argument.decimalLongitude[0] == 146.10350847244263

            // And then the particular information
            argument.scientificName[0] == "Abcandonopsis Karanovic, 2004"
            argument.multimedia.filename[0] == "2.png"
            argument.individualCount[0] == 1

            [null, null]
        }


        then:
        outputService.recordService.createRecord(_) >> { argument ->

            //Let's cover the basics
            assert argument.activityId[0] == "activity1"
            assert argument.outputId[0] == "output1"
            assert argument.projectActivityId[0] == "projAct1"
            assert argument.projectId[0] == "project1"
            assert argument.userId[0] == "user1"

            // Still common but we need to ensure they comply
            assert argument?.json[0] == null
//            argument.decimalLatitude[0] == -41.68385191046434
//            argument.decimalLongitude[0] == 146.10350847244263

            // And then the particular information
            argument.scientificName[0] == "Sida sp. B (C.Dunlop 1739)"
            argument.multimedia.filename[0] == "3.png"
            argument.individualCount[0] == 1

            [null, null]
        }

        then:
        outputService.recordService.createRecord(_) >> { argument ->

            //Let's cover the basics
            assert argument.activityId[0] == "activity1"
            assert argument.outputId[0] == "output1"
            assert argument.projectActivityId[0] == "projAct1"
            assert argument.projectId[0] == "project1"
            assert argument.userId[0] == "user1"

            // Still common but we need to ensure they comply
            assert argument?.json[0] == null
//            argument.decimalLatitude[0] == -41.68385191046434
//            argument.decimalLongitude[0] == 146.10350847244263

            // And then the particular information
            argument.scientificName[0] == "Deflexula pacifica"
            argument.multimedia.filename[0] == "4.png"
            argument.individualCount[0] == 1

            [null, null]
        }



    }


    def "deleteProject should soft delete the project and all related records when destroy = false"() {
        setup:
        Output output = createHierarchy()

        when:
        outputService.delete(output.outputId, false)

        then:
        Output.count() == 1
        Document.count() == 2
        Record.count() == 2
        Document.findAll().each { assert it.status == DELETED }
        Output.findAll().each { assert it.status == DELETED }
        Record.findAll().each { assert it.status == DELETED }
    }

    def "deleteProject should hard delete the project and all related records when destroy = true"() {
        setup:
        Output output = createHierarchy()

        when:
        outputService.delete(output.outputId, true)

        then:
        Document.count() == 0
        Output.count() == 0
        Record.count() == 0
    }

    private static createHierarchy() {
        Output output = new Output(outputId: "out2", activityId: "bla").save(failOnError: true, flush: true)
        new Record(outputId: output.outputId).save(failOnError: true, flush: true)
        new Record(outputId: output.outputId).save(failOnError: true, flush: true)
        new Document(documentId: "doc5", outputId: output.outputId).save(failOnError: true, flush: true)
        new Document(documentId: "doc6", outputId: output.outputId).save(failOnError: true, flush: true)

        output
    }
}
