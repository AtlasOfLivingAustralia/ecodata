package au.org.ala.ecodata

import grails.async.Promises
import grails.testing.mixin.integration.Integration
import grails.util.GrailsWebMockUtil
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

@Integration
class NoRequestPropagationTaskDecoratorSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    def "the Grails web request task decorator is replaced"() {
        expect:
        applicationContext.getBean('grailsWebRequestTaskDecorator') instanceof NoRequestPropagationTaskDecorator
    }

    def "Promise tasks do not inherit the web request of the submitting thread"() {
        setup:
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()

        when:
        def requestInTask = Promises.task { GrailsWebRequest.lookup() }.get()

        then:
        GrailsWebRequest.lookup() == webRequest
        requestInTask == null
    }
}
