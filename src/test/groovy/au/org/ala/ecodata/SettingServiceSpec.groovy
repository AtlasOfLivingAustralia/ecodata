package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import org.springframework.context.support.StaticMessageSource
import spock.lang.Specification

class SettingServiceSpec extends Specification implements ServiceUnitTest<SettingService>, DomainUnitTest<Setting> {

    def "The getScopedSettingTextForKey method will try to get the scoped setting first, then fall back on the default"() {
        setup:
        ((StaticMessageSource)service.messageSource).addMessage("example.setting", Locale.default, "messageSource")

        Setting setting1 = new Setting(key:"hub.example.setting", value:"hub.example.setting")
        Setting setting2 = new Setting(key:"hubexample.setting", value:"hubexample.setting")
        Setting setting3 = new Setting(key:"example.setting", value:"example.setting")
        setting1.save()
        setting2.save()
        setting3.save()

        when:
        String value = service.getScopedSettingTextForKey('hub', 'example.setting')

        then:
        value == 'hub.example.setting'

        when:
        value = service.getScopedSettingTextForKey(null, 'example.setting')

        then:
        value == 'example.setting'

        when:
        setting1.delete()
        value = service.getScopedSettingTextForKey('hub', 'example.setting')

        then:
        value == 'hubexample.setting'

        when:
        setting2.delete()
        value = service.getScopedSettingTextForKey('hub', 'example.setting')

        then:
        value == 'example.setting'

        when:
        setting3.delete()
        value = service.getScopedSettingTextForKey('hub', 'example.setting')

        then:
        value == 'messageSource'

    }
}
