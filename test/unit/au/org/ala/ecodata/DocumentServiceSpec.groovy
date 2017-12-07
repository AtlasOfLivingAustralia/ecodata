package au.org.ala.ecodata

import spock.lang.Specification

/**
 * Tests the DocumentService.
 * Images for orientation testing are from: https://github.com/recurser/exif-orientation-examples
 * The test landscape image is by Pierre Bouillot.
 * The test portrait image is by John Salvino.
 */
class DocumentServiceSpec extends Specification {

    DocumentService service = new DocumentService()
    File tempUploadDir
    File tempArchiveDir
    Map grailsApplication

    def setup() {
        File tempDir = File.createTempDir()

        tempUploadDir = new File(tempDir, "upload")
        tempUploadDir.mkdirs()
        tempArchiveDir = new File(tempDir, "archive")
        tempArchiveDir.mkdirs()
        new File(tempUploadDir, "test").mkdir()

        grailsApplication = [config: [app: [file: [archive: [path: tempArchiveDir.getAbsolutePath()], upload: [path: tempUploadDir.getAbsolutePath()]]]]]
        service.grailsApplication = grailsApplication
    }

    def cleanup() {
        tempUploadDir.delete()
        tempArchiveDir.delete()
    }

    private InputStream fileAsStream(String filename) {
        getClass().getResourceAsStream('/resources/images/'+filename)
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


}
