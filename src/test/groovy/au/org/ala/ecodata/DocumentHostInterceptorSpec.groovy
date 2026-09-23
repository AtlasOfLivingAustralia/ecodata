package au.org.ala.ecodata

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification
import spock.lang.Unroll

class DocumentHostInterceptorSpec extends Specification implements InterceptorUnitTest<DocumentHostInterceptor> {
    def hubService

    def setup() {
        interceptor.hubService = hubService = Stub(HubService)
    }

    def cleanup() {
        DocumentHostInterceptor.clearDocumentHostUrlPrefix()
    }

    @Unroll
    void "interceptor must set the document host name to #expected when the host name header is '#hostName'"() {
        given:
        String headerName = grailsApplication.config.getProperty('app.http.header.hostName')
        request.addHeader(headerName, hostName)

        when:
        boolean proceed = interceptor.before()

        then:
        proceed
        DocumentHostInterceptor.documentHostUrlPrefix.get() == expected

        when: "the request has been rendered"
        interceptor.afterView()

        then: "the thread local is cleared"
        DocumentHostInterceptor.documentHostUrlPrefix.get() == null

        where:
        hostName                            | expected
        'https://biocollect.ala.org.au'     | 'https://biocollect.ala.org.au'
        'https://biocollect.ala.org.au:8080'| 'https://biocollect.ala.org.au:8080'
        'https://example.com'               | null
        'biocollect.ala.org.au'             | null
        ''                                  | null
    }
}
