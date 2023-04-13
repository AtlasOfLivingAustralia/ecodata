package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest

class ProjectActivityServiceSpec extends MongoSpec implements ServiceUnitTest<ProjectActivityService> {
    void "PA embargoed if embargoForDays is ahead of current date" () {
        setup:
        def pa =  [visibility: [alaAdminEnforcedEmbargo: true, embargoForDays: 100, embargoOption: EmbargoOption.DAYS, embargoUntil: new Date().plus(100)]]

        when:
        def result = service.isProjectActivityEmbargoed(pa)

        then:
        result == true
    }


    void "PA not embargoed if embargoForDays is behind current date" () {
        setup:
        def pa =  [visibility: [alaAdminEnforcedEmbargo: false, embargoForDays: -10, embargoOption: EmbargoOption.DAYS, embargoUntil: new Date().plus(-10)]]

        when:
        def result = service.isProjectActivityEmbargoed(pa)

        then:
        result == false
    }

    void "PA not embargoed if embargoUntil is behind current date" () {
        setup:
        def pa =  [visibility: [alaAdminEnforcedEmbargo: false, embargoOption: EmbargoOption.DATE, embargoUntil: new Date().plus(-10)]]

        when:
        def result = service.isProjectActivityEmbargoed(pa)

        then:
        result == false
    }

    void "PA embargoed if embargoUntil is ahead of current date" () {
        setup:
        def pa =  [visibility: [alaAdminEnforcedEmbargo: false, embargoOption: EmbargoOption.DATE, embargoUntil: new Date().plus(10)]]

        when:
        def result = service.isProjectActivityEmbargoed(pa)

        then:
        result == true
    }

    def "should return an notifiable property when creating project activity"() {
        given:
        def body = [foo: 1, methodName: 2]
        def old = [:]

        expect:
        service.notifiableProperties(body, old) == ['methodName']
    }

    def "should return an empty list when there are no changes to subscribed properties"() {
        given:
        def body = [foo: 1, methodName: 2]
        def old = [foo: 1, methodName: 2]

        expect:
        service.notifiableProperties(body, old) == []
    }

    def "should return a list of changed subscribed properties"() {
        given:
        def body = [foo: 1, methodName: 2]
        def old = [foo: 1, methodName: 3]

        expect:
        service.notifiableProperties(body, old) == ['methodName']
    }

    def "should not return properties that are not subscribed"() {
        given:
        def body = [foo: 1, methodName: 3]
        def old = [foo: 2, methodName: 3]

        expect:
        service.notifiableProperties(body, old) == []
    }

}
