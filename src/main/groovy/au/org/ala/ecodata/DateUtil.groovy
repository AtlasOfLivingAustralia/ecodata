package au.org.ala.ecodata

import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by mol109 on 25/5/17.
 */

class DateUtil {

    private static String dateFormat = "yyyy-MM-dd'T'hh:mm:ssZ"
    static DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static Date parse(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat)
        return format.parse(dateStr.replace("Z", "+0000"))
    }

    static String format(Date date) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)
        dateTime.format(ISO_DATE_FORMATTER)
    }

    /**
     * Returns a formatted string representing the financial year a report or activity falls into, based on
     * the end date.  This method won't necessarily work for start dates as it will subtract a day from the value
     * before determining the financial year due to the way report / activity dates tend to be aligned in MERIT, and
     * to account for some dates with incorrect time zone information.
     * @param endDate the end date of the report or activity.
     * @return a string formatted like yyyy / yyyy
     */
    static String getFinancialYearBasedOnEndDate(Date endDate) {
        if (!endDate) {
            return null
        }
        // Most end dates in MERIT are set to midnight on the 1st day of the next month, so to correctly
        // determine the year, it's safest to subtract a day, just in case.
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault())
        dateTime = dateTime.minusDays(1)
        int financialYear = dateTime.getYear()
        Month month = dateTime.getMonth()
        if (month.getValue() > Month.JUNE.getValue()) {
            financialYear++
        }

        return Integer.toString(financialYear-1) + "/"+Integer.toString(financialYear)
    }

    /**
     * Returns a {@link TimeZone} instance for the given clientTimezoneOffset
     *
     * clientTimezoneOffset is a String value of the minutes from GMT as provided by the JS call new Date().getTimezoneOffset()
     * @param clientTimezoneOffset the String value in minutes for the timezone offset
     * @return the TimeZone for the clientTimezoneOffset or TimeZone.default if for some reason the clientTimezoneOffset can't be converted
     */
    static TimeZone getTimeZoneFromTimezoneOffset(String clientTimezoneOffset) {
        TimeZone timeZone
        try {

            int clientTz = Integer.parseInt(clientTimezoneOffset)

            // Offset is the time difference from GMT to the local timezone so get the GMT string we need to invert the signs
            String sign = clientTz < 0 ? "+" : "-"

            clientTz = clientTz.abs()

            Number hours = clientTz / 60
            Number minutes = clientTz % 60

            DecimalFormat numberFormatter = new DecimalFormat("00")
            MathContext mathContext = new MathContext(1, RoundingMode.DOWN)


            String timeZoneStr =
                    "GMT" + sign + numberFormatter.format(hours.round(mathContext))  + ":"  + numberFormatter.format(minutes)


            timeZone = TimeZone.getTimeZone(timeZoneStr)
        }
        catch (Exception e) {
            timeZone = TimeZone.default
        }
        return timeZone
    }

    /**
     * Return the {@link TimeZone} from the given string for example "America/Los_Angeles" or the default timezone if the string does not represent a valid timezone
     * @param clientTimezoneStr the timezone as a String
     * @return The timezone
     */
    static TimeZone getTimeZoneFromString(String clientTimezoneStr) {
        TimeZone timeZone

        if (clientTimezoneStr) {
            try {
                timeZone = TimeZone.getTimeZone(clientTimezoneStr)
            } finally {
                timeZone = timeZone ?: TimeZone.default
            }
        } else {
            timeZone = timeZone ?: TimeZone.default
        }
        return timeZone
    }
}
