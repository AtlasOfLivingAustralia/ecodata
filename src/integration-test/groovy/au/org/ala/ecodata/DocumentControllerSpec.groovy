package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import spock.lang.Specification
import org.apache.commons.io.FilenameUtils
//import org.springframework.mock.web.MockMultipartFile
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import grails.util.GrailsWebMockUtil
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Integration
class DocumentControllerSpec extends Specification {

    @Autowired
    DocumentController documentController

    @Autowired
    WebApplicationContext ctx

    def documentService
    def grailsApplication

    def setup() {
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)

        documentService.grailsApplication = grailsApplication
    }

    def cleanup() {
    }

    void "test create document"() {

        setup:
            def doc = [projectId:'TestARoo', name:'Test Document', filename:'ehcache.xml', role:'Information', dynamicProperty:'dynamicProperty']
            GrailsMockMultipartFile file = new GrailsMockMultipartFile("files", "alaLogo.jpg", "image/jpg",
                                                                    new File("src/integration-test/resources/alaLogo.jpg").newInputStream())
            documentController.request.addFile(file)
            documentController.request.addParameter('document', (doc as JSON).toString())

        when: "creating a document"
        Document.withTransaction {
            documentController.update('')
        }

        then: "ensure we get a response including a documentId"
         //def responseJson = JSON.parse(documentController.response.text)
            def responseJson = extractJson(documentController.response.text)
            def documentId = responseJson.documentId
            documentController.response.contentType == 'application/json;charset=UTF-8'
            responseJson.message == 'created'
            documentId != null


        when: "retrieving the new document"
            documentController.response.reset()
           // def savedDoc
            Document.withTransaction {
                documentController.get(documentId)
            }
            def savedDoc = extractJson(documentController.response.text)

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
        InputStream input = new File("src/integration-test/resources/images/Landscape_2.jpg").newInputStream() //getClass().getResourceAsStream('/resources/images/Landscape_2.jpg')
        GrailsMockMultipartFile file = new GrailsMockMultipartFile('image', 'Landscape_2.jpg', 'image/jpg', input)

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

    def extractJson (String str) {
        if(str.indexOf('{') > -1 && str.indexOf('}') > -1) {
            String jsonStr = str.substring(str.indexOf('{'), str.lastIndexOf('}') + 1)
            new JsonSlurper().parseText(jsonStr)
        }
    }

}
