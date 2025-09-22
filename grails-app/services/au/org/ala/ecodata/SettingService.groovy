package au.org.ala.ecodata

import groovy.util.logging.Slf4j
import org.owasp.html.HtmlChangeListener
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.context.NoSuchMessageException

/**
 * Service for getting and setting static content (originally termed setting data)
 */
@Slf4j
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

    String getSanitizedMarkdownForKey(String key) {
        String text = getSettingTextForKey(key)
        if (text) {
            return markdownToHtmlAndSanitise(text)
        }
        return text
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

    /** Allow simple formatting, links and text within p and divs by default */
    static PolicyFactory policy = (Sanitizers.FORMATTING & Sanitizers.LINKS & Sanitizers.BLOCKS) & new HtmlPolicyBuilder().allowTextIn("p", "div").toFactory()

    static String markdownToHtmlAndSanitise(String text) {
        Parser parser = Parser.builder().build()
        org.commonmark.node.Node document = parser.parse(text)
        HtmlRenderer renderer = HtmlRenderer.builder().build()
        String html = renderer.render(document)

        internalSanitise(policy, html)
    }

    private static String internalSanitise(PolicyFactory policyFactory, String input, String imageId = '', String metadataName = '') {
        policyFactory.sanitize(input, new HtmlChangeListener<Object>() {
            void discardedTag(Object context, String elementName) {
                log.warn("Dropping element $elementName in $imageId.$metadataName")
            }
            void discardedAttributes(Object context, String tagName, String... attributeNames) {
                log.warn("Dropping attributes $attributeNames from $tagName in $imageId.$metadataName")
            }
        }, null)
    }

}
