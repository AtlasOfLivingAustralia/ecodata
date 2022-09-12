package au.org.ala.ecodata

import groovy.transform.CompileStatic
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes

@CompileStatic
class DocumentHostInterceptor {
    static final String DOCUMENT_HOST_NAME = "DOCUMENT_HOST_NAME"

    /** The HTTP header to look for the host name */
    private String hostNameHeader

    /** Allowed domain suffix for the hast name */
    private String allowedHostSuffix

    HubService hubService
    DocumentHostInterceptor () {
        matchAll()
        hostNameHeader = grailsApplication.config.getProperty('app.http.header.hostName')
        allowedHostSuffix = grailsApplication.config.getProperty('app.allowedHostName')
    }

    boolean before() {
        String hostName = request.getHeader(hostNameHeader)
        if (hostName) {
            try {
                URI host = new URI(hostName)
                if (host.scheme && host.host?.endsWith(allowedHostSuffix)) {
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
