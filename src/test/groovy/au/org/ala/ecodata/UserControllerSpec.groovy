package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class UserControllerSpec extends Specification implements ControllerUnitTest<UserController>, DataTest{

    UserService userService = Mock(UserService)
    WebService webService = Mock(WebService)

    def setup() {
        controller.userService = userService
        controller.webService = webService
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
}
