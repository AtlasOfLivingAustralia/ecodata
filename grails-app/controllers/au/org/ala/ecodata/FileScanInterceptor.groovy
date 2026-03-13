package au.org.ala.ecodata

import grails.converters.JSON
import org.apache.http.HttpStatus
import xyz.capybara.clamav.commands.scan.result.ScanResult

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
            List results = []
            def files = request.getFileNames()
            while(files.hasNext()) {
                def fileName = files.next()
                def file = request.getFile(fileName)
                if (file) {
                    def inputStream = file.inputStream
                    try {
                        results << documentService.isDocumentInfected(inputStream)
                    }
                    catch (Exception e) {
                        log.error("Error scanning file ${fileName}: ${e.message}", e)
                        results << "ERROR"
                    }
                    finally {
                        inputStream.close()
                    }
                }
            }

            if (results.every { it == ScanResult.OK.INSTANCE }) {
                return true
            }
            else if (results.any { it instanceof ScanResult.VirusFound }) {
                response.status = HttpStatus.SC_UNPROCESSABLE_ENTITY
                render contentType: 'application/json', text: [success: false, message: "File upload rejected: virus detected"] as JSON, status: HttpStatus.SC_UNPROCESSABLE_ENTITY
                return false
            }
            else {
                response.status = HttpStatus.SC_INTERNAL_SERVER_ERROR
                render contentType: 'application/json', text: [success: false, message: "An error occurred during file scanning"] as JSON, status: HttpStatus.SC_INTERNAL_SERVER_ERROR
                return false
            }
        }

        true
    }

    boolean after() { true }

    void afterView() { }
}
