package au.org.ala.ecodata.metadata

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.CommonService
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Output
import au.org.ala.ecodata.OutputService
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.ProjectActivity
import au.org.ala.ecodata.Record
import au.org.ala.ecodata.RecordService
import au.org.ala.ecodata.Site
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.sf.json.groovy.JsonSlurper
import spock.lang.Specification

@TestFor(OutputService)
@Mock([Project, Site, Activity, Output, Record, ProjectActivity])
class OutputServiceSpec extends Specification {

    OutputService service
    MetadataService mockMetadataService
    RecordService mockRecordService

    def setup() {
        service = new OutputService()
        mockMetadataService = Mock(MetadataService)
        mockRecordService = Mock(RecordService)
        service.metadataService = mockMetadataService
        service.recordService = mockRecordService
        service.grailsApplication = [mainContext: [commonService: Mock(CommonService)]]
    }

    def "create output should not create a record even if the output data model has record = true as there is no species info"() {
        setup:
        String activityId = 'activity1'
        Activity activity = new Activity(activityId: activityId, type: 'Test', description: 'A test activity')
        activity.save(flush: true, failOnError: true)
        mockMetadataService.getOutputDataModelByName(_) >> [modelName: "Single Sighting",
                                                            record   : "true",
                                                            dataModel: [[dataType: "singleSighting"]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "10598",
                    "individualCount" : "3",
                    "decimalLatitude" : "-31.203404950917385",
                    "decimalLongitude": "146.95312499999997"
            }
        }"""

        Map response = service.create(new JsonSlurper().parseText(request) as Map)

        then:
        response.status != "error"
        0 * mockRecordService.createRecord(_) >> [[:]]
        Output.count() == 1
    }

    def "create output should create a record if the output data model has record = false"() {
        setup:
        String activityId = 'activity1'
        Activity activity = new Activity(activityId: activityId, type: 'Test', description: 'A test activity')
        activity.save(flush: true, failOnError: true)
        mockMetadataService.getOutputDataModelByName(_) >> [modelName: "Single Sighting",
                                                            record   : "false",
                                                            dataModel: [[dataType: "singleSighting"]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "10598",
                    "individualCount" : "3",
                    "decimalLatitude" : "-31.203404950917385",
                    "decimalLongitude": "146.95312499999997"
            }
        }"""

        Map response = service.create(new JsonSlurper().parseText(request) as Map)

        then:
        response.status != "error"
        0 * mockRecordService.createRecord(_) >> [[:]]
        Output.count() == 1
    }

    def "create output should NOT create a record if the output data model does not have the 'record' attribute"() {
        setup:
        String activityId = 'activity1'
        Activity activity = new Activity(activityId: activityId, type: 'Test', description: 'A test activity')
        activity.save(flush: true, failOnError: true)
        mockMetadataService.getOutputDataModelByName(_) >> [modelName: "Single Sighting",
                                                            dataModel: [[dataType: "singleSighting"]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "10598",
                    "individualCount" : "3",
                    "decimalLatitude" : "-31.203404950917385",
                    "decimalLongitude": "146.95312499999997"
            }
        }"""

        Map response = service.create(new JsonSlurper().parseText(request) as Map)

        then:
        response.status != "error"
        0 * mockRecordService.createRecord(_) >> [[:]]
        Output.count() == 1
    }

    def "create output should create records for each list item if the output data model has record = true, data type = list and species information is present"() {
        setup:
        String activityId = 'activity1'
        Activity activity = new Activity(activityId: activityId, type: 'Test', description: 'A test activity')
        activity.save(flush: true, failOnError: true)
        mockMetadataService.getOutputDataModelByName(_) >> [modelName: "Model 1",
                                                            record   : "true",
                                                            dataModel: [
                                                                    [
                                                                            name    : "actions",
                                                                            dataType: "list",
                                                                            columns : [
                                                                                    [
                                                                                            name: "col1",
                                                                                            dataType: "species"
                                                                                    ]
                                                                            ]]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Something",
            "data": {
                    "actions": [
                        {"col1" : {"guid": "urn:lsid:someid", "outputSpeciesId": "anhotherid", "name" : "SpiciesName"}},
                        {"col1" : {"guid": "urn:lsid:someid2", "outputSpeciesId": "anhotherid2", "name" : "SpiciesName2"}}
                    ]
            }
        }"""

        Map response = service.create(new JsonSlurper().parseText(request) as Map)

        then:
        response.status != "error"
        2 * mockRecordService.createRecord(_) >> [[:]]
        Output.count() == 1
    }
}
