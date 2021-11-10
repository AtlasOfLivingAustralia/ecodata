package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class UserControllerSpec extends Specification implements ControllerUnitTest<UserController>, DataTest {

    UserService userService = Mock(UserService)
    WebService webService = Mock(WebService)

    def setup() {
        controller.userService = userService
        controller.webService = webService
        mockDomain(Hub)
        mockDomain(User)
    }

    boolean loadExternalBeans() {
        true
    }

    def cleanup() {
    }

    void "Get key - invalid credentials"() {
        setup:

        when:
        controller.getKey()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.getJson().status == 'error'
        response.getJson().error == 'Missing username or password'
    }

    void "Get key - error"() {
        setup:

        when:
        request.addHeader('userName', 'test')
        request.addHeader('password', 'test')
        controller.getKey()

        then:
        1 * userService.getUserKey('test', 'test') >>  [error: "Timed out calling web service.",  statusCode: 500]
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.getJson().status == 'error'
        response.getJson().error == 'Failed to get key for user: test'
    }

    void "Get key"() {
        setup:

        when:
        request.addHeader('userName', 'test')
        request.addHeader('password', 'test')
        controller.getKey()

        then:
        1 * userService.getUserKey('test', 'test') >>  [resp: [success:'ok']]
        1 * webService.doPostWithParams(grailsApplication.config.userDetails.url + "getUserDetails", [userName:'test']) >> [resp: [statusCode: null, userId: '1', firstName: 'test', lastName: 'test']]
        response.status == HttpStatus.SC_OK
        response.getJson().userId == '1'
        response.getJson().firstName == 'test'
        response.getJson().lastName == 'test'
    }

    def "The recordLoginTime service requires a POST"() {
        when:
        controller.recordLoginTime()
        then:
        response.status == HttpStatus.SC_METHOD_NOT_ALLOWED
    }

    def "The hubId parameter is mandatory for the recordLoginTime action"() {
        when:
        request.method = "POST"
        request.addHeader("ContentType ", "application/json")
        request.addHeader("Accept", "application/json")
        controller.recordLoginTime()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
    }

    def "The hubId supplied to the recordLoginTime action must exist"() {
        when:
        request.method = "POST"
        request.addHeader("ContentType ", "application/json")
        request.addHeader("Accept", "application/json")
        request.userId = "u1"
        request.hubId = "h1"
        controller.recordLoginTime()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
    }

    def "The recordLoginTime action delegates to the UserService"() {
        setup:
        String hubId = "h1"
        Hub hub = new Hub(hubId:hubId, urlPath: "hub")
        hub.save()

        when:
        request.method = "POST"
        request.addHeader("ContentType ", "application/json")
        request.addHeader("Accept", "application/json")
        request.json = [userId:'u1', hubId:hubId, loginTime:"2021-01-01T00:00:00Z"]
        controller.recordLoginTime()

        then:
        1 * userService.recordLoginTime(hubId, "u1", DateUtil.parse("2021-01-01T00:00:00Z")) >> new User(userId:"u1")
        response.status == HttpStatus.SC_OK

    }
}
