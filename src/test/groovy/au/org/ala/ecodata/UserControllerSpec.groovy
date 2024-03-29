package au.org.ala.ecodata


import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification
import au.org.ala.ecodata.converter.ISODateBindingConverter

class UserControllerSpec extends Specification implements ControllerUnitTest<UserController>, DataTest {

    UserService userService = Mock(UserService)
    WebService webService = Mock(WebService)

    def setup() {
        controller.userService = userService
        controller.webService = webService
        mockDomain(Hub)
        mockDomain(User)

        defineBeans {
            formattedStringConverter(ISODateBindingConverter)
        }
    }

    def cleanup() {
    }

    def "The recordLoginTime service requires a POST"() {
        when:
        controller.recordUserLogin()
        then:
        response.status == HttpStatus.SC_METHOD_NOT_ALLOWED
    }

    def "The hubId parameter is mandatory for the recordLoginTime action"() {
        when:
        request.method = "POST"
        request.addHeader("ContentType ", "application/json")
        request.addHeader("Accept", "application/json")
        controller.recordUserLogin()

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
        controller.recordUserLogin()

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
        controller.recordUserLogin()

        then:
        1 * userService.recordUserLogin(hubId, "u1", DateUtil.parse("2021-01-01T00:00:00Z")) >> new User(userId:"u1")
        response.status == HttpStatus.SC_OK

    }
}
