package au.org.ala.ecodata

import grails.converters.JSON

class FileScanInterceptor {
    DocumentService documentService
    int order = HIGHEST_PRECEDENCE + 15 // Run before AclInterceptor

    FileScanInterceptor() {
        matchAll().excludes(controller: 'document', actionName: "scanDocument").excludes(controller:'admin', actionName: "scanDocument") // exclude requests that are specifically for scanning
    }

    boolean before() {
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
                response.status = 400
                render contentType: 'application/json', text: [success: false, message: "File upload rejected: virus detected"] as JSON, status: 400
                return false
            }
        }

        true
    }

    boolean after() { true }

    void afterView() { }
}
