package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import org.bson.types.ObjectId
import spock.lang.Specification

class SubmissionServiceSpec extends Specification implements ServiceUnitTest<SubmissionService>, DomainUnitTest<SubmissionRecord> {
    CommonService commonService = Mock(CommonService)
    UserService userService = Mock(UserService)
    WebService webService = Mock(WebService)

    Class[] getDomainClassesToMock() {
        [SubmissionRecord]
    }

    def setup() {
        service.grailsApplication = grailsApplication
        service.grailsApplication.config.aekosPolling.url = 'test'
        service.commonService = commonService
        service.userService = userService
        service.webService = webService

        SubmissionPackage.findAll().each { it.delete(flush:true) }
        SubmissionRecord.findAll().each { it.delete(flush:true) }
    }

    def cleanup() {
        SubmissionPackage.findAll().each { it.delete(flush:true) }
        SubmissionRecord.findAll().each { it.delete(flush:true) }
    }

    void "Get by id"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1')
        submissionRecord.save(flush: true, failOnError: true)
        String id = SubmissionRecord.findBySubmissionId('1').id

        SubmissionPackage submissionPackage = new SubmissionPackage(submissionRecordId: '1')
        submissionPackage.save(flush: true, failOnError: true)

        when:
        def response = service.get(id)

        then:
        1 * commonService.toBareMap(submissionRecord) >> [submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1']
        1 * userService.getUserForUserId('1') >> [userId: '1', userName: 'test', displayName: 'test']
        1 * commonService.toBareMap(submissionPackage) >> [submissionRecordId: '1']
        response != null
        response.submissionId == '1'
    }

    void "Get by id - invalid id"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1')
        submissionRecord.save(flush: true, failOnError: true)

        SubmissionPackage submissionPackage = new SubmissionPackage(submissionRecordId: '1')
        submissionPackage.save(flush: true, failOnError: true)

        when:
        def response = service.get(new ObjectId())

        then:
        response == null
    }

    void "Get by id - no SubmissionPackage"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1')
        submissionRecord.save(flush: true, failOnError: true)
        String id = SubmissionRecord.findBySubmissionId('1').id

        when:
        def response = service.get(id)

        then:
        response == null
    }

    void "update by id"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1')
        submissionRecord.save(flush: true, failOnError: true)

        SubmissionPackage submissionPackage = new SubmissionPackage(submissionRecordId: '1')
        submissionPackage.save(flush: true, failOnError: true)

        Map props = [projectActivityId : '1', submissionPackage : [datasetContact: [:]]]

        when:
        def response = service.update('1', props)

        then:
        1 * commonService.updateProperties(submissionRecord, props)
        1 * commonService.updateProperties(submissionPackage, [:])
    }

    void "update by id - invalid id"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1')
        submissionRecord.save(flush: true, failOnError: true)
        SubmissionPackage submissionPackage = new SubmissionPackage(submissionRecordId: '1')
        submissionPackage.save(flush: true, failOnError: true)

        Map props = [projectActivityId : '1',  submissionPackage : [datasetContact: [:]]]

        when:
        def response = service.update('2', props)

        then:
        response == null
    }

    void "update by id - no SubmissionPackage"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1')
        submissionRecord.save(flush: true, failOnError: true)

        Map props = [projectActivityId : '1', submissionPackage : [datasetContact: [:]]]

        when:
        def response = service.update('1', props)

        then:
        response == null
    }

    void "check submission"() {
        setup:
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId:'1', submissionDoi: 'Pending')
        submissionRecord.save(flush: true, failOnError: true)

        SubmissionPackage submissionPackage = new SubmissionPackage(submissionRecordId: '1')
        submissionPackage.save(flush: true, failOnError: true)

        when:
        def response = service.checkSubmission()

        then:
        1 * webService.getJson(grailsApplication.config.aekosPolling?.url + '/1', 10000) >> [message: 'DOI minted.', submissionDoi: 'ok']
        response != null
    }

    void "check submission - error" () {
        SubmissionRecord submissionRecord = new SubmissionRecord(submissionId: '1', datasetSubmitter: "1", submissionRecordId: '1',  submissionDoi: 'Pending')
        submissionRecord.save(flush: true, failOnError: true)
        SubmissionPackage submissionPackage = new SubmissionPackage(submissionRecordId: '1')
        submissionPackage.save(flush: true, failOnError: true)

        when:
        def response = service.checkSubmission()

        then:
        1 * webService.getJson(grailsApplication.config.aekosPolling?.url + '/1', 10000) >> [statusCode: 500, detail: 'error']
    }
}
