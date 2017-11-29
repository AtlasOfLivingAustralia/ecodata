package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.apache.commons.io.FilenameUtils
import org.springframework.mock.web.MockMultipartFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class DocumentControllerSpec extends IntegrationSpec {

    def documentController = new DocumentController()
    def documentService = new DocumentService()
    def grailsApplication

    def setup() {
        documentService.grailsApplication = grailsApplication
    }

    def cleanup() {
    }

    void "test create document"() {

        setup:
            def doc = [projectId:'TestARoo', name:'Test Document', filename:'ehcache.xml', role:'Information', dynamicProperty:'dynamicProperty']
            MockMultipartFile file = new MockMultipartFile("files", "alaLogo.jpg", "image/jpg", getClass().getResourceAsStream('/resources/alaLogo.jpg'))
            documentController.request.addFile(file)
            documentController.request.addParameter('document', (doc as JSON).toString())

        when: "creating a document"
            documentController.update('')

        then: "ensure we get a response including a documentId"
            def responseJson = JSON.parse(documentController.response.text)
            def documentId = responseJson.documentId
            documentController.response.contentType == 'application/json;charset=UTF-8'
            responseJson.message == 'created'
            documentId != null


        when: "retrieving the new document"
            documentController.response.reset()
            def savedDoc = documentController.get(documentId) // To support JSONP the controller returns a model object, which is transformed to JSON via a filter.

        then: "ensure the properties are the same as the original"
            savedDoc.projectId == doc.projectId
            savedDoc.name == doc.name
            savedDoc.filename.endsWith(doc.filename) // Filenames that are not unique are given a prefix.
            savedDoc.role == doc.role
            savedDoc.dynamicProperty == doc.dynamicProperty
            def savedFile = new File(documentService.fullPath(savedDoc.filepath, savedDoc.filename))
            savedFile.exists()

    }

    void "ensure thumbnails can be generated"() {
        setup:
        int thumbSize = 400
        InputStream input = getClass().getResourceAsStream('/resources/images/Landscape_2.jpg')
        MockMultipartFile file = new MockMultipartFile('image', 'Landscape_2.jpg', 'image/jpg', input)

        when:
        documentController.params.size = thumbSize
        documentController.request.addFile file
        documentController.createThumbnail()

        then:
        documentController.response.contentType == 'image/jpg'
        File tmp = File.createTempFile("result", "."+FilenameUtils.getExtension(file.originalFilename))
        new FileOutputStream(tmp).withStream { it << documentController.response.contentAsByteArray }

        BufferedImage img = ImageIO.read(tmp)
        Math.max(img.width, img.height) == thumbSize

        tmp.delete()
    }

}
