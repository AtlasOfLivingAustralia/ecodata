package au.org.ala.ecodata

import grails.plugins.mail.MailService

class EmailService {
    MailService mailService
    UserService userService
    def grailsApplication

    def sendEmail(String subjectLine, String body, Collection recipients, Collection ccList = [], String systemEmail = null, String senderEmail = null) {
        String systemEmailAddress = systemEmail ?: grailsApplication.config.ecodata.support.email.address
        String sender = (senderEmail ?: grailsApplication.config.ecodata.system.email.sender) ?: systemEmailAddress
        try {
            // This is to prevent spamming real users while testing.
            def emailFilter = grailsApplication.config.emailFilter
            if (emailFilter) {
                if (!ccList instanceof Collection) {
                    ccList = [ccList]
                }

                recipients = recipients.findAll {it ==~ emailFilter}
                if (!recipients) {
                    // The email won't be sent unless we have a to address - use the submitting user since
                    // the purpose of this is testing.
                    recipients = [userService.getCurrentUserDetails().userName]
                }

                ccList = ccList.findAll {it ==~ emailFilter}
            }
            log.info("Sending email: ${subjectLine} to: ${recipients}, cc:${ccList}, body: ${body}")

            mailService.sendMail {
                async true
                to recipients
                if (ccList) {cc ccList}
                from sender
                replyTo systemEmailAddress
                subject subjectLine
                html body

            }
        }
        catch (Exception e) {
            log.error("Failed to send email: ", e)
        }
    }

    def emailSupport(String subjectLine, String body) {
        sendEmail(subjectLine, body, [grailsApplication.config.ecodata.support.email.address])
    }
}
