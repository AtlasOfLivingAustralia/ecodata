package au.org.ala.ecodata

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification
import xyz.capybara.clamav.ScanFailureException
import xyz.capybara.clamav.commands.scan.result.ScanResult

class FileScanInterceptorSpec extends Specification implements InterceptorUnitTest<FileScanInterceptor> {
    def documentService

    def setup() {
        interceptor.documentService = documentService = Mock(DocumentService)
    }

    void "interceptor allows clean files to pass through"() {
        given:
        def controller = (DocumentationController) mockController(DocumentationController)
        def file = Mock(org.springframework.web.multipart.MultipartFile)
        file.name >> "clean"
        file.inputStream >> new ByteArrayInputStream("clean file content".bytes)
        request.addFile(file)

        when:
        def result = withInterceptors(controller: DocumentationController) {
            controller.getProjectSites()
        }

        then:
        1 * documentService.isDocumentInfected(_ as InputStream) >> ScanResult.OK.INSTANCE
        result == []
        response.status == 200
    }

    void "interceptor blocks infected files"() {
        given:
        def controller = (DocumentationController) mockController(DocumentationController)
        def file = Mock(org.springframework.web.multipart.MultipartFile)
        file.name >> "infected"
        file.inputStream >> new ByteArrayInputStream("infected file content".bytes)
        request.addFile(file)

        when:
        def result = withInterceptors(controller: DocumentationController) {
            controller.getProjectSites()
        }

        then:
        1 * documentService.isDocumentInfected(_ as InputStream) >> new ScanResult.VirusFound(["infected.text": ["EICAR-Test-File"]])
        response.status == 422
        result == null
    }

    void "interceptor should check multiple files"() {
        given:
        def controller = (DocumentationController) mockController(DocumentationController)
        def cleanFile = Mock(org.springframework.web.multipart.MultipartFile)
        cleanFile.name >> "clean"
        cleanFile.originalFilename >> "clean.txt"
        cleanFile.inputStream >> new ByteArrayInputStream("clean file content".bytes)
        def infectedFile = Mock(org.springframework.web.multipart.MultipartFile)
        infectedFile.name >> "infected"
        infectedFile.originalFilename >> "infected.txt"
        infectedFile.inputStream >> new ByteArrayInputStream("infected file content".bytes)
        request.getFileNames() >> ['clean.txt', 'infected.txt'].iterator()
        request.addFile(cleanFile)
        request.addFile(infectedFile)

        when:
        def result = withInterceptors(controller: DocumentationController) {
            controller.getProjectSites()
        }

        then:
        1 * documentService.isDocumentInfected(cleanFile.inputStream) >> ScanResult.OK.INSTANCE
        1 * documentService.isDocumentInfected(infectedFile.inputStream) >> new ScanResult.VirusFound(["infected.txt": ["EICAR-Test-File"]])
        response.status == 422
        result == null
    }

    void "interceptor should return internal server error if an exception is encountered"() {
        given:
        def controller = (DocumentationController) mockController(DocumentationController)
        def cleanFile = Mock(org.springframework.web.multipart.MultipartFile)
        cleanFile.name >> "clean"
        cleanFile.originalFilename >> "clean.txt"
        cleanFile.inputStream >> new ByteArrayInputStream("clean file content".bytes)
        request.getFileNames() >> ['clean.txt'].iterator()
        request.addFile(cleanFile)

        when:
        def result = withInterceptors(controller: DocumentationController) {
            controller.getProjectSites()
        }

        then:
        1 * documentService.isDocumentInfected(cleanFile.inputStream) >> { throw new ScanFailureException("Scan failed") }
        response.status == 500
        result == null
    }
}
