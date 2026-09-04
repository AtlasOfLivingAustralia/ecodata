package au.org.ala.ecodata


import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class OrganisationSpec extends MongoSpec implements DomainUnitTest<Project> {

    def setup() {
        Organisation.findByOrganisationId("o1")?.delete(flush:true)
    }

    def cleanup() {
        Organisation.findByOrganisationId("o1")?.delete(flush:true)
    }

    def "Once set, the hubId cannot be overwritten"() {
        when:
        Organisation o = new Organisation(organisationId:"o1", name:"Org 1", hubId:"merit")
        o.save(flush:true, failOnError:true)

        o.hubId = "newHub"
        o.save()

        then:
        o.hasErrors()
        o.errors.getFieldError("hubId")
    }
}
