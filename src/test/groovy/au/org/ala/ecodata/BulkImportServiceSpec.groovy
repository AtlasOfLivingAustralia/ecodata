package au.org.ala.ecodata

import au.org.ala.web.AuthService
import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest

class BulkImportServiceSpec extends MongoSpec implements ServiceUnitTest<BulkImportService>, DomainUnitTest<BulkImport> {
    def authService
    def setup() {
        defineBeans {
            commonService(CommonService)
        }

        authService = service.authService = Mock(AuthService)

        def bulkImport = new BulkImport(
                bulkImportId: "0",
                dataToLoad: [],
                formName: "test form",
                description: "test description",
                projectId: "1",
                userId: "2",
                projectActivityId: "3")
        bulkImport.save(failOnError: true, flush: true)
        new Project(name: "Test project", projectId: "1").save(failOnError: true, flush: true)

    }

    def cleanup() {
        BulkImport.findAll().each { it.delete(flush: true) }
    }

    def "create method should save a new BulkImport object to the database"() {
        given:
        def content = [
                bulkImportId     : "1",
                dataToLoad       : [],
                formName         : "test form 1",
                description      : "test description 1",
                projectId        : "2",
                userId           : "3",
                projectActivityId: "4"]

        when:
        def result = service.create(content)

        then:
        result != null
        result.projectId == "2"
        result.userId == "3"
        result.projectActivityId == "4"
        result.hasErrors() == false
        BulkImport.findByBulkImportId(result.bulkImportId) != null
    }

    def "list method should return imports"() {

        when:
        def result = service.list([projectId: "1"], [max:10, offset:0], null)

        then:
        1 * authService.getUserForUserId(_) >> new au.org.ala.web.UserDetails(id: 1, firstName: 'test', lastName: 'user', userName: "x@y.com", userId: "2", locked: false, roles: [])
        result.total == 1
        result.items.size() == 1
        result.items[0].projectId == "1"
        result.items[0].userId == "2"
        result.items[0].userName == "test user"
        result.items[0].projectName == "Test project"
    }

    def "update method should update an existing BulkImport object in the database"() {
        given:
        def props = [
                bulkImportId     : "0",
                dataToLoad       : [],
                formName         : "test form 1",
                description      : "test description 2",
                projectId        : "2",
                userId           : "3",
                projectActivityId: "4"
        ]

        when:
        def result = service.update(props)

        then:
        result.status == 'ok'
        BulkImport.findByBulkImportId("0").projectId == "2"
        BulkImport.findByBulkImportId("0").description == "test description 2"
    }

    def "update method should return an error if an exception occurs"() {
        given:
        def props = [bulkImportId: "1", projectId: 2L, userId: 2L, projectActivityId: 2L]

        when:
        def result = service.update(props)

        then:
        result.status == 'error'
        result.error.contains("Error updating bulk import 1")
    }
}
