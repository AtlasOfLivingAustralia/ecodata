package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.*
import grails.test.spock.IntegrationSpec

class RecordControllerSpec extends IntegrationSpec {

    RecordController recordController = new RecordController()
    def grailsApplication

    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def "count should not include DELETED records"() {
        setup:
        new Record(userId: "id1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id1", status: DELETED).save(flush: true, failOnError: true)
        new Record(userId: "id1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id2", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id3", status: DELETED).save(flush: true, failOnError: true)

        when:
        recordController.count()

        then:
        recordController.response.json.count == 3
    }

    def "list should not include DELETED records"() {
        setup:
        new Record(userId: "id1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id1", status: DELETED).save(flush: true, failOnError: true)
        new Record(userId: "id1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id2", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id3", status: DELETED).save(flush: true, failOnError: true)

        when:
        recordController.list()

        then:
        recordController.response.json.total == 3
    }

    def "listUncertainIdentifications should not include DELETED records"() {
        setup:
        Record record1 = new Record(status: ACTIVE)
        record1["identificationVerificationStatus"] = "Uncertain"
        record1.save(failOnError: true, flush: true)
        Record record2 = new Record(status: DELETED)
        record2["identificationVerificationStatus"] = "Uncertain"
        record2.save(failOnError: true, flush: true)
        Record record3 = new Record(status: ACTIVE)
        record3["identificationVerificationStatus"] = "Uncertain"
        record3.save(failOnError: true, flush: true)

        when:
        recordController.listUncertainIdentifications()

        then:
        recordController.response.json.size() == 2
    }

    def "listRecordWithImages should not include DELETED records"() {
        setup:
        Record record1 = new Record(status: ACTIVE)
        record1["multimedia"] = [stuff: "here"]
        record1.save(failOnError: true, flush: true)
        Record record2 = new Record(status: DELETED)
        record2["multimedia"] = [stuff: "here"]
        record2.save(failOnError: true, flush: true)
        Record record3 = new Record(status: ACTIVE)
        record3["multimedia"] = [stuff: "here"]
        record3.save(failOnError: true, flush: true)

        when:
        recordController.listRecordWithImages()

        then:
        recordController.response.json.size() == 2
    }

    def "listForUser should not include DELETED records"() {
        setup:
        new Record(userId: "id1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id1", status: DELETED).save(flush: true, failOnError: true)
        new Record(userId: "id1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(userId: "id2", status: ACTIVE).save(flush: true, failOnError: true)

        when:
        recordController.params.id = "id1"
        recordController.listForUser()

        then:
        recordController.response.json.total == 2
    }

    def "listForProject should not include DELETED records"() {
        setup:
        new Record(projectId: "project1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: DELETED).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE).save(flush: true, failOnError: true)
        new Record(projectId: "project2", status: ACTIVE).save(flush: true, failOnError: true)

        when:
        recordController.params.id = "project1"
        recordController.listForProject()

        then:
        recordController.response.json.total == 2
    }
}
