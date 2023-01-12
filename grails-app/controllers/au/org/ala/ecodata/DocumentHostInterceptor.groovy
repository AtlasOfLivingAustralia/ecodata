package au.org.ala.ecodata

import groovy.transform.CompileStatic
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes

@CompileStatic
class DocumentHostInterceptor {
    static ThreadLocal<String> documentHostUrlPrefix = new ThreadLocal<String>()

    HubService hubService
    DocumentHostInterceptor () {
        matchAll()
    }

    boolean before() {
        String hostName = request.getHeader(grailsApplication.config.getProperty('app.http.header.hostName'))
        log.warn("Found host name: $hostName in "+grailsApplication.config.getProperty('app.http.header.hostName'))
        if (hostName) {
            try {
                URI host = new URI(hostName)
                if (host.scheme && host.host?.endsWith(grailsApplication.config.getProperty('app.allowedHostName'))) {
                    hostName = "${host.scheme}://${host.host}${host.port != -1?':' + host.port : ''}"
                    log.warn("Setting threadlocal to "+hostName)
                    documentHostUrlPrefix.set(hostName)
                }
            } catch(Exception e) {
                log.error("Error parsing host name", e)
            }
        }

        true
    }

    boolean after() { true }

    void afterView() {
        documentHostUrlPrefix.set(null)
    }
}
