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

}
