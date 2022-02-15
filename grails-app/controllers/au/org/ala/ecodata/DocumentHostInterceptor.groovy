package au.org.ala.ecodata

import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes

class DocumentHostInterceptor {
    static final String DOCUMENT_HOST_NAME = "DOCUMENT_HOST_NAME"

    HubService hubService
    DocumentHostInterceptor () {
        matchAll()
    }

    boolean before() {
        String hubName = request.getHeader(grailsApplication.config.app.http.header.hubUrlPath)
        if (hubService.findBioCollectHubs()?.contains(hubName)) {
            GrailsWebRequest.lookup().setAttribute(DOCUMENT_HOST_NAME, grailsApplication.config.biocollect.hostname, RequestAttributes.SCOPE_REQUEST)
        }

        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
