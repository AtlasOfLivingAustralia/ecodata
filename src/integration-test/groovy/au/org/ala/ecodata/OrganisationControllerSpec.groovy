package au.org.ala.ecodata

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import grails.util.GrailsWebMockUtil
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse

@Integration
@Rollback
class OrganisationControllerSpec extends IntegrationTestHelper {
    @Autowired
    OrganisationController organisationController

    @Autowired
    WebApplicationContext ctx

    def organisationService

    def stubbedCollectoryService = Stub(CollectoryService)
    def stubbedUserService = Stub(UserService)
    def collectoryService
    def userService
    def grailsApplication

    def setup() {
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)

        grailsApplication.config.collectory.collectoryIntegrationEnabled = true
        organisationService.grailsApplication = grailsApplication
        organisationService.collectoryService = stubbedCollectoryService
        organisationService.userService = stubbedUserService
        organisationController.organisationService = organisationService
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
        def org = TestDataHelper.buildNewOrganisation([name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty'])
        setupPost(organisationController.request, org)

        when: "creating an organisation"
        organisationController.update('') // Empty or null ID triggers a create

        then: "ensure we get a response including an organisationId"
        organisationController.response.contentType == 'application/json;charset=UTF-8'
        def resp = extractJson(organisationController.response.text)
        resp.message == 'created'
        def organisationId = resp.organisationId
        resp.organisationId != null


        when: "retrieving the new project"
        organisationController.response.reset()
        organisationController.get(organisationId)

        def savedOrganisation = extractJson(organisationController.response.text)

        then: "ensure the properties are the same as the original"
        savedOrganisation.organisationId == organisationId
        savedOrganisation.name == org.name
        savedOrganisation.description == org.description
        // savedOrganisation.dynamicProperty == org.dynamicProperty (dynamic properties not working in tests)
        savedOrganisation.collectoryInstitutionId == institutionId

        and: "the user who created the organisation is an admin of the new organisation"
        def orgPermissions = UserPermission.findAllByEntityIdAndEntityType(savedOrganisation.organisationId, Organisation.class.name)
        orgPermissions.size() == 1
        orgPermissions[0].userId == userId
        orgPermissions[0].accessLevel == AccessLevel.admin

    }

    void "projects should be associated with an organisation by the organisationId property"() {
        setup:

        // Create some data for the database.
        def organisation = TestDataHelper.buildOrganisation([name:"org 1"])
        def projects = []
        (1..2).each {
            projects << TestDataHelper.buildProject([organisationId:organisation.organisationId, name:'org project '+it])
        }
        (1..3).each {
            projects << TestDataHelper.buildProject([name:'project '+it]) // some projects without our organisation id.
        }
        Organisation.withTransaction {
            TestDataHelper.saveAll(projects + [organisation])
        }

        when: "retrieving the organisation"
        organisationController.request.addParameter('view', 'all') // The 'all' view will return associated projects.
        Organisation.withTransaction {
            organisationController.get(organisation.organisationId)
        }
        def org = extractJson(organisationController.response.text)

        then: "ensure all of the projects are returned"
        org.organisationId == organisation.organisationId
        org.name == organisation.name
        org.projects.size() == 2
        org.projects.each {
            it.organisationId == organisation.organisationId
        }

    }

    void "projects can be associated with an organisation by the serviceProviderOrganisationId property"() {
        setup:

        // Create some data for the database.
        def organisation = TestDataHelper.buildOrganisation([name: 'org 1'])
        def projects = []
        (1..2).each {
            projects << TestDataHelper.buildProject([orgIdSvcProvider: organisation.organisationId, name:'svc project '+it])
        }
        projects << TestDataHelper.buildProject([organisationId: organisation.organisationId, name:'org project'])
        (1..3).each {
            projects << TestDataHelper.buildProject([name:"project "+it]) // some projects without our organisation id.
        }
        TestDataHelper.saveAll(projects+[organisation])

        when: "retrieving the organisation"
        organisationController.request.addParameter('view', 'all') // The 'all' view will return associated projects.
        Organisation.withTransaction {
            organisationController.get(organisation.organisationId)
        }
        def org = extractJson(organisationController.response.text)

        then: "ensure all of the projects are returned"
        org.organisationId == organisation.organisationId
        org.name == organisation.name
        org.projects.size() == 3
        org.projects.each {
            it.organisationId == organisation.organisationId || it.orgIdSvcProvider == organisation.organisationId
        }
    }

    private extractJson (String str) {
        if(str.indexOf('{') > -1 && str.indexOf('}') > -1) {
            String jsonStr = str.substring(str.indexOf('{'), str.lastIndexOf('}') + 1)
            new JsonSlurper().parseText(jsonStr)
        }
    }

}
