package au.org.ala.ecodata

import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Created by mol109 on 25/5/17.
 */

class DateUtil {
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
