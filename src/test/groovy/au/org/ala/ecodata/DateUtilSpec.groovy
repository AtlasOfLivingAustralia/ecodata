package au.org.ala.ecodata

import spock.lang.Specification

class DateUtilSpec extends Specification {

    def "A date can be parsed and formatted using the ISO 8601 format"() {
        given:
        String date = "2021-06-30T00:12:33Z"

        expect:
        DateUtil.format(DateUtil.parse(date)) == date
    }
}
