package au.org.ala.ecodata

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import grails.plugins.mail.MailService
import groovy.text.GStringTemplateEngine

class EmailService implements GrailsConfigurationAware {
    MailService mailService
    UserService userService
    SettingService settingService

    String defaultReplyToAddress
    String defaultFromAddress

    /**
     * Can be specified in non-production environments to prevent emails being sent to real users
     * while still retaining the ability for emails to be sent for testing purposes
     */
    String emailFilter


    @Override
    void setConfiguration(Config config) {
        defaultReplyToAddress = config.getProperty('ecodata.support.email.address')
        defaultFromAddress = config.getProperty('ecodata.system.email.sender')
        if (!defaultFromAddress) {
            defaultFromAddress = defaultReplyToAddress
        }
        emailFilter = config.getProperty('emailFilter')
    }

    /**
     * Sends an email by obtaining the subject and body from the Settings collection and
     * substituting values supplied by the model.
     * The templates should use ${} to denote placeholders for substitution.
     * @param keyPrefix The
     * @param templateSubjectKey The key used to identify the Setting containing the template for the email subject
     * @param templateBodyKey The key used to identify the Setting containing the template for the email body.
     * @param model A map used for substitution into the templates
     * @param recipients List of addresses to send the email to.
     * @param ccList optional List of addresses to copy the email to
     * @oaram replyToAddress The address to set as the reply-to address.  If omitted the
     */
    void sendTemplatedEmail(String keyPrefix, String templateSubjectKey, String templateBodyKey, Map model, Collection recipients, Collection ccList = [], String replyToAddress = null, String fromAddress = null) {
        String subject = getEmailContent(keyPrefix, templateSubjectKey, model)
        String body = getEmailContent(keyPrefix, templateBodyKey, model)

        sendEmail(subject, body, recipients, ccList, replyToAddress, fromAddress)
    }

    private String getEmailContent(String keyPrefix, String key, Map model) {
        String templateText = settingService.getScopedSettingTextForKey(keyPrefix, key)
        GStringTemplateEngine templateEngine = new GStringTemplateEngine()
        return templateEngine.createTemplate(templateText).make(model).toString()
    }

    def sendEmail(String subjectLine, String body, Collection recipients, Collection ccList = [], String systemEmail = null, String senderEmail = null) {
        String systemEmailAddress = systemEmail ?: defaultReplyToAddress
        String sender = senderEmail ?: defaultFromAddress
        try {
            // This is to prevent spamming real users while testing.
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
        sendEmail(subjectLine, body, [defaultReplyToAddress])
    }
}
