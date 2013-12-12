package au.org.ala.ecodata

import org.springframework.context.NoSuchMessageException

/**
 * Service for getting and setting static content (originally termed setting data)
 */
class SettingService {

    def messageSource

    def getSetting(String key, String defaultValue="") {
        if (!key) {
            return defaultValue
        }

        def setting = Setting.findByKey(key)
        // if user saves an empty value in Admin -> Settings, then default value is used
        if (setting && setting.value?.trim()) {
            return setting.value
        }
        return defaultValue
    }

    def setSetting(String key, String value) {
        def setting = Setting.findByKey(key)
        if (!setting) {
            setting = new Setting()
            setting.key = key
        }
        setting.value = value
        setting.save(flush: true, failOnError: true)
    }

    public String getSettingTextForKey(String key) {
        def defaultValue = getKeyMapForKey(key)
        return getSetting(key, defaultValue?:'')
    }


    public void setSettingText(String content, String key) {
        setSetting(key, content)
    }

    def getKeyMapForKey(key) {
        def defaultValue

        try {
            // See if the default value is in messages.properties
            defaultValue = messageSource.getMessage(key, null, Locale.default)
        } catch (NoSuchMessageException ex) {
            log.info "Requested i18n message not found for: ${key}"
        }

        defaultValue
    }

}
