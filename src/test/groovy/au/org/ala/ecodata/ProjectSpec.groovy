package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DomainUnitTest

class ProjectSpec extends MongoSpec implements DomainUnitTest<Project> {

    def setup() {
        Project.findByProjectId("p1")?.delete(flush:true)
    }

    def cleanup() {
        Project.findByProjectId("p1")?.delete(flush:true)
    }

    def "Once set, the hubId cannot be overwritten"() {
        when:
        Project p = new Project(projectId:"p1", name:"Project 1", hubId:"merit")
        p.save(flush:true, failOnError:true)

        p.hubId = "newHub"
        p.save()

        then:
        p.hasErrors()
        p.errors.getFieldError("hubId")
    }
}
