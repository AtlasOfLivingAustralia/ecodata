package au.org.ala.ecodata

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes
import spock.lang.Specification

class DocumentHostInterceptorSpec extends Specification implements InterceptorUnitTest<DocumentHostInterceptor> {
    def hubService

    def setup() {
        interceptor.hubService = hubService = Stub(HubService)
    }

    def cleanup() {

    }

    void "interceptor must set document host name if request is coming from biocollect hub"() {
        given:
        def hostName = 'https://biocollect.ala.org.au'
        def controller = (DocumentationController) mockController(DocumentationController)
        request.addHeader(grailsApplication.config.app.http.header.hostName, hostName)

        when:
            withInterceptors([controller: DocumentationController]) {
                controller.getProjectSites()
            }

        then:
            GrailsWebRequest.lookup().getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST) == hostName
    }

    void "interceptor must reject requests with a not allowed hostname "() {
        given:
        def hostName = 'https://example.com'
        def controller = (DocumentationController) mockController(DocumentationController)
        request.addHeader(grailsApplication.config.app.http.header.hostName, hostName)

        when:
        withInterceptors([controller: DocumentationController]) {
            controller.getProjectSites()
        }

        then:
        GrailsWebRequest.lookup().getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST) == null
    }


    void "interceptor must not set document host name if incorrect url is supplied"() {
        given:
        def hostName = ''
        def controller = (DocumentationController) mockController(DocumentationController)
        request.addHeader(grailsApplication.config.app.http.header.hostName, hostName)

        when:
        withInterceptors([controller: DocumentationController]) {
            controller.getProjectSites()
        }

        then:
        GrailsWebRequest.lookup().getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST) == null
    }
}
