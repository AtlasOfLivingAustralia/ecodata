package au.org.ala.ecodata

import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification
import xyz.capybara.clamav.ScanFailureException
import xyz.capybara.clamav.commands.scan.result.ScanResult

class DocumentControllerSpec extends Specification implements ControllerUnitTest<DocumentController> {

    DocumentService documentService = Mock(DocumentService)
    StorageService storageService = new FileSystemService()

    File tmpFile
    File tmpImageFile

    def setup() {
        controller.documentService = documentService
        controller.storageService = storageService
        File tempDir = File.createTempDir()
        File tmpUploadDir = new File(tempDir, "test")
        tmpUploadDir.mkdir()
        tmpFile = File.createTempFile("tmp", ".pdf",  tmpUploadDir)
        tmpImageFile = new File(tmpUploadDir, "Landscape_1.jpg")
        tmpImageFile.createNewFile()
        FileOutputStream outputStream = new FileOutputStream(tmpImageFile)
        outputStream << fileAsStream("Landscape_1.jpg")
        outputStream.close()

        grailsApplication.config.app = [file: [upload: [path: tempDir.getAbsolutePath()]]]
        controller.grailsApplication = grailsApplication
        storageService.grailsApplication = grailsApplication
    }

    private InputStream fileAsStream(String filename) {
        new File("src/test/resources/images/"+filename).newInputStream()
    }

    def "The document service can download a file"() {

        when:
        controller.download('test', tmpFile.getName())

        then:
        response.contentType == "application/pdf"
    }

    def "The document can create thumbnail on request"() {
        setup:
        controller.documentService = new DocumentService()
        controller.documentService.storageService = storageService

        when:
        controller.download('test', Document.THUMBNAIL_PREFIX + tmpImageFile.getName())

        then:
        response.contentType == "image/jpeg"
        response.status == HttpStatus.SC_OK
    }

    def "The document download should return error when file not found "() {
        setup:
        controller.documentService = new DocumentService()
        controller.documentService.storageService = storageService

        when:
        controller.download('test', Document.THUMBNAIL_PREFIX + "not_a_file.jpg")

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "The download will return an error if a file traversal is detected"() {
        when:
        controller.download('../../test', tmpFile.getName())

        then:
        0 * documentService._

        and:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "The download will return an error if the file doesn't exist"() {
        when:
        controller.download('test', 'test.pdf')

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "scanDocument should return OK when the file is clean"() {
        given:
        def mockFile = new MockMultipartFile("fileToScan", "test.txt", "text/plain", "clean content".bytes)
        request.addFile(mockFile)
        documentService.isDocumentInfected(_) >> ScanResult.OK.INSTANCE

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
        documentService.isDocumentInfected(_) >> new ScanResult.VirusFound(["test.txt": ["EICAR-Test-File"]])

        when:
        controller.scanDocument()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
        response.json.message == "File is infected"
    }

    def "scanDocument should return internal server error when scan fails for other reasons"() {
        given:
        def mockFile = new MockMultipartFile("fileToScan", "test.txt", "text/plain", "infected content".bytes)
        request.addFile(mockFile)
        documentService.isDocumentInfected(_) >> { throw new ScanFailureException("Error scanning file") }

        when:
        controller.scanDocument()

        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.json.message == "An error occurred while scanning file"
    }

    def "scanDocument should return BAD_REQUEST when no file is provided"() {
        when:
        controller.scanDocument()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.json.message == "No file provided"
    }
}
