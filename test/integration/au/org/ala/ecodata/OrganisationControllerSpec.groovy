package au.org.ala.ecodata

class OrganisationControllerSpec extends IntegrationTestHelper {

    def organisationController = new OrganisationController()
    def organisationService
    def stubbedCollectoryService = Stub(CollectoryService)

    def setup() {
        organisationService.collectoryService = stubbedCollectoryService
        organisationController.organisationService = organisationService
    }

    void "test create organisation"() {

        setup:
        def institutionId = "dr1"
        stubbedCollectoryService.createInstitution(_) >> institutionId
        def org = TestDataHelper.buildNewOrganisation([name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty'])
        setupPost(organisationController.request, org)

        when: "creating an organisation"
        def resp = organisationController.update('') // Empty or null ID triggers a create

        then: "ensure we get a response including an organisationId"

        def organisationId = resp.organisationId
        organisationController.response.contentType == 'application/json;charset=UTF-8'
        resp.message == 'created'
        organisationId != null


        when: "retrieving the new project"
        organisationController.response.reset()
        def savedOrganisation = organisationController.get(organisationId) // To support JSONP the controller returns a model object, which is transformed to JSON via a filter.

        then: "ensure the properties are the same as the original"
        savedOrganisation.organisationId == organisationId
        savedOrganisation.name == org.name
        savedOrganisation.description == org.description
        savedOrganisation.dynamicProperty == org.dynamicProperty
        savedOrganisation.collectoryInstitutionId == institutionId

    }

    void "projects should be associated with an organisation by the organisationId property"() {
        setup:

        // Create some data for the database.
        def organisation = TestDataHelper.buildOrganisation()
        def projects = []
        (1..2).each {
            projects << TestDataHelper.buildProject([organisationId:organisation.organisationId])
        }
        (1..3).each {
            projects << TestDataHelper.buildProject() // some projects without our organisation id.
        }
        TestDataHelper.saveAll(projects+[organisation])

        def projectList = Project.findAll()

        when: "retrieving the organisation"
        organisationController.request.addParameter('view', 'all') // The 'all' view will return associated projects.
        def org = organisationController.get(organisation.organisationId)

        then: "ensure all of the projects are returned"
        org.organisationId == organisation.organisationId
        org.name == organisation.name
        org.projects.size() == 2
        org.projects.each {
            it.organisationId == organisation.organisationId
        }


    }

}
