package au.org.ala.ecodata

import org.apache.http.HttpStatus

import static au.org.ala.ecodata.Status.*
import grails.test.spock.IntegrationSpec

class RecordControllerSpec extends IntegrationSpec {

    RecordController recordController = new RecordController()
    PermissionService permissionService

    def grailsApplication

    def setup() {
        permissionService = Mock(PermissionService)
        recordController.permissionService = permissionService

        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def "get should return a 401 (SC_UNAUTHORIZED) if a user is requesting an embargoed record for a project they are not a member of"() {
        setup:
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)
        permissionService.isUserMemberOfProject(_, _) >> false
        new Record(occurrenceID: "1234", userId: "id1", projectId: "project1", embargoUntil: future.getTime(), status: ACTIVE).save(flush: true, failOnError: true)

        when:
        recordController.params.userId = "user1"
        recordController.params.id = "1234"
        recordController.get()

        then:
        recordController.response.status == HttpStatus.SC_UNAUTHORIZED
    }

    def "get should return a 401 (SC_UNAUTHORIZED) if there is no userId when requesting an embargoed record"() {
        setup:
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)
        new Record(occurrenceID: "1234", userId: "id1", projectId: "project1", embargoUntil: future.getTime(), status: ACTIVE).save(flush: true, failOnError: true)

        when:
        recordController.params.id = "1234"
        recordController.get()

        then:
        recordController.response.status == HttpStatus.SC_UNAUTHORIZED
    }

    def "get should return the record if a user is requesting an embargoed record for a project they ARE a member of"() {
        setup:
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)
        permissionService.isUserMemberOfProject(_, _) >> true
        new Record(occurrenceID: "1234", userId: "id1", projectId: "project1", embargoUntil: future.getTime(), status: ACTIVE).save(flush: true, failOnError: true)

        when:
        recordController.params.userId = "user1"
        recordController.params.id = "1234"
        recordController.get()

        then:
        recordController.response.status == HttpStatus.SC_OK
        recordController.response.json.occurrenceID == "1234"
    }

    def "get should return the record if the user is an ALA Admin"() {
        setup:
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)
        permissionService.isUserMemberOfProject(_, _) >> false
        permissionService.isUserAlaAdmin(_) >> true
        new Record(occurrenceID: "1234", userId: "id1", projectId: "project1", embargoUntil: future.getTime(), status: ACTIVE).save(flush: true, failOnError: true)

        when:
        recordController.params.userId = "user1"
        recordController.params.id = "1234"
        recordController.get()

        then:
        recordController.response.status == HttpStatus.SC_OK
        recordController.response.json.occurrenceID == "1234"
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

    def "list should not include embargoed records unless the embargo date has passed when there is no userid"() {
        setup:
        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: null).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)

        when: "the list is requested without a specific user id"
        recordController.list()

        then: "only the records with a future or null embargo date should be returned"
        recordController.response.json.total == 3
        recordController.response.json.list[0] != null
    }

    def "list should include embargoed records when there is a userid and the record belongs to a project where the user is a member"() {
        setup:
        permissionService.getProjectsForUser(_) >> ["project1", "project2"]

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project2", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project3", status: ACTIVE, embargoUntil: null).save(flush: true, failOnError: true)
        new Record(projectId: "project4", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project5", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)

        when: "the list is requested by a specific user "
        recordController.params.userId = "user1"
        recordController.list()

        then: "all records belonging to project(s) where the user is a member should be returned, even if it has a future embargo date"
        recordController.response.json.total == 2
        recordController.response.json.list[0].size() > 0
    }

    def "list should include embargoed records when there is a userid and the user is an ALA Admin"() {
        setup:
        permissionService.getProjectsForUser(_) >> []
        permissionService.isUserAlaAdmin(_) >> true

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project2", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project3", status: ACTIVE, embargoUntil: null).save(flush: true, failOnError: true)
        new Record(projectId: "project4", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project5", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)

        when: "the list is requested by a specific user "
        recordController.params.userId = "user1"
        recordController.list()

        then: "all records should be returned, even if it has a future embargo date"
        recordController.response.json.total == 5
        recordController.response.json.list[0].size() > 0
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

    def "listUncertainIdentifications should not include embargoed records unless the embargo date has passed when there is no userid"() {
        setup:
        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        Record record1 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record1["identificationVerificationStatus"] = "Uncertain"
        record1.save(flush: true, failOnError: true)
        Record record2 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record2["identificationVerificationStatus"] = "Uncertain"
        record2.save(flush: true, failOnError: true)
        Record record3 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: null)
        record3["identificationVerificationStatus"] = "Uncertain"
        record3.save(flush: true, failOnError: true)
        Record record4 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime())
        record4["identificationVerificationStatus"] = "Uncertain"
        record4.save(flush: true, failOnError: true)

        when: "the list is requested without a specific user id"
        recordController.listUncertainIdentifications()

        then: "only the records with a future or null embargo date should be returned"
        recordController.response.json.size() == 3
    }

    def "listUncertainIdentifications should include embargoed records when there is a userid and the record belongs to a project where the user is a member"() {
        setup:
        permissionService.getProjectsForUser(_) >> ["project1", "project2"]

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        Record record1 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record1["identificationVerificationStatus"] = "Uncertain"
        record1.save(flush: true, failOnError: true)
        Record record2 = new Record(projectId: "project2", status: ACTIVE, embargoUntil: past.getTime())
        record2["identificationVerificationStatus"] = "Uncertain"
        record2.save(flush: true, failOnError: true)
        Record record3 = new Record(projectId: "project3", status: ACTIVE, embargoUntil: null)
        record3["identificationVerificationStatus"] = "Uncertain"
        record3.save(flush: true, failOnError: true)
        Record record4 = new Record(projectId: "project4", status: ACTIVE, embargoUntil: future.getTime())
        record4["identificationVerificationStatus"] = "Uncertain"
        record4.save(flush: true, failOnError: true)

        when: "the list is requested by a specific user "
        recordController.params.userId = "user1"
        recordController.listUncertainIdentifications()

        then: "all records belonging to project(s) where the user is a member should be returned, even if it has a future embargo date"
        recordController.response.json.size() == 2
    }

    def "listUncertainIdentifications should include embargoed records when there is a userid and the user is an ALA Admin"() {
        setup:
        permissionService.getProjectsForUser(_) >> []
        permissionService.isUserAlaAdmin(_) >> true

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        Record record1 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record1["identificationVerificationStatus"] = "Uncertain"
        record1.save(flush: true, failOnError: true)
        Record record2 = new Record(projectId: "project2", status: ACTIVE, embargoUntil: past.getTime())
        record2["identificationVerificationStatus"] = "Uncertain"
        record2.save(flush: true, failOnError: true)
        Record record3 = new Record(projectId: "project3", status: ACTIVE, embargoUntil: null)
        record3["identificationVerificationStatus"] = "Uncertain"
        record3.save(flush: true, failOnError: true)
        Record record4 = new Record(projectId: "project4", status: ACTIVE, embargoUntil: future.getTime())
        record4["identificationVerificationStatus"] = "Uncertain"
        record4.save(flush: true, failOnError: true)

        when: "the list is requested by a specific user "
        recordController.params.userId = "user1"
        recordController.listUncertainIdentifications()

        then: "all records should be returned, even if it has a future embargo date"
        recordController.response.json.size() == 4
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

    def "listRecordWithImages should not include embargoed records unless the embargo date has passed when there is no userid"() {
        setup:
        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        Record record1 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record1["multimedia"] = [stuff: "here"]
        record1.save(flush: true, failOnError: true)
        Record record2 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record2["multimedia"] = [stuff: "here"]
        record2.save(flush: true, failOnError: true)
        Record record3 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: null)
        record3["multimedia"] = [stuff: "here"]
        record3.save(flush: true, failOnError: true)
        Record record4 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime())
        record4["multimedia"] = [stuff: "here"]
        record4.save(flush: true, failOnError: true)

        when: "the list is requested without a specific user id"
        recordController.listRecordWithImages()

        then: "only the records with a future or null embargo date should be returned"
        recordController.response.json.size() == 3
    }

    def "listRecordWithImages should include embargoed records when there is a userid and the record belongs to a project where the user is a member"() {
        setup:
        permissionService.getProjectsForUser(_) >> ["project1", "project2"]

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        Record record1 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record1["multimedia"] = [stuff: "here"]
        record1.save(flush: true, failOnError: true)
        Record record2 = new Record(projectId: "project2", status: ACTIVE, embargoUntil: past.getTime())
        record2["multimedia"] = [stuff: "here"]
        record2.save(flush: true, failOnError: true)
        Record record3 = new Record(projectId: "project3", status: ACTIVE, embargoUntil: null)
        record3["multimedia"] = [stuff: "here"]
        record3.save(flush: true, failOnError: true)
        Record record4 = new Record(projectId: "project4", status: ACTIVE, embargoUntil: future.getTime())
        record4["multimedia"] = [stuff: "here"]
        record4.save(flush: true, failOnError: true)

        when: "the list is requested by a specific user "
        recordController.params.userId = "user1"
        recordController.listRecordWithImages()

        then: "all records belonging to project(s) where the user is a member should be returned, even if it has a future embargo date"
        recordController.response.json.size() == 2
    }

    def "listRecordWithImages should include embargoed records when there is a userid and the user is an ALA Admin"() {
        setup:
        permissionService.getProjectsForUser(_) >> []
        permissionService.isUserAlaAdmin(_) >> true

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        Record record1 = new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime())
        record1["multimedia"] = [stuff: "here"]
        record1.save(flush: true, failOnError: true)
        Record record2 = new Record(projectId: "project2", status: ACTIVE, embargoUntil: past.getTime())
        record2["multimedia"] = [stuff: "here"]
        record2.save(flush: true, failOnError: true)
        Record record3 = new Record(projectId: "project3", status: ACTIVE, embargoUntil: null)
        record3["multimedia"] = [stuff: "here"]
        record3.save(flush: true, failOnError: true)
        Record record4 = new Record(projectId: "project4", status: ACTIVE, embargoUntil: future.getTime())
        record4["multimedia"] = [stuff: "here"]
        record4.save(flush: true, failOnError: true)

        when: "the list is requested by a specific user "
        recordController.params.userId = "user1"
        recordController.listRecordWithImages()

        then: "all records belonging should be returned, even if it has a future embargo date"
        recordController.response.json.size() == 4
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

    def "listForProject should not include embargoed records unless the embargo date has passed when there is no userid"() {
        setup:
        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: null).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)

        when: "the list is requested without a specific user id"
        recordController.params.id = "project1"
        recordController.listForProject()

        then: "only the records with a future or null embargo date should be returned"
        recordController.response.json.total == 3
    }

    def "listForProject should include embargoed records when there is a userid and the user is a member of the parent project"() {
        setup:
        permissionService.isUserMemberOfProject(_, _) >> true

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: null).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)

        when: "the list is requested by a user who is a member of the project"
        recordController.params.id = "project1"
        recordController.params.userId = "user1"
        recordController.listForProject()

        then: "all records, including the one with the future embargo date, should be returned"
        recordController.response.json.total == 4
        recordController.response.json.list[0].size() > 0
    }

    def "listForProject should include embargoed records when there is a userid and the user is an ALA Admin"() {
        setup:
        permissionService.isUserMemberOfProject(_, _) >> false
        permissionService.isUserAlaAdmin(_) >> true

        Calendar past = Calendar.getInstance()
        past.add(Calendar.MONTH, -1)
        Calendar future = Calendar.getInstance()
        future.add(Calendar.MONTH, 1)

        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: past.getTime()).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: null).save(flush: true, failOnError: true)
        new Record(projectId: "project1", status: ACTIVE, embargoUntil: future.getTime()).save(flush: true, failOnError: true)

        when: "the list is requested by a user who is an ALA Admin"
        recordController.params.id = "project1"
        recordController.params.userId = "user1"
        recordController.listForProject()

        then: "all records, including the one with the future embargo date, should be returned"
        recordController.response.json.total == 4
        recordController.response.json.list[0].size() > 0
    }
}
