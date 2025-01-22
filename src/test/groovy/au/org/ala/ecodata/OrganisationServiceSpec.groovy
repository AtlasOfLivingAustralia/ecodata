package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.validation.ValidationException
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class OrganisationServiceSpec extends Specification implements ServiceUnitTest<OrganisationService>, DomainUnitTest<Organisation> {

    def collectoryService = Mock(CollectoryService)
    def stubbedUserService = Stub(UserService)
    def mockedPermissionService = Mock(PermissionService)
    def emailService = Mock(EmailService)

    def setup() {
        grailsApplication.config.collectory.collectoryIntegrationEnabled = true
        grailsApplication.config.ecodata.support.email.address = 'test@test.com'
        service.commonService = new CommonService()
        service.commonService.grailsApplication = grailsApplication
        service.collectoryService = collectoryService
        service.userService = stubbedUserService
        service.permissionService = mockedPermissionService
        service.emailService = emailService
        service.grailsApplication = grailsApplication

    }

    def cleanup() {
        Organisation.findAll().each { it.delete(flush:true) }
    }

    def "test create organisation"() {
        given:
        def orgData = [name:'test org', description: 'test org description', dynamicProperty: 'dynamicProperty']
        def institutionId = 'dr1'
        def userId = '1234'
        collectoryService.createInstitution(_) >> institutionId
        stubbedUserService.getCurrentUserDetails() >> [userId:userId]


        def result
            when:
            Organisation.withNewTransaction {
                result = service.create(orgData, true)
            }
            then: "ensure the response contains the id of the new organisation"
                result.status == 'ok'
                result.organisationId != null
                1 * mockedPermissionService.addUserAsRoleToOrganisation(userId, AccessLevel.admin, !null)


            when: "select the new organisation back from the database"
                def savedOrg = Organisation.findByOrganisationId(result.organisationId)


            then: "ensure the properties are the same as the original"
                savedOrg.name == orgData.name
                savedOrg.description == orgData.description
                savedOrg.collectoryInstitutionId == institutionId
    }

    def "Organisations should still be created if collectory institution creation fails"() {
        given:
        Map orgData = [name:'another test org', description: 'another test org description']
        String userId = '1234'
        stubbedUserService.getCurrentUserDetails() >> [userId:userId]

        when:
        Map result
        Organisation.withNewTransaction {
            result = service.create(orgData, true)
        }

        then:
        1 * collectoryService.createInstitution(orgData) >> { throw new RuntimeException("test message") }
        and: "An email is sent to inform system administrators of the failure"
        1 * emailService.sendEmail(_, "Error: test message", ['test@test.com'])
        and: "The organisation is created successfully"
        result.status == 'ok'
    }

    def "test organisation validation"() {
        given:
        def orgData = [description: 'test org description', dynamicProperty: 'dynamicProperty']
        collectoryService.createInstitution(_) >> ""

        when:
        def result = service.create(orgData)

        then:
            result.status == 'error'
            result.errors.hasErrors()

    }

}
