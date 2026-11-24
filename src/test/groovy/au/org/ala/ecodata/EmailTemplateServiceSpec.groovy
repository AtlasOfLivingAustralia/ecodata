package au.org.ala.ecodata

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class EmailTemplateServiceSpec extends Specification implements ServiceUnitTest<EmailTemplateService> {

    def "the download complete template can be rendered outside of a web request"() {
        when:
        String body = service.render("downloadComplete", [url: "http://test/download/1?fileExtension=zip&a=b", days: 3])

        then:
        body.contains('<a href="http://test/download/1?fileExtension=zip&amp;a=b">')
        body.contains('after 3 days')
    }

    def "the download failed template can be rendered without a model"() {
        expect:
        service.render("downloadFailed").contains("An unexpected error has occurred")
    }

    def "nested model values are HTML escaped"() {
        when:
        String body = service.render("speciesAlert", [values: [projectName: "<script>x</script>", name: "Test"]])

        then:
        body.contains("&lt;script&gt;x&lt;/script&gt;")
        !body.contains("<script>")
    }

    def "an exception is thrown for a missing template"() {
        when:
        service.render("doesNotExist")

        then:
        thrown(IllegalArgumentException)
    }
}
