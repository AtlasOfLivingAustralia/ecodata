package au.org.ala.ecodata

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class HubServiceIntegrationSpec extends Specification {

    @Autowired
    HubService hubService

    def setup() {
        Hub.collection.drop()
    }

    def cleanup() {
        Hub.collection.drop()
    }

    void "create a hub from properties and make sure HTML scripts are removed"() {
        setup:
        Map props = [urlPath: "test", title: "Test hub", templateConfiguration: [header: [links: [[introductoryText: "<script>alert('test')</script>", displayName: "Test", href: "/home/index"]]]]]
        Map result
        Map hub
        when:
        Hub.withTransaction {
            result = hubService.create(props)
        }


        then:
        result.status == 'ok'
        result.hubId != null

        when:
        Hub.withTransaction {
            hub = hubService.toMap(hubService.get(result.hubId))
        }

        then:


        then:
        hub.title == "Test hub"
        hub.hubId == result.hubId
        hub.templateConfiguration.header.links[0].introductoryText == ""
    }

    void "update a hub with HTML scripts in the properties"() {
        setup:
        Map props = [hubId: "hub1", urlPath: "test", title: "Test hub", templateConfiguration: [header: [links: [[introductoryText: "<script>alert('test')</script>", displayName: "Test", href: "/home/index"]]]]]
        Map result
        Hub hub
        String hubId
        when:
        Hub.withTransaction {
            hub = new Hub(props)
            hub.save(flush: true, failOnError: true)
            hubId = hub.hubId
        }

        then:
        hubId != null

        when:
        Hub.withTransaction {
            result = hubService.update(hubId, [title: "Updated title", templateConfiguration: [header: [links: [[introductoryText: "<h1 class='c1'>test</h1><script>alert('test')</script>", displayName: "Test", href: "/home/index"]]]]])
        }

        then:
        result.status == 'ok'


        when:
        Hub.withTransaction {
            hub = hubService.get(hubId)
        }

        then:
        hub.title == "Updated title"
        hub.hubId == hubId // bind data should exclude changes to hubId
        hub.templateConfiguration.header.links[0].introductoryText == "<h1>test</h1>"
    }
}
