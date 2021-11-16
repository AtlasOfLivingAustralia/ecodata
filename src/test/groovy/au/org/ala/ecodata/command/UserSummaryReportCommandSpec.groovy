package au.org.ala.ecodata.command

import au.org.ala.ecodata.Hub
import au.org.ala.ecodata.UserService
import grails.testing.gorm.DomainUnitTest
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

class UserSummaryReportCommandSpec extends Specification implements DomainUnitTest<Hub> {

    UserService userService = Mock(UserService)

    UserSummaryReportCommand command
    def setup() {
        command = new UserSummaryReportCommand()
        command.userService = userService
    }

    def "The command will use the logged in user email if the email paramter is not supplied"() {
        when:
        command.validate()

        then:
        1 * userService.getCurrentUserDetails() >> [userName:"test@test.com"]
        command.email == "test@test.com"
        command.hasErrors()
    }

    def "The command will attempt to populate some missing parameters from the Hub"() {
        setup:
        String hubId = "merit"
        String replyEmail = "no-reply@test.com"
        String senderEmail = "sender@test.com"
        new Hub(
                hubId:hubId,
                urlPath:"merit",
                emailFromAddress: senderEmail,
                emailReplyToAddress: replyEmail,
                downloadUrlPrefix: '/download/').save()

        when:
        command.hubId = "merit"
        command.validate()

        then:
        1 * userService.getCurrentUserDetails() >> [userName:"test@test.com"]
        command.email == "test@test.com"
        command.senderEmail == senderEmail
        command.systemEmail == replyEmail
        command.hubId == hubId
        command.downloadUrl == '/download/'
        !command.hasErrors()
    }

    def "The command will overwrite params with it's derived values"() {
        setup:
        String hubId = "hub1"
        new Hub(
                hubId:hubId,
                urlPath:"merit").save()
        when:
        command.hubId = hubId
        command.downloadUrl = "/download/"
        command.senderEmail = "senderEmail"
        command.systemEmail = "systemEmail"
        command.email = "email"
        GrailsParameterMap map = command.populateParams(new GrailsParameterMap(new MockHttpServletRequest()))

        then:
        map.hubId == hubId
        map.downloadUrl == "/download/"
        map.senderEmail == "senderEmail"
        map.systemEmail == "systemEmail"
        map.email == "email"
    }

}
