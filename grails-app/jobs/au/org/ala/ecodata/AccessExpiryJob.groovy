package au.org.ala.ecodata


import groovy.util.logging.Slf4j

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

    private static final int BATCH_SIZE = 100

    PermissionService permissionService
    UserService userService
    HubService hubService
    EmailService emailService

    static triggers = {
        cron name: "midnight", cronExpression: "0 0 0 * * ? *"
    }

    /**
     * Called when the cron job is fired - checks for users and UserPermissions that need to be removed due
     * to inactivity or reaching their expiry date.
     */
    def execute() {
        ZonedDateTime processingTime = ZonedDateTime.now(ZoneOffset.UTC)
        processInactiveUsers(processingTime)
        processExpiredPermissions(processingTime)
    }

    /**
     * Finds users who have not logged in for a Hub configurable amount of time, and either warns them
     * their access is due to expire, or expires their access to the Hub.
     * @param processingTime The time this job started running
     */
    void processInactiveUsers(ZonedDateTime processingTime) {
        log.info("AccessExpiryJob is searching for inactive users for processing")
        List<Hub> hubs = hubService.findHubsEligibleForAccessExpiry()
        for (Hub hub : hubs) {

            // Get the configuration for the job from the hub
            int month = hub.accessManagementOptions.expireUsersAfterThisNumberOfMonthsInactive
            Date loginDateEligibleForAccessRemoval = Date.from(processingTime.minusMonths(month).toInstant())
            int month2 = hub.accessManagementOptions.warnUsersAfterThisNumberOfMonthsInactive
            Date loginDateEligibleForWarning = Date.from(processingTime.minusMonths(month2).toInstant())

            processExpiredUserAccess(hub, loginDateEligibleForAccessRemoval)
            processInactiveUserWarnings(
                    hub, loginDateEligibleForAccessRemoval, loginDateEligibleForWarning, Date.from(processingTime.toInstant()))

        }
    }

    private void processExpiredUserAccess(Hub hub, Date loginDateEligibleForAccessRemoval) {
        int offset = 0
        List<User> users = userService.findUsersNotLoggedInToHubSince(hub.hubId, loginDateEligibleForAccessRemoval, offset, BATCH_SIZE)
        while (users) {
            for (User user : users) {
                log.info("Deleting all permissions for user ${user.userId} in hub ${hub.urlPath}")
                permissionService.deleteUserPermissionByUserId(user.userId, hub.hubId)
                sendEmail(hub, user.userId, ACCESS_EXPIRED_EMAIL_KEY)
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
        permissionService.findPermissionsByExpiryDate(processingDate).each {

            log.info("Deleting expired permission for user ${it.userId} for entity ${it.entityType} with id ${it.entityId}")
            it.delete()

            // Find the hub attached to the expired permission.
            String hubId = permissionService.findOwningHubId(it)
            Hub hub = Hub.findByHubId(hubId)

            sendEmail(hub, it.userId, PERMISSION_EXPIRED_EMAIL_KEY)
        }
    }
}
