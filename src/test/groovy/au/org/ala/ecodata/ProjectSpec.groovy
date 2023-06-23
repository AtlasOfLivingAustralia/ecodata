package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DomainUnitTest

class ProjectSpec extends MongoSpec implements DomainUnitTest<Project> {

    def setup() {
        Project.findByProjectId("p1")?.delete(flush:true)
        Service.findAll()?.each{it.delete(flush:true)}
    }

    def cleanup() {
        Project.findByProjectId("p1")?.delete(flush:true)
        Service.findAll()?.each{it.delete(flush:true)}
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

    def "The Project can return associated services"() {
        setup:
        for (int i=1; i<=3; i++) {
            Service s = new Service(serviceId:i, legacyId: i, name:"Service $i")
            s.save()
        }
        Project p = new Project()
        p.custom = [details:[serviceIds:[1, 2, 3]]]

        when:
        List services = p.findProjectServices()

        then:
        services.size() == 3
        services*.name == ['Service 1', 'Service 2', 'Service 3']
        services*.legacyId == [1,2,3]
    }

    def "The Project can return monitoring categories specified in the MERI plan"() {
        setup:
        Project p = new Project()
        p.custom = [details:[:]]

        when:
        p.custom.details.baseline = [rows:[[protocols:["c1", "c2"]], [protocols:["c2", "c3"]]]]
        p.custom.details.monitoring = [rows:[[protocols:["c2"]], [protocols:["c4", "c3"]]]]

        then:
        p.getMonitoringProtocolCategories() == ["c1", "c2", "c3", "c4"]

        when:
        p.custom.details.baseline = null

        then:
        p.getMonitoringProtocolCategories() == ["c2", "c4", "c3"]

        when:
        p.custom.details.monitoring = null

        then:
        p.getMonitoringProtocolCategories() == []

    }
}
