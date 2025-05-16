package au.org.ala.ecodata

import spock.lang.Specification

class DateUtilSpec extends Specification {

    def "Dates can be placed into a financial year to support the downloads"(String dateStr, String financialYear) {
        given:
        Date date = DateUtil.parse(dateStr)

        expect:
        DateUtil.getFinancialYearBasedOnEndDate(date) == financialYear

        where:
        dateStr                 | financialYear
        "2021-06-30T14:00:00Z"  | "2020/2021"
        "2021-06-14T14:00:00Z"  | "2020/2021"
        "2021-07-01T00:00:00Z"  | "2020/2021"
        "2021-12-31T13:00:00Z"  | "2021/2022"
        "2021-07-14T14:00:00Z"  | "2021/2022"
        "2021-07-01T13:59:59Z"  | "2020/2021" // These last two entries exist to document a deliberate fudge
        "2021-07-01T14:00:00Z"  | "2021/2022" // in the algorithm to account for different timezones when dates are entered.
    }

    def "A date can be parsed and formatted using the ISO 8601 format"() {
        given:
        String date = "2021-06-30T00:12:33Z"

        expect:
        DateUtil.format(DateUtil.parse(date)) == date
    }

    def "A date can be parsed and formatted using the ISO 8601 format with milliseconds"() {
        given:
        String date = "2021-06-30T00:12:33.123Z"
4
        expect:
        DateUtil.formatWithMilliseconds(DateUtil.parseWithMilliseconds(date)) == date
    }

    def "A date can be parsed and displayed in provided timezone"(){
        given:
        String date = "2021-06-30T00:12:33Z"

        expect:
        DateUtil.convertUTCDateToStringInTimeZone(date, TimeZone.getTimeZone("Australia/Sydney"), "dd/MM/yyyy") == "30/06/2021"
    }

    def "A date can be parsed and displayed in provided timezone with and without milliseconds"(){
        given:
        String date = "2021-06-30T00:12:33.123Z"

        expect:
        DateUtil.formatWithMilliseconds(DateUtil.parseDateWithAndWithoutMilliSeconds(date)) == date

        when:
        date = "2021-06-15T00:12:33Z"

        then:
        DateUtil.format(DateUtil.parseDateWithAndWithoutMilliSeconds(date)) == date
    }
}
