package au.org.ala.ecodata

import grails.converters.JSON
import org.apache.http.HttpStatus

class FileScanInterceptor {
    DocumentService documentService
    int order = HIGHEST_PRECEDENCE + 15 // Run before AclInterceptor
    def dependsOn = [ApiKeyInterceptor, PreAuthoriseInterceptor]

    FileScanInterceptor() {
        matchAll().excludes(controller: 'document', action: "scanDocument").excludes(controller:'admin', action: "scanDocument") // exclude requests that are specifically for scanning
    }

    boolean before() {
        if (!grailsApplication.config.getProperty('scanFile.enabled', Boolean, true)) {
            return true
        }

        if (request.respondsTo('getFile')) {
            boolean infected = false
            def files = request.getFileNames()
            while(files.hasNext()) {
                def fileName = files.next()
                def file = request.getFile(fileName)
                if (file) {
                    def inputStream = file.inputStream
                    infected |= documentService.isDocumentInfected(inputStream)
                }
            }

            if (infected) {
                response.status = HttpStatus.SC_UNPROCESSABLE_ENTITY
                render contentType: 'application/json', text: [success: false, message: "File upload rejected: virus detected"] as JSON, status: HttpStatus.SC_UNPROCESSABLE_ENTITY
                return false
            }
        }

        true
    }

    boolean after() { true }

    void afterView() { }
}
