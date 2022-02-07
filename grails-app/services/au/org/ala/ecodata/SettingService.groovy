package au.org.ala.ecodata

import org.springframework.context.NoSuchMessageException

/**
 * Service for getting and setting static content (originally termed setting data)
 */
class SettingService {

    def messageSource

    String getSetting(String key, String defaultValue="") {
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

    String getSettingTextForKey(String key) {
        String defaultValue = getKeyMapForKey(key)
        return getSetting(key, defaultValue?:'')
    }

    /**
     * This method attempts to get a value stored in the Settings collection for a supplied key with an
     * optional namespace.
     * It first tries the key prefixed with the hubPrefix+'.', then prefixed with only the hubPrefix,
     * then the key without a prefix.  Finally, if it still hasn't found a result, it look up the key in
     * the message bundle.
     *
     * The purpose of this is to allow hubs to override certain values, while allowing for ecodata wide defaults
     * to be applied.
     *
     * @param scope the scope for the key, will be used as a prefix.  Normal usage is to use the Hub name or urlPath.
     * @param key the key specifying the setting to lookup
     */
    String getScopedSettingTextForKey(String scope, String key) {
        List keys = scope ? [scope+'.'+key, scope+key, key] : [key]
        String value = null
        for (String attempt in keys) {
            value = getSetting(attempt)
            if (value) {
                break
            }
        }
        if (!value) {
            value = getKeyMapForKey(key)
        }
        value
    }


    void setSettingText(String content, String key) {
        setSetting(key, content)
    }

    String getKeyMapForKey(key) {
        String defaultValue = null

        try {
            // See if the default value is in messages.properties
            defaultValue = messageSource.getMessage(key, null, Locale.default)
        } catch (NoSuchMessageException ex) {
            log.debug "Requested i18n message not found for: ${key}, ${ex.getMessage()}"
        }

        defaultValue
    }

}
