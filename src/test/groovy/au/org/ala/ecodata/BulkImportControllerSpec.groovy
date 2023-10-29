package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class BulkImportControllerSpec  extends Specification implements ControllerUnitTest<BulkImportController>, DomainUnitTest<BulkImport> {
    def bulkImportService
    def setup() {
        bulkImportService = controller.bulkImportService = Mock(BulkImportService)
    }

    def "list: test sorting by lastUpdated field in descending order"() {
        when:
        params.sort = "lastUpdated"
        params.order = "asc"
        params.offset = "5"
        params.max = "3"
        params.query = 'test'
        params.userId = '123'
        controller.list()
        def json = JSON.parse(response.text)

        then:
        1 * bulkImportService.list([userId: '123'], [sort: "lastUpdated", order: "asc", max: 3, offset: 5], "test")  >> [total: 5, items: []]
        response.status == 200
        json.total == 5
        json.items == []
    }

    def "list: test error handling"() {
        when:
        controller.list()
        def json = JSON.parse(response.text)

        then:
        1 * bulkImportService.list([:], [sort: "lastUpdated", order: "desc", max: 10, offset: 0], "") >> { throw new RuntimeException("Something went wrong") }
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        json.status == "error"
        json.error.contains("Something went wrong")
    }

    def "create: test missing userId"() {
        when:
        request.method = 'POST'
        request.json = [:]
        controller.create()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == '{"status":"error","error":"Missing userId"}'
    }

    def "create: test successful bulk import creation"() {
        when:
        request.method = 'POST'
        request.json = [
                dataToLoad: [],
                projectActivityId: 'abc',
                projectId: '123',
                formName: "test",
                description: "test 1",
                userId : 'user1'
        ]
        controller.create()

        then:
        1 * bulkImportService.create(_) >> new BulkImport (
                bulkImportId: "1",
                dataToLoad: [],
                projectActivityId: 'abc',
                projectId: '123',
                formName: "test",
                description: "test 1",
                userId : 'user1'
        )

        response.status == HttpStatus.SC_CREATED
        response.text == '{"status":"ok","bulkImportId":"1"}'
    }

    def "create: test bulk import creation failure"() {
        def result = new BulkImport(bulkImportId: 1)
        result.save(flush: true)
        when:
        request.method = 'POST'
        request.json = [
                bulkImportId: 1,
                dataToLoad: [],
                projectActivityId: 'abc',
                projectId: '123',
                formName: "test",
                description: "test 1",
                userId : 'user1'
        ]
        controller.create()

        then:
        1 * bulkImportService.create(_) >> result
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == '{"status":"error","error":"Failed to save bulk import data"}'
    }

    def "update: test missing id"() {
        when:
        request.method = 'PUT'
        request.json = [:]
        controller.update()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == '{"status":"error","error":"Missing id"}'
    }

    def "update: test mismatching bulk import identifier"() {
        when:
        request.method = 'PUT'
        request.json = [bulkImportId: "1"]
        params.id = "2"
        controller.update()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == '{"status":"error","error":"Bulk import identifier provided in JSON body and URL do not match"}'
    }

    def "update: test successful bulk import update"() {
        when:
        request.method = 'PUT'
        request.json = [
                bulkImportId: "1",
                dataToLoad: [],
                projectActivityId: 'abc',
                projectId: '123',
                formName: "test",
                description: "test 1",
                userId : 'user1'
        ]
        params.id = "1"
        controller.update()

        then:
        1 * bulkImportService.update(_) >> [status: 'ok', bulkImportId: "1"]
        response.status == HttpStatus.SC_OK
        response.text == '{"status":"ok","bulkImportId":"1"}'
    }

    def "update: test bulk import update failure"() {
        given:
        def json = [
                bulkImportId: "1",
                dataToLoad: [],
                projectActivityId: 'abc',
                projectId: '123',
                formName: "test",
                description: "test 1",
                userId : 'user1'
        ]

        when:
        params.id = "1"
        request.method = 'PUT'
        request.json = json
        controller.update()

        then:
        1 * bulkImportService.update(_) >> [status: 'error', error: 'Failed to update']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == '{"status":"error","error":"Failed to update"}'
    }

    def "get: test get valid bulk import id"() {
        given:
        def bulkImport = new BulkImport(
                bulkImportId: "1",
                dataToLoad: [],
                projectActivityId: 'abc',
                projectId: '123',
                formName: "test",
                description: "test 1",
                userId : 'user1'
        )
        bulkImport.save(flush: true, failOnError: true)

        when:
        request.addHeader('Accept', "application/json")
        params.id = bulkImport.bulkImportId
        controller.get()

        then:
        response.status == HttpStatus.SC_OK
        response.contentType == "application/json;charset=UTF-8"
        response.getJson().bulkImportId == bulkImport.bulkImportId
        response.getJson().userId == bulkImport.userId
    }


    def "get: test get missing bulk import id"() {
        when:
        controller.get()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.contentType == "application/json;charset=utf-8"
        response.getJson().status == "error"
        response.getJson().error == "Missing id"
    }

    def "get: test get missing bulk import id"() {

        when:
        params.id = "123"
        controller.get()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.contentType == "application/json;charset=utf-8"
        response.getJson().status == "error"
        response.getJson().error == "Bulk import not found"
    }
}
