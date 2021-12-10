package au.org.ala.ecodata.converter

import au.org.ala.ecodata.DateUtil
import spock.lang.Specification

import java.text.ParseException

class ISODateBindingConverterSpec extends Specification {

    ISODateBindingConverter converter = new ISODateBindingConverter()


    def "It should only convert values matching it's format string"() {
        expect:
        converter.convert("2021-01-01T00:00:00Z", ISODateBindingConverter.FORMAT) == DateUtil.parse("2021-01-01T00:00:00Z")
        converter.convert("2021-01-01T00:00:00Z", "yyyy-MM-dd") == null
    }

    def "Invalid values can throw an exception as this will be handled by the data binding infrastruture"() {
        when:
        converter.convert("not a date", ISODateBindingConverter.FORMAT)

        then:
        thrown(ParseException)
    }
}
