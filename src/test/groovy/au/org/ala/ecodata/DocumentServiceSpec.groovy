package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import grails.util.Holders

/**
 * Tests the DocumentService.
 * Images for orientation testing are from: https://github.com/recurser/exif-orientation-examples
 * The test landscape image is by Pierre Bouillot.
 * The test portrait image is by John Salvino.
 */
class DocumentServiceSpec extends MongoSpec implements ServiceUnitTest<DocumentService>, DomainUnitTest<Document> {

    DocumentService service = new DocumentService()
    File tempUploadDir
    File tempArchiveDir

    def setup() {
        File tempDir = File.createTempDir()

        tempUploadDir = new File(tempDir, "upload")
        tempUploadDir.mkdirs()
        tempArchiveDir = new File(tempDir, "archive")
        tempArchiveDir.mkdirs()
        new File(tempUploadDir, "test").mkdir()

        grailsApplication.config.app = [file: [archive: [path: tempArchiveDir.getAbsolutePath()], upload: [path: tempUploadDir.getAbsolutePath()]], uploads: [url: '/document/download/']]
        service.grailsApplication = grailsApplication
        Holders.config.app = grailsApplication.config.app
        service.webService = Mock(WebService)
    }

    def cleanup() {
        tempUploadDir.delete()
        tempArchiveDir.delete()
    }

    private InputStream fileAsStream(String filename) {
        new File("src/test/resources/images/"+filename).newInputStream()
    }

    def "archiveFile should move the file from the main directory to the 'archive' directory"() {
        setup:
        File file = new File(tempUploadDir, "test/file1.txt")
        file.createNewFile()

        Document document = new Document(filepath: "test", filename: "file1.txt")

        expect:
        tempUploadDir.listFiles()[0].list().size() == 1
        tempArchiveDir.listFiles().size() == 0

        when:
        service.archiveFile(document)

        then:
        tempUploadDir.listFiles()[0].list().size() == 0
        tempArchiveDir.listFiles()[0].list().size() == 1
    }

    def "the documentService will ensure a unique filename for new Documents"() {
        setup:
        Document d = new Document(filepath: 'Mar-17', filename: 'Landscape_1.jpg', name:'Test Image')

        when:
        service.saveFile(d.filepath, d.filename, fileAsStream(d.filename), false, Document.DOCUMENT_TYPE_IMAGE)
        String filename = service.saveFile(d.filepath, d.filename, fileAsStream(d.filename), false, Document.DOCUMENT_TYPE_IMAGE)

        then:
        new File(grailsApplication.config.app.file.upload.path + File.separator + d.filepath + File.separator + d.filename).exists()
        new File(grailsApplication.config.app.file.upload.path + File.separator + d.filepath + File.separator + '0_' + d.filename).exists()
        filename == '0_'+d.filename

    }

    def "thumbnails can be created for an image document"() {
        setup:
        Document d = new Document(filepath: 'Mar-17', filename: 'Landscape_1.jpg', name:'Test Image')

        when:
        service.saveFile(d.filepath, d.filename, fileAsStream(d.filename), false, Document.DOCUMENT_TYPE_IMAGE)

        then:
        new File(grailsApplication.config.app.file.upload.path + File.separator + d.filepath + File.separator + Document.THUMBNAIL_PREFIX+d.filename).exists()
    }

    def "a rotated copy of an image will be created depending on the exif orientation"(String filename, boolean result) {
        setup:
        Document d = new Document(filepath: 'Mar-17', filename:filename, name:'Test Image')

        when:
        service.saveFile(d.filepath, d.filename, fileAsStream(d.filename), false, Document.DOCUMENT_TYPE_IMAGE)

        then:
        File f = new File(grailsApplication.config.app.file.upload.path + File.separator + d.filepath + File.separator + Document.PROCESSED_PREFIX+d.filename)
        // Checking the image has been orientated correctly is difficult as the operation is not lossless.
        f.exists() == result

        where:
        filename | result
        'Landscape_1.jpg' | false
        'Landscape_2.jpg' | true
        'Landscape_3.jpg' | true
        'Landscape_4.jpg' | true
        'Landscape_5.jpg' | true
        'Landscape_6.jpg' | true
        'Landscape_7.jpg' | true
        'Landscape_8.jpg' | true

    }

    def "The full path method prepends the app.file.upload.path config item"() {
        expect:
        service.fullPath("2020-01", "myfile.jpg") == tempUploadDir.getAbsolutePath() + File.separator + "2020-01" + File.separator + "myfile.jpg"
        service.fullPath("2020-01", "myfile.jpg", true) == tempUploadDir.getCanonicalPath() + File.separator + "2020-01" + File.separator + "myfile.jpg"
    }

    def "The document service can validate a path to protect from file traversal vulnerabilities"(String path, String filename, boolean expectedResult) {
        expect:
        service.validateDocumentFilePath(path, filename) == expectedResult

        where:
        path      | filename        | expectedResult
        "2020-01" | "file1.jpg"     | true
        "../../"  | "file"          | false
        null      | "../../../file" | false
        ""        | "../../../file" | false
        "/etc/"   | "file"          | false
    }

    def "findImageUrlForProjectId should provide provide thumbnail url as well as full image url"(){
        setup:
        def projectId = 'abc'
        def url
        Document d = new Document(documentId: 'doc1', filepath: '2022-03', filename: 'Landscape_1.jpg', name:'Test Image', projectId: projectId, type: Document.DOCUMENT_TYPE_IMAGE, role: Document.ROLE_LOGO, status: Status.ACTIVE)
        d.save(flush: true, failOnError: true)
        service.saveFile(d.filepath, d.filename, fileAsStream(d.filename), false, Document.DOCUMENT_TYPE_IMAGE)

        when:
        url = service.findImageUrlForProjectId(projectId)

        then:
        url.contains('thumb_')

        when:
        url = service.findImageUrlForProjectId(projectId, false)

        then:
        !url.contains('thumb_')
        url.endsWith('2022-03' + File.separator + "Landscape_1.jpg")

        cleanup:
        d.delete(flush: true)
    }

    def "If a local copy of the file doesn't exist, the DocumentService should try the URL"() {
        setup:
        Map doc = [name:'doc1', path:'path1', url:'https://host/document/download']

        when:
        service.readJsonDocument(doc)

        then:
        1 * service.webService.getJson(doc.url, null, null, true)
    }

}
