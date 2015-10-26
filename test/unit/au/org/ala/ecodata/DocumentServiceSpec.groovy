package au.org.ala.ecodata

import spock.lang.Specification

class DocumentServiceSpec extends Specification {

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

        service.grailsApplication = [config: [app: [file: [archive: [path: tempArchiveDir.getAbsolutePath()], upload: [path: tempUploadDir.getAbsolutePath()]]]]]
    }

    def cleanup() {
        tempUploadDir.delete()
        tempArchiveDir.delete()
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
}
