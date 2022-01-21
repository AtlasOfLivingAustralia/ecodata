package au.org.ala.ecodata

import grails.util.Holders
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus

import java.text.SimpleDateFormat
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * This job runs once per day and checks for:
 * 1) Users who haven't logged into a hub for a configurable amount of time
 * 2) Users who will soon be subject to removal of access because of condition (1)
 * 3) Specific roles that have passed the expiry date
 */
@Slf4j
class AccessExpiryJob {

    /** Used to lookup the email template warning a user that their access will soon expire */
    static final String WARNING_EMAIL_KEY = 'accessexpiry.warning.email'

    /** Used to lookup the email template informing a user that their access has expired */
    static final String ACCESS_EXPIRED_EMAIL_KEY = 'accessexpiry.expired.email'

    /** Used ot lookup the email template informing a user that their elevated permission has expired */
    static final String PERMISSION_EXPIRED_EMAIL_KEY = 'permissionexpiry.expired.email'

    /** Used ot lookup the email template informing a user that their elevated permission will expire 1 month from now */
    static final String PERMISSION_WARNING_EMAIL_KEY = 'permissionexpiry.warning.email'


    private static final int BATCH_SIZE = 100

    PermissionService permissionService
    UserService userService
    HubService hubService
    EmailService emailService

    static triggers = {
        String accessExpiryCron = Holders.config.getProperty("access.expiry.cron.expression", String, "0 10 3 * * ? *")
        // Allow the reporting server to override the default to prevent this job from running
        // on both the reporting and primary server
        if (accessExpiryCron) {
            cron name: "accessExpiry", cronExpression: accessExpiryCron
        }
    }

    /**
     * Called when the cron job is fired - checks for users and UserPermissions that need to be removed due
     * to inactivity or reaching their expiry date.
     */
    def execute() {
        ZonedDateTime processingTime = ZonedDateTime.now(ZoneOffset.UTC)
        User.withNewSession {
            processInactiveUsers(processingTime)

        }
        UserPermission.withNewSession {
            processExpiredPermissions(processingTime)
        }

        UserPermission.withNewSession {
            processWarningPermissions(processingTime)
        }
    }

    /**
     * Finds users who have not logged in for a Hub configurable amount of time, and either warns them
     * their access is due to expire, or expires their access to the Hub.
     * @param processingTime The time this job started running
     */
    void processInactiveUsers(ZonedDateTime processingTime) {
        log.info("AccessExpiryJob is searching for inactive users for processing")
        List<Hub> hubs = hubService.findHubsEligibleForAccessExpiry()
        Date processingTimeAsDate = Date.from(processingTime.toInstant())
        for (Hub hub : hubs) {
            // Get the configuration for the job from the hub
            Period period = hub.accessManagementOptions.getAccessExpiryPeriod()
            if (period) {
                Date loginDateEligibleForAccessRemoval = Date.from(processingTime.minus(period).toInstant())
                processExpiredUserAccess(hub, loginDateEligibleForAccessRemoval, processingTimeAsDate)

                period = hub.accessManagementOptions.getAccessExpiryWarningPeriod()
                if (period) {
                    Date loginDateEligibleForWarning = Date.from(processingTime.minus(period).toInstant())

                    processInactiveUserWarnings(
                            hub, loginDateEligibleForAccessRemoval, loginDateEligibleForWarning, processingTimeAsDate)
                }
            }
        }
    }

    private void processExpiredUserAccess(Hub hub, Date loginDateEligibleForAccessRemoval, Date processingTime) {
        int offset = 0
        List<User> users = userService.findUsersNotLoggedInToHubSince(hub.hubId, loginDateEligibleForAccessRemoval, offset, BATCH_SIZE)
        while (users) {
            for (User user : users) {
                UserHub userHub = user.getUserHub(hub.hubId)
                if (!userHub.accessExpired()) {
                    log.info("Deleting all permissions for user ${user.userId} in hub ${hub.urlPath}")
                    Map result = permissionService.deleteUserPermissionByUserId(user.userId, hub.hubId)
                    userHub.accessExpiredDate = processingTime
                    user.save()
                    if (result.status == HttpStatus.SC_OK) {
                        sendEmail(hub, user.userId, ACCESS_EXPIRED_EMAIL_KEY)
                    }
                }
            }
            offset += BATCH_SIZE
            users = userService.findUsersNotLoggedInToHubSince(hub.hubId, loginDateEligibleForAccessRemoval, offset, BATCH_SIZE)
        }

    }

    private void processInactiveUserWarnings(
            Hub hub, Date loginDateEligibleForWarning, Date loginDateEligibleForAccessRemoval, Date processingTime) {

        int offset = 0
        List<User> users = userService.findUsersWhoLastLoggedInToHubBetween(
                hub.hubId, loginDateEligibleForWarning, loginDateEligibleForAccessRemoval, offset, BATCH_SIZE)
        while (users) {
            for (User user : users) {

                UserHub userHub = user.getUserHub(hub.hubId)
                // Filter out users who have already been sent a warning
                if (!userHub.sentAccessRemovalDueToInactivityWarning()) {

                    log.info("Sending inactivity warning to user ${user.userId} in hub ${hub.urlPath}")
                    sendEmail(hub, user.userId, WARNING_EMAIL_KEY)
                    userHub.inactiveAccessWarningSentDate = processingTime
                    user.save()
                }
            }
            offset += BATCH_SIZE
            users = userService.findUsersNotLoggedInToHubSince(hub.hubId, loginDateEligibleForAccessRemoval, offset, BATCH_SIZE)
        }
    }

    private void sendEmail(Hub hub, String userId, String key) {
        def userDetails = userService.lookupUserDetails(userId)
        emailService.sendTemplatedEmail(
                hub.urlPath,
                key+'.subject',
                key+'.body',
                [:],
                [userDetails.email],
                [],
                hub.emailReplyToAddress,
                hub.emailFromAddress)
    }

    /**
     * Finds all UserPermissions with an expiry date that is before the supplied processing time and removes them.
     * @param processingTime the time this job started running.
     */
    void processExpiredPermissions(ZonedDateTime processingTime) {

        Date processingDate = Date.from(processingTime.toInstant())
        List permissions = permissionService.findPermissionsByExpiryDate(processingDate)
        permissions.each {

            log.info("Deleting expired permission for user ${it.userId} for entity ${it.entityType} with id ${it.entityId}")
            it.delete()

            // Find the hub attached to the expired permission.
            String hubId = permissionService.findOwningHubId(it)
            Hub hub = Hub.findByHubId(hubId)

            sendEmail(hub, it.userId, PERMISSION_EXPIRED_EMAIL_KEY)
        }
    }

    void processWarningPermissions(ZonedDateTime processingTime) {
        log.info("AccessExpiryJob process is searching for users expiring 1 month from today")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date monthFromNow = sdf.parse(processingTime.plusMonths(1).toString())

        List permissions = permissionService.findAllByExpiryDate(monthFromNow)
        permissions.each {
            // Find the hub attached to the expired permission.
            String hubId = permissionService.findOwningHubId(it)
            Hub hub = Hub.findByHubId(hubId)

            log.info("Sending expiring role warning to user ${it.userId} in hub ${hub.urlPath}")

            sendEmail(hub, it.userId, PERMISSION_WARNING_EMAIL_KEY)
        }
    }
}
