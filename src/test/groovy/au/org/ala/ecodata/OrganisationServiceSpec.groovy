package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.validation.ValidationException
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class OrganisationServiceSpec extends Specification implements ServiceUnitTest<OrganisationService>, DomainUnitTest<Organisation> {

    //OrganisationService service = new OrganisationService()
    def stubbedCollectoryService = Stub(CollectoryService)
    def stubbedUserService = Stub(UserService)
    def mockedPermissionService = Mock(PermissionService)

    def setup() {
       // Fongo fongo = new Fongo("ecodata-test")
//        mongoDomain(fongo.mongo, [Organisation])

        service.commonService = new CommonService()
        service.commonService.grailsApplication = grailsApplication
        service.collectoryService = stubbedCollectoryService
        service.userService = stubbedUserService
        service.permissionService = mockedPermissionService
        grailsApplication.config.collectory = [collectoryIntegrationEnabled:true]
    //    service.grailsApplication = [config:[collectory:[collectoryIntegrationEnabled:true]]]
    }

    def cleanup() {
        Organisation.findAll().each { it.delete(flush:true) }
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

/*
    def "test organisation views"() {
        // The dbo property on the domain object appears to be missing during unit tests which prevents toMap from working.

        given:
       // def orgId = 'organisation_id'
        def projects = [[projectId:'1'], [projectId:'2']]
        def orgId = Identifiers.getNew(true, '')
        def org = new Organisation(organisationId: orgId, name: 'a test org', description: 'a test org description')
      //  Organisation.withNewTransaction {
          //  service.create(org)
       // }
     //   Organisation.withNewTransaction {
            org.save(flush: true, failOnError: true)
     //   }
        ProjectService projectService = Mock(ProjectService)
        service.projectService = projectService
        projectService.search([organisationId: orgId]) >> projects
        projectService.search([orgIdSvcProvider: orgId]) >> []


        when:
        def result
     //   print (orgId)
       // Organisation.withNewTransaction {
            result = service.get(orgId)
       //     print result
      //  }
     //   def result = service.toMap(org)

        then:
        result.organisationId == orgId
        result.name == org.name
        result.description == org.description
        result.projects == null

        when:
      //  Organisation.withNewTransaction {
      //     result = service.get(orgId, [OrganisationService.PROJECTS])
      //  print result
      //  }
        def result1 = service.toMap(org, [OrganisationService.PROJECTS])

        then:
        result1.organisationId == orgId
        result1.name == org.name
        result1.description == org.description
        result1.dynamicProperty == org['dynamicProperty']
        result1.projects == projects

    }
*/




}
