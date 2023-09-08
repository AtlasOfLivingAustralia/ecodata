package au.org.ala.ecodata

import au.org.ala.ecodata.converter.ISODateBindingConverter
import au.org.ala.ecodata.paratoo.ParatooProject
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class ParatooControllerSpec extends Specification implements ControllerUnitTest<ParatooController> { //, JsonViewUnitTest {

    UserService userService = Mock(UserService)
    ParatooService paratooService = Mock(ParatooService)
    WebService webService = Mock(WebService)

    Closure doWithSpring() {{ ->
        formattedStringConverter(ISODateBindingConverter)
    }}

    def setup() {
        controller.userService = userService
        controller.paratooService = paratooService
        controller.webService = webService
    }

    def cleanup() {
    }

    void "The user projects call delegates to the ParatooService"() {
        setup:
        String userId = 'u1'
        List<ParatooProject> projects = stubUserProjects()

        when:
        controller.userProjects()

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.userProjects(userId) >> projects

        and:
        response.status == HttpStatus.SC_OK
        model == [projects:projects]
    }

    void "The validate token calls a protected endpoint to get the framework to validate the token supplied in the body"() {
        setup:
        String token = 'I am a JWT'
        when:
        request.method = 'POST'
        request.JSON = [token:token]
        controller.validateToken()

        then:
        1 * webService.getJson({it.endsWith'/paratoo/noop'}, null, ['Authorization': token], false) >> [statusCode:HttpStatus.SC_OK]
        response.status == HttpStatus.SC_OK
        response.json == [valid:true]
    }

    void "Protocol check"() {
        setup:
        String userId = 'u1'

        when:
        response.reset()
        controller.hasReadAccess(null, "guid-1")

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        when:
        response.reset()
        controller.hasReadAccess("p1", null)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        when:
        response.reset()
        controller.hasReadAccess('p1', "guid-1")

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.protocolReadCheck(userId, 'p1', "guid-1") >> true

        and:
        response.status == HttpStatus.SC_OK
        response.json == [isAuthorised:true]

        when:
        response.reset()
        controller.hasReadAccess('p2', "guid-1")

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.protocolReadCheck(userId, 'p2', "guid-1") >> false

        and:
        response.status == HttpStatus.SC_OK
        response.json == [isAuthorised:false]
    }

    void "Mint collection id called with invalid body"() {
        when:
        request.method = 'POST'
        request.json = [projectId: 'p1']
        controller.mintCollectionId()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
    }

    void "Mint collection id"() {
        setup:
        String userId = 'u1'

        when:
        request.method = "POST"
        request.json = buildCollectionIdJson()
        controller.mintCollectionId()

        then:
        1 * userService.currentUserDetails >> [userId: userId]
        1 * paratooService.protocolWriteCheck(userId, 'p1', "guid-1") >> true
        1 * paratooService.mintCollectionId(_) >> [orgMintedIdentifier:"id1"]

        and:
        response.status == HttpStatus.SC_OK
        response.json.orgMintedIdentifier == "id1"
    }

    void "We attempt to mint a collection id for a project or protocol we don't have permissions for"() {
        setup:
        String userId = 'u1'

        when:
        request.method = "POST"
        request.json = buildCollectionIdJson()
        controller.mintCollectionId()

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.protocolWriteCheck(userId, 'p1', "guid-1") >> false

        and:
        response.status == HttpStatus.SC_FORBIDDEN
        response.json == [code:HttpStatus.SC_FORBIDDEN, message:"Project / protocol combination not available"]

    }

    void "The request body is not correct for a call to submitCollection, a status code of UNPROCESSABLE_ENTITY will be returned"() {
        when:
        request.method = 'POST'
        request.json = [projectId: 'p1']
        controller.mintCollectionId()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
    }

    void "We attempt to submit a collection for a project or protocol we don't have permissions for"() {
        setup:
        String userId = 'u1'
        Map collection = buildCollectionJson()

        when:
        request.method = "POST"
        request.json = collection
        controller.submitCollection()

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.findDataSet(userId, collection.orgMintedIdentifier) >> [project:new ParatooProject(id:'p1'), dataSet:[:]]
        1 * paratooService.protocolWriteCheck(userId, 'p1', "guid-1") >> false

        and:
        response.status == HttpStatus.SC_FORBIDDEN
        response.json == [code:HttpStatus.SC_FORBIDDEN, message:"Project / protocol combination not available"]

    }

    void "We submit a collection for a project and protocol"() {
        setup:
        String userId = 'u1'
        Map collection = buildCollectionJson()
        Map searchResults = [project:new ParatooProject(id:'p1'), dataSet:[:]]

        when:
        request.method = "POST"
        request.json = collection
        controller.submitCollection()

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.findDataSet(userId, collection.orgMintedIdentifier) >> searchResults
        1 * paratooService.protocolWriteCheck(userId, 'p1', "guid-1") >> true
        1 * paratooService.submitCollection({it.orgMintedIdentifier == "c1"}, searchResults.project) >> [:]

        and:
        response.status == HttpStatus.SC_OK
        response.json == [success:true]

    }

    void "We submit a collection for a project and protocol and an error is encountered"() {
        setup:
        String userId = 'u1'
        Map collection = buildCollectionJson()
        Map searchResults = [project:new ParatooProject(id:'p1'), dataSet:[:]]

        when:
        request.method = "POST"
        request.json = collection
        controller.submitCollection()

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.findDataSet(userId, collection.orgMintedIdentifier) >> searchResults
        1 * paratooService.protocolWriteCheck(userId, 'p1', "guid-1") >> true
        1 * paratooService.submitCollection({it.orgMintedIdentifier == "c1"}, searchResults.project) >> [error:"Error"]

        and:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.json == [code:HttpStatus.SC_INTERNAL_SERVER_ERROR, message:"Error"]

    }

    void "The call to find data set"() {
        setup:
        String userId = 'u1'

        when:
        request.method = "GET"
        params.id = "c1"
        controller.collectionIdStatus()

        then:
        1 * userService.currentUserDetails >> [userId:userId]
        1 * paratooService.findDataSet(userId, 'c1') >> [dataSet:[progress:Activity.STARTED]]

        and:
        response.status == HttpStatus.SC_OK
        response.json == [isSubmitted:true]

    }



    private List<ParatooProject> stubUserProjects() {
        ParatooProject project = new ParatooProject(id:'projectId')
        [project]
    }

    private Map buildCollectionIdJson() {
        [
            "surveyId": [
                    surveyType: "Bird",
                    time: "2023-01-01T00:00:00Z",
                    randNum: 1234,
                    "projectId":"p1",
                    "protocol": [
                            "id": "guid-1",
                            "version": 1
                    ]
            ]
        ]
    }

    private Map buildCollectionJson() {
        [
                "orgMintedIdentifier":"c1",
                "projectId":"p1",
                "userId": "u1",
                "protocol": [
                        "id": "guid-1",
                        "version": 1
                ],
                "eventTime":"2023-01-01T00:00:00Z"
        ]
    }

}
