package au.org.ala.ecodata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.springframework.context.MessageSource
import spock.lang.Specification

@TestMixin(MongoDbTestMixin)
@Domain(ManagementUnit)
@TestFor(ManagementUnitService)
class ManagementUnitServiceSpec extends Specification {

    CommonService commonService = new CommonService()

    def setup() {
        commonService.grailsApplication = grailsApplication
        commonService.messageSource = Mock(MessageSource)
        service.commonService = commonService

        ManagementUnit.findAll().each { it.delete(flush:true) }
    }

    def cleanup() {
        ManagementUnit.findAll().each { it.delete(flush:true) }
    }


    void "find an existing mu"() {
        setup:
        ManagementUnit p1 = new ManagementUnit(managementUnitId: 'p1', name: 'test 1', description: 'description 1',status:Status.ACTIVE)
        p1.save(flush:true, failOnError: true)

        when:

        ManagementUnit mu = service.get('p1',false)

        then:
        mu.name == 'test 1'

    }

}
