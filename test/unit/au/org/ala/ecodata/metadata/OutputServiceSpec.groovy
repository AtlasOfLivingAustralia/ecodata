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

    def "create output should create a record if the output data model has record = true"() {
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
        1 * mockRecordService.createRecord(_) >> [[:]]
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

    def "create output should create records for each list item if the output data model has record = true and data type = list"() {
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
                                                                                            dataType: "text"
                                                                                    ]
                                                                            ]]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Something",
            "data": {
                    "actions": [
                        {"col1": "action1"},
                        {"col1": "action2"}
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
