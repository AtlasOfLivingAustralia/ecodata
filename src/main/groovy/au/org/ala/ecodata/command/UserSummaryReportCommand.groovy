package au.org.ala.ecodata.command

import au.org.ala.ecodata.Hub
import au.org.ala.ecodata.UserService
import grails.validation.Validateable
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.beans.factory.annotation.Value

/**
 * Validates and sets default inputs for the User Summary report
 */
class UserSummaryReportCommand implements Validateable {

    @Value('${ecodata.system.email.sender}')
    private String defaultFromEmail
    @Value('${ecodata.system.email.replyTo}')
    private String defaultReplyToEmail

    UserService userService

    /** The hub to report on */
    String hubId

    /** The email address to send the report to */
    String email

    /** The prefix for the report download link */
    String downloadUrl

    /** The replyTo address for the email */
    String systemEmail

    /** The from address for the email */
    String senderEmail

    /**
     * Checks for missing parameters and apply defaults if needed.
     */
    void beforeValidate() {
        if (!email) {
            email = userService.getCurrentUserDetails()?.userName
        }
        if ((!systemEmail || !senderEmail || !downloadUrl) && hubId) {
            Hub targetHub = Hub.findByHubId(hubId)
            if (!senderEmail) {
                senderEmail = targetHub.emailFromAddress ?: defaultFromEmail
            }
            if (!systemEmail) {
                systemEmail = targetHub.emailReplyToAddress ?: defaultReplyToEmail
            }
            if (!downloadUrl) {
                downloadUrl = targetHub.downloadUrlPrefix
            }
        }
    }

    /**
     * This is unfortunately required because the downloadService currently expects to work with
     * a GrailsParameterMap instead of a Map interface.
     * Overwrites/sets values in the GrailsParameterMap with the values from this command.
     * @param params the GrailsParameterMap to populate.
     * @return the same GrailsParameterMap
     */
    GrailsParameterMap populateParams(GrailsParameterMap params) {
        params.fileExtension = 'csv'
        params.email = email
        params.systemEmail = systemEmail
        params.senderEmail = senderEmail
        params.downloadUrl = downloadUrl
        params.hubId = hubId
        params
    }

}
