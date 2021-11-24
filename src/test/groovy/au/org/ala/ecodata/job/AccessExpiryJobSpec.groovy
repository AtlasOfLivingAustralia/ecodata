package au.org.ala.ecodata.job

import au.org.ala.ecodata.*
import grails.test.mongodb.MongoSpec
import org.apache.http.HttpStatus
import org.grails.testing.GrailsUnitTest

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AccessExpiryJobSpec extends MongoSpec implements GrailsUnitTest {

    AccessExpiryJob job = new AccessExpiryJob()
    HubService hubService = Mock(HubService)
    UserService userService = Mock(UserService)
    EmailService emailService = Mock(EmailService)
    PermissionService permissionService = Mock(PermissionService)
    Hub merit

    private void deleteAll() {
        User.findAll().each{it.delete(flush:true)}
        UserPermission.findAll().each{it.delete(flush:true)}
        Hub.findAll().each{it.delete(flush:true)}
    }

    def setup() {
        deleteAll()
        AccessManagementOptions options = new AccessManagementOptions()
        options.warnUsersAfterThisNumberOfMonthsInactive = 23
        options.expireUsersAfterThisNumberOfMonthsInactive = 24
        merit = new Hub(hubId:'h1', urlPath:'merit')
        merit.accessManagementOptions = options
        merit.save(flush:true, failOnError:true)
        job.hubService = hubService
        job.userService = userService
        job.emailService = emailService
        job.permissionService = permissionService
    }

    def cleanup() {
        deleteAll()
    }

    def "The access expiry job will remove all access for users who have not logged in for a specified amount of time"() {
        setup:
        ZonedDateTime processTime = ZonedDateTime.parse("2021-01-01T00:00:00Z", DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC)
        User user = new User(userId:'u1', userHubs: [new UserHub(hubId:merit.hubId)])

        when:
        job.processInactiveUsers(processTime)

        then:
        1 * hubService.findHubsEligibleForAccessExpiry() >> [merit]
        1 * userService.findUsersNotLoggedInToHubSince("h1", DateUtil.parse("2019-01-01T00:00:00Z"), 0, 100) >> [user]
        1 * userService.findUsersWhoLastLoggedInToHubBetween("h1", DateUtil.parse("2019-01-01T00:00:00Z"), DateUtil.parse("2019-02-01T00:00:00Z"), 0, 100) >> []

        1 * permissionService.deleteUserPermissionByUserId(user.userId, merit.hubId) >> [status: HttpStatus.SC_OK]
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
        user.getUserHub(merit.hubId).accessExpiredDate == Date.from(processTime.toInstant())
        user.getUserHub(merit.hubId).accessExpired()
    }

    def "The access expiry job will send warning emails to users who have not logged in for a specified amount of time"() {
        setup:
        ZonedDateTime processTime = ZonedDateTime.parse("2021-01-01T00:00:00Z", DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC)
        User user = new User(userId:'u1', userHubs: [new UserHub(hubId:merit.hubId)])
        user.loginToHub(merit.hubId, DateUtil.parse("2019-01-31T00:00:00Z"))
        user.save()

        when:
        job.processInactiveUsers(processTime)

        then:
        1 * hubService.findHubsEligibleForAccessExpiry() >> [merit]
        1 * userService.findUsersNotLoggedInToHubSince("h1", DateUtil.parse("2019-01-01T00:00:00Z"), 0, 100) >> []
        1 * userService.findUsersWhoLastLoggedInToHubBetween("h1", DateUtil.parse("2019-01-01T00:00:00Z"), DateUtil.parse("2019-02-01T00:00:00Z"), 0, 100) >> [user]
        0 * permissionService.deleteUserPermissionByUserId(_, _)

        1 * userService.lookupUserDetails(user.userId) >> [email:'test@test.com']
        1 * emailService.sendTemplatedEmail(
                'merit',
                AccessExpiryJob.WARNING_EMAIL_KEY+'.subject',
                AccessExpiryJob.WARNING_EMAIL_KEY+'.body',
                [:],
                ["test@test.com"],
                [],
                merit.emailReplyToAddress,
                merit.emailFromAddress)
        user.getUserHub(merit.hubId).inactiveAccessWarningSentDate == Date.from(processTime.toInstant())
        user.getUserHub(merit.hubId).sentAccessRemovalDueToInactivityWarning()
    }

    def "The access expiry job will expire UserPermission entries that have passed their expiry date"() {
        setup:
        ZonedDateTime processTime = ZonedDateTime.parse("2021-01-01T00:00:00Z", DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC)
        UserPermission permission = new UserPermission(userId:"u1", entityType: Project.class.name, entityId:'p1', accessLevel: AccessLevel.admin)
        permission.save()

        when:
        job.processExpiredPermissions(processTime)

        then:
        1 * permissionService.findPermissionsByExpiryDate(Date.from(processTime.toInstant())) >> [permission]
        1 * permissionService.findOwningHubId(permission) >> merit.hubId
        1 * userService.lookupUserDetails(permission.userId) >> [email:'test@test.com']
        1 * emailService.sendTemplatedEmail(
                merit.urlPath,
                AccessExpiryJob.PERMISSION_EXPIRED_EMAIL_KEY+'.subject',
                AccessExpiryJob.PERMISSION_EXPIRED_EMAIL_KEY+'.body',
                [:],
                ["test@test.com"],
                [],
                merit.emailReplyToAddress,
                merit.emailFromAddress)
        and: "The permission was deleted"
        !UserPermission.findAllByUserId(permission.userId)

    }

}
