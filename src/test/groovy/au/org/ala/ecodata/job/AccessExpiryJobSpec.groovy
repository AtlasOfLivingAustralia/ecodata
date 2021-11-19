package au.org.ala.ecodata.job

import au.org.ala.ecodata.AccessExpiryJob
import au.org.ala.ecodata.AccessManagementOptions
import au.org.ala.ecodata.DateUtil
import au.org.ala.ecodata.EmailService
import au.org.ala.ecodata.Hub
import au.org.ala.ecodata.HubService
import au.org.ala.ecodata.PermissionService
import au.org.ala.ecodata.User
import au.org.ala.ecodata.UserService
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AccessExpiryJobSpec extends Specification implements GrailsUnitTest {

    AccessExpiryJob job = new AccessExpiryJob()
    HubService hubService = Mock(HubService)
    UserService userService = Mock(UserService)
    EmailService emailService = Mock(EmailService)
    PermissionService permissionService = Mock(PermissionService)
    Hub merit

    def setup() {
        AccessManagementOptions options = new AccessManagementOptions()
        options.warnUsersAfterThisNumberOfMonthsInactive = 23
        options.expireUsersAfterThisNumberOfMonthsInactive = 24
        merit = new Hub(hubId:'h1', urlPath:'merit')
        merit.accessManagementOptions = options
        job.hubService = hubService
        job.userService = userService
        job.emailService = emailService
        job.permissionService = permissionService
    }

    def "The access expiry job will remove all access for users who have not logged in for a specified amount of time"() {
        setup:
        ZonedDateTime processTime = ZonedDateTime.parse("2021-01-01T00:00:00Z", DateTimeFormatter.ISO_DATE_TIME)
        User user = new User(userId:'u1')

        when:
        job.processInactiveUsers(processTime)

        then:
        1 * hubService.findHubsEligibleForAccessExpiry() >> [merit]
        1 * userService.findUsersNotLoggedInToHubSince("h1", DateUtil.parse("2019-01-01T00:00:00Z")) >> [user]
        1 * permissionService.deleteUserPermissionByUserId(user.userId, merit.hubId)
        1 * userService.lookupUserDetails(user.userId) >> [email:'test@test.com']
        1 * emailService.sendTemplatedEmail(
                'merit',
                AccessExpiryJob.ACCESS_EXPIRED_EMAIL_KEY+'.subject',
                AccessExpiryJob.ACCESS_EXPIRED_EMAIL_KEY+'.body',
                [:],
                ["test@test.com"],
                [],
                merit.emailReplyToAddress,
                merit.emailFromAddress)
    }

}
