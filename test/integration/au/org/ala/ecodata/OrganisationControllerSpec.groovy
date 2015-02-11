package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.spock.IntegrationSpec

class OrganisationControllerSpec extends IntegrationSpec {

    def organisationController = new OrganisationController()

    def setup() {

    }

    def cleanup() {
    }

    void "test create organisation"() {

        setup:
        def org = [name: 'Test Organisation', description: 'Test description', dynamicProperty: 'dynamicProperty']
        organisationController.request.contentType = 'application/json;charset=UTF-8'
        organisationController.request.content = (org as JSON).toString().getBytes('UTF-8')
        organisationController.request.method = 'POST'

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

    }

}
