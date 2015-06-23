package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile

class RecordControllerSpec extends IntegrationSpec {

    def recordController = new RecordController()

    def setup() {
    }

    def cleanup() {
    }
//
//    void "test record create"() {
//
//        def record = [scientificName: 'Macropus rufus', userId: "34"]
//
//        recordController.request.contentType = 'application/json;charset=UTF-8'
//        recordController.request.content = (record as JSON).toString().getBytes('UTF-8')
//        recordController.request.method = 'POST'
//
//        when: "creating a record"
//        def resp = recordController.create() // Empty or null ID triggers a create
//
//        then: "ensure we get a response including a occurrenceID"
//        def recordId = resp.occurrenceID
//        recordController.response.contentType == 'application/json'
//        recordId != null
//    }

//    void "test multipart submit"() {
//
//        given:
//        def record = [scientificName: 'Macropus rufus', userId: "34"]
//        mobileController.request.contentType = 'application/json;charset=UTF-8'
//        mobileController.request.content = (record as JSON).toString().getBytes('UTF-8')
//        mobileController.request.method = 'POST'
//        mobileController.recordService = Mock(RecordService)
//        mobileController.userService = Mock(UserService)
//
//        //construct the multipart POST
//        def multipartFile = new GrailsMockMultipartFile('image', 'image.jpeg', 'image/jpeg', new byte[0])
//        mobileController.request.addFile(multipartFile)
//        mobileController.request.setParameter("record", ([scientificName: "Macropus rufus", "userId": "34"] as JSON).toString())
//
//        when: "creating a record with multipart submit"
//        def resp = mobileController.submitRecord() // Empty or null ID triggers a create
//
//        then: "ensure we get a response including a occurrenceID"
//        def recordId = resp.occurrenceID
//        mobileController.response.status == 200
//        recordId != null
//    }
}
