package au.org.ala.ecodata

import grails.plugins.mail.MailService
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class EmailServiceSpec extends Specification implements ServiceUnitTest<EmailService> {

    MailService mailService = Mock(MailService)
    SettingService settingService = Mock(SettingService)

    void setup() {
        service.mailService = mailService
        service.settingService = settingService
    }

    def "The email service can evaluate templates and delegate to the mailService to send the email"() {
        setup:
        String templateSubjectKey = "email.subject.key"
        String templateBodyKey = "email.body.key"
        String subjectTemplate = "Hi"
        String bodyTemplate = "This is a templated email for \${test}"
        Map model = [test:"test"]

        when:
        service.sendTemplatedEmail('merit', templateSubjectKey, templateBodyKey, model, ["to@test.com"])

        then:
        1 * settingService.getScopedSettingTextForKey('merit', templateSubjectKey) >> subjectTemplate
        1 * settingService.getScopedSettingTextForKey('merit', templateBodyKey) >> bodyTemplate

        1 * mailService.sendMail({it instanceof Closure})
    }
}
