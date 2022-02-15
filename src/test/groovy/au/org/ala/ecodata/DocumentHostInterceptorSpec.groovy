package au.org.ala.ecodata

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes
import spock.lang.Specification

class DocumentHostInterceptorSpec extends Specification implements InterceptorUnitTest<DocumentHostInterceptor> {
    def hubService

    Closure doWithConfig() {{ config ->
        config.biocollect.hostname = "https://biocollect.ala.org.au"
    }}

    def setup() {
        interceptor.hubService = hubService = Stub(HubService)
    }

    def cleanup() {

    }

    void "interceptor must set document host name if request is coming from biocollect hub"() {
        given:
        interceptor.hubService.findBioCollectHubs() >> ['ala']
        def controller = (DocumentationController) mockController(DocumentationController)
        request.addHeader(grailsApplication.config.app.http.header.hubUrlPath, 'ala')

        when:
            withInterceptors([controller: DocumentationController]) {
                controller.getProjectSites()
            }

        then:
            GrailsWebRequest.lookup().getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST) == grailsApplication.config.biocollect.hostname
    }

    void "interceptor must set document host name if request is coming from MERIT"() {
        given:
        interceptor.hubService.findBioCollectHubs() >> ['ala']
        def controller = (DocumentationController) mockController(DocumentationController)

        when:
        withInterceptors([controller: DocumentationController]) {
            controller.getProjectSites()
        }

        then:
        GrailsWebRequest.lookup().getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST) == null
    }
}
