package au.org.ala.ecodata

import com.github.fakemongo.Fongo
import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */

@TestMixin(MongoDbTestMixin)
class OrganisationServiceSpec extends Specification {

    OrganisationService service = new OrganisationService()
    def stubbedCollectoryService = Stub(CollectoryService)
    def stubbedUserService = Stub(UserService)
    def mockedPermissionService = Mock(PermissionService)

    def setup() {
        Fongo fongo = new Fongo("ecodata-test")
        mongoDomain(fongo.mongo, [Organisation])

        service.commonService = new CommonService()
        service.commonService.grailsApplication = grailsApplication
        service.collectoryService = stubbedCollectoryService
        service.userService = stubbedUserService
        service.permissionService = mockedPermissionService
    }

    def "test create organisation"() {
        given:
        def orgData = [name:'test org', description: 'test org description', dynamicProperty: 'dynamicProperty']
        def institutionId = 'dr1'
        def userId = '1234'
        stubbedCollectoryService.createInstitution(_) >> institutionId
        stubbedUserService.getCurrentUserDetails() >> [userId:userId]


        def result
            when:
            Organisation.withNewTransaction {
                result = service.create(orgData)
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
                //savedOrg['dynamicProperty'] == orgData.dynamicProperty  The dbo property on the domain object appears to be missing during unit tests which prevents dynamic properties from being retreived.

    }

    def "test organisation validation"() {
        given:
        def orgData = [description: 'test org description', dynamicProperty: 'dynamicProperty']
        stubbedCollectoryService.createInstitution(_) >> ""

        when:
        def result = service.create(orgData)

        then:
            result.status == 'error'
            result.errors.hasErrors()

    }

    def "test organisation views"() {
        // The dbo property on the domain object appears to be missing during unit tests which prevents toMap from working.
        /*
        given:
        def orgId = 'organisation_id'
        def projects = [[projectId:'1'], [projectId:'2']]
        Organisation org = new Organisation(organisationId: orgId, name: 'test org', description: 'test org description')
        Organisation.withNewTransaction {
            org.save(flush: true, failOnError: true)
        }
        ProjectService projectService = Mock(ProjectService)
        projectService.search([organisationId: orgId]) >> projects


        when:
        def result = service.toMap(org)

        then:
        result.organisationId == orgId
        result.name == org.name
        result.description == org.description
        result.projects == null

        when:
        result = service.toMap(org, [OrganisationService.PROJECTS])

        then:
        result.organisationId == orgId
        result.name == org.name
        result.description == org.description
        result.dynamicProperty == org['dynamicProperty']
        result.projects == projects
        */
    }




}
