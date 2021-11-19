package au.org.ala.ecodata


import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * This job runs once per day and checks for:
 * 1) Users who haven't logged into a hub for a configurable amount of time
 * 2) Users who will soon be subject to removal of access because of condition (1)
 * 3) Specific roles that have passed the expiry date
 */
class AccessExpiryJob {

    /** Used to lookup the email template warning a user that their access will soon expire */
    static final String WARNING_EMAIL_KEY = 'accessexpiry.warning.email'

    /** Used to lookup the email template informing a user that their access has expired */
    static final String ACCESS_EXPIRED_EMAIL_KEY = 'accessexpiry.expired.email'

    /** Used ot lookup the email template informing a user that their elevated permission has expired */
    static final String PERMISSION_EXPIRED_EMAIL_KEY = 'permissionexpiry.expired.email'

    PermissionService permissionService
    UserService userService
    HubService hubService
    EmailService emailService

    static triggers = {
        cron name: "midnight", cronExpression: "0 0 0 * * ? *"
    }

    def execute() {
        ZonedDateTime processingTime = ZonedDateTime.now(ZoneOffset.UTC)
        processInactiveUsers(processingTime)
        processExpiredPermissions(processingTime)
    }

    void processInactiveUsers(ZonedDateTime processingTime) {
        List<Hub> hubs = hubService.findHubsEligibleForAccessExpiry()
        for (Hub hub : hubs) {

            // Get the configuration for the job from the hub
            int month = hub.accessManagementOptions.expireUsersAfterThisNumberOfMonthsInactive
            Date loginDateEligibleForAccessRemoval = Date.from(processingTime.minusMonths(month).toInstant())
            int month2 = hub.accessManagementOptions.warnUsersAfterThisNumberOfMonthsInactive
            Date loginDateEligibleForWarning = Date.from(processingTime.minusMonths(month2).toInstant())

            processExpiredUserAccess(hub, loginDateEligibleForAccessRemoval)
            processInactiveUserWarnings(hub, loginDateEligibleForAccessRemoval, loginDateEligibleForWarning)

        }
    }

    private void processExpiredUserAccess(Hub hub, Date loginDateEligibleForAccessRemoval) {
        // Expire these users
        userService.findUsersNotLoggedInToHubSince(hub.hubId, loginDateEligibleForAccessRemoval).each {
            permissionService.deleteUserPermissionByUserId(it.userId, hub.hubId)

            sendEmail(hub, it.userId, ACCESS_EXPIRED_EMAIL_KEY)
        }
    }

    private void processInactiveUserWarnings(
            Hub hub, Date loginDateEligibleForWarning, Date loginDateEligibleForAccessRemoval) {

        userService.findUsersWhoLastLoggedInToHubBetween(
                hub.hubId, loginDateEligibleForWarning, loginDateEligibleForAccessRemoval).each { User user ->

            UserHub userHub = user.getUserHub(hub.hubId)
            // Filter out users who have already been sent a warning
            if (!userHub.sentAccessRemovalDueToInactivityWarning()) {
                    Date now = new Date()
                    emailService.sendEmail(hub, user.userId, WARNING_EMAIL_KEY)
                    userHub.inactiveAccessWarningSentDate = now
                    user.save()
            }
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

    void processExpiredPermissions(ZonedDateTime processingTime) {

        Date processingDate = Date.from(processingTime.toInstant())
        permissionService.findPermissionsByExpiryDate(processingDate).each {
            it.delete()

            // Find the hub attached to the expired permission.
            String hubId = permissionService.findOwningHubId(it)
            Hub hub = Hub.findByHubId(hubId)

            emailService.sendEmail(hub, it.userId, PERMISSION_EXPIRED_EMAIL_KEY)
        }
    }
}
