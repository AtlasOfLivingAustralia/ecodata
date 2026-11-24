package au.org.ala.ecodata

import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import org.springframework.web.util.HtmlUtils

import java.util.concurrent.ConcurrentHashMap

/**
 * Renders email bodies from Groovy templates on the classpath (src/main/resources/email).
 * Unlike the GSP PageRenderer, this has no dependency on a web request so can be used from
 * background threads, promises and jobs.
 *
 * String values in the model are HTML escaped, matching the default GSP expression codec.
 */
class EmailTemplateService {

    static transactional = false

    private static final String TEMPLATE_PATH = 'email/'
    private static final String TEMPLATE_EXTENSION = '.tpl'

    private final StreamingTemplateEngine engine = new StreamingTemplateEngine()
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>()

    String render(String templateName, Map model = [:]) {
        Template template = templateCache.computeIfAbsent(templateName) { String name -> loadTemplate(name) }
        template.make(htmlEscape(model ?: [:]) as Map).toString()
    }

    private Template loadTemplate(String name) {
        String path = TEMPLATE_PATH + name + TEMPLATE_EXTENSION
        URL resource = getClass().classLoader.getResource(path)
        if (!resource) {
            throw new IllegalArgumentException("Email template not found on classpath: ${path}")
        }
        engine.createTemplate(resource.getText('UTF-8'))
    }

    private def htmlEscape(value) {
        if (value instanceof CharSequence) {
            return HtmlUtils.htmlEscape(value.toString())
        }
        if (value instanceof Map) {
            return value.collectEntries { k, v -> [(k): htmlEscape(v)] }
        }
        if (value instanceof Collection) {
            return value.collect { htmlEscape(it) }
        }
        value
    }
}
