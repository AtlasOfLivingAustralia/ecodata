package au.org.ala.ecodata

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration

@Integration
@Rollback
class OrganisationServiceIntegrationSpec extends IntegrationTestHelper {

    def organisationService

    def stubbedCollectoryService = Stub(CollectoryService)
    def stubbedUserService = Stub(UserService)
    def collectoryService
    def userService
    def grailsApplication

    def setup() {
        grailsApplication.config.collectory.collectoryIntegrationEnabled = true
        organisationService.grailsApplication = grailsApplication
        organisationService.collectoryService = stubbedCollectoryService
        organisationService.userService = stubbedUserService
    }

    def cleanup() {
        // The environment persists for all integration tests so we need to restore the service to it's previous condition.
        organisationService.userService = userService
        organisationService.collectoryService = collectoryService
    }

    void "test create organisation"() {

        setup:
        def institutionId = "dr1"
        def userId = '1234'
        stubbedCollectoryService.createInstitution(_) >> institutionId
        stubbedUserService.getCurrentUserDetails() >> [userId: userId]
        def org = [name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty'] //TestDataHelper.buildNewOrganisation([name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty'])

        //def org = [name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty'] //TestDataHelper.buildNewOrganisation([name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty'])
      //  setupPost(organisationController.request, org)

        when: "creating an organisation"
        def result = organisationService.create(org, true)

        then: "ensure we get a response including an organisationId"
       // def resp = extractJson(organisationController.response.text)
        result.status == 'ok'
        def organisationId = result.organisationId
        result.organisationId != null


        when: "retrieving the new project"
        def savedOrganisation
        Organisation.withTransaction {
            savedOrganisation = organisationService.get(organisationId)
        }

        then: "ensure the properties are the same as the original"
        savedOrganisation.organisationId == organisationId
        savedOrganisation.name == org.name
        savedOrganisation.description == org.description
        savedOrganisation.collectoryInstitutionId == institutionId

        and: "The OrganisationService no longer supports dynamic properties"
        savedOrganisation.dynamicProperty == null


        and: "the user who created the organisation is an admin of the new organisation"
        def orgPermissions = UserPermission.findAllByEntityIdAndEntityType(savedOrganisation.organisationId, Organisation.class.name)
        orgPermissions.size() == 1
        orgPermissions[0].userId == userId
        orgPermissions[0].accessLevel == AccessLevel.admin

    }

    void "projects should be associated with an organisation by the organisationId property"() {
       setup:

       // Create some data for the database.
       def organisation
       Organisation.withTransaction {
           organisation = TestDataHelper.buildOrganisation([name: 'Test Organisation1'])
           def projects = []
           (1..2).each {
               projects << TestDataHelper.buildProject([organisationId:organisation.organisationId])
           }
           (1..3).each {
               projects << TestDataHelper.buildProject() // some projects without our organisation id.
           }

           TestDataHelper.saveAll(projects + [organisation])
       }

       when: "retrieving the organisation"
      // organisationController.request.addParameter('view', 'all') // The 'all' view will return associated projects.
       def org
       Organisation.withTransaction {
           def levelOfDetail = ['documents', 'projects']
           org = organisationService.get(organisation.organisationId, levelOfDetail)
       }
       //def org = extractJson(organisationController.response.text)

       then: "ensure all of the projects are returned"
       org.organisationId == organisation.organisationId
       org.name == organisation.name
       org.projects.size() == 2
       org.projects.each {
           it.organisationId == organisation.organisationId
       }

    }

       void "projects can be associated with an organisation by the associatedOrgs property"() {
           setup:

           def organisation
           Organisation.withTransaction {

               // Create some data for the database.
               organisation = TestDataHelper.buildOrganisation([name: 'Test Organisation2'])
               def projects = []
               (1..2).each {
                   projects << TestDataHelper.buildProject([associatedOrgs: [[organisationId:organisation.organisationId, name:'org project '+it]]])
               }
               projects << TestDataHelper.buildProject([organisationId: organisation.organisationId])
               (1..3).each {
                   projects << TestDataHelper.buildProject() // some projects without our organisation id.
               }
               TestDataHelper.saveAll(projects + [organisation])
           }
           when: "retrieving the organisation"
           def org
           Organisation.withTransaction {
               def levelOfDetail = ['documents', 'projects']
               org = organisationService.get(organisation.organisationId, levelOfDetail)
           }
        /*   organisationController.request.addParameter('view', 'all') // The 'all' view will return associated projects.
           Organisation.withTransaction {
               organisationController.get(organisation.organisationId)
           }
           def org = extractJson(organisationController.response.text)*/

           then: "ensure all of the projects are returned"
           org.organisationId == organisation.organisationId
           org.name == organisation.name
           org.projects.size() == 3
           org.projects.each {
               it.organisationId == organisation.organisationId || it.orgIdSvcProvider == organisation.organisationId
           }
       }

}
