package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.springframework.mock.web.MockMultipartFile

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

}
