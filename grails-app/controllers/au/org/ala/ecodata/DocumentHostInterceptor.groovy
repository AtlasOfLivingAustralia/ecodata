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
        String hostName = request.getHeader(grailsApplication.config.app.http.header.hostName)
        if (hostName) {
            try {
                URI host = new URI(hostName)
                if (host.scheme && host.host?.endsWith(grailsApplication.config.app.allowedHostName)) {
                    hostName = "${host.scheme}://${host.host}${host.port != -1?':' + host.port : ''}"
                    GrailsWebRequest.lookup().setAttribute(DOCUMENT_HOST_NAME, hostName, RequestAttributes.SCOPE_REQUEST)
                }
            } catch(Exception e) {
                log.error("Error parsing host name", e)
            }
        }

        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
