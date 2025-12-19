package au.org.ala.ecodata

import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

class DocumentControllerSpec extends Specification implements ControllerUnitTest<DocumentController> {

    DocumentService documentService = Mock(DocumentService)

    File tmpFile

    def setup() {
        controller.documentService = documentService
        File tempDir = File.createTempDir()
        File tmpUploadDir = new File(tempDir, "test")
        tmpUploadDir.mkdir()
        tmpFile = File.createTempFile("tmp", ".pdf",  tmpUploadDir)

        grailsApplication.config.app = [file: [upload: [path: tempDir.getAbsolutePath()]]]
        controller.grailsApplication = grailsApplication
    }

    def "The document service can download a file"() {

        when:
        controller.download('test', 'test.pdf')

        then:
        1 * documentService.validateDocumentFilePath('test',  "test.pdf") >> true
        1 * documentService.fullPath('test',  "test.pdf") >> tmpFile.getAbsolutePath()

        and:
        response.contentType == "application/pdf"
    }

    def "The download works with then path is specified in the filename"() {

        when:
        params.filename = "test/test.pdf"
        controller.download()

        then:
        1 * documentService.validateDocumentFilePath(null,  "test/test.pdf") >> true
        1 * documentService.fullPath(null,  "test/test.pdf") >> tmpFile.getAbsolutePath()

        and:
        response.contentType == "application/pdf"
    }

    def "The download will return an error if a file traversal is detected"() {
        when:
        controller.download('../../test', 'test.pdf')

        then:
        1 * documentService.validateDocumentFilePath('../../test',  "test.pdf") >> false
        0 * documentService._

        and:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "The download will return an error if the file doesn't exist"() {
        when:
        controller.download('test', 'test.pdf')

        then:
        1 * documentService.validateDocumentFilePath('test',  "test.pdf") >> true
        1 * documentService.fullPath('test',  "test.pdf") >> "/doesnotexist.txt"

        and:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "scanDocument should return OK when the file is clean"() {
        given:
        def mockFile = new MockMultipartFile("fileToScan", "test.txt", "text/plain", "clean content".bytes)
        request.addFile(mockFile)
        documentService.isDocumentInfected(_) >> false

        when:
        controller.scanDocument()

        then:
        response.status == HttpStatus.SC_OK
        response.json.message == "File is clean"
    }

    def "scanDocument should return BAD_REQUEST when the file is infected"() {
        given:
        def mockFile = new MockMultipartFile("fileToScan", "test.txt", "text/plain", "infected content".bytes)
        request.addFile(mockFile)
        documentService.isDocumentInfected(_) >> true

        when:
        controller.scanDocument()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.json.message == "File is infected"
    }

    def "scanDocument should return BAD_REQUEST when no file is provided"() {
        when:
        controller.scanDocument()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.json.message == "No file provided"
    }
}
