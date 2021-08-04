package au.org.ala.ecodata.metadata

import com.skedgo.converter.TimezoneMapper

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Parses a date stored in a variety of formats an
 * <p>
 * More description.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 CSIRO
 */
class DateTimeParser {
    static ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    static DATE_FORMATS = [
            ISO8601_FORMAT,
            new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy"),
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
    ]

    Style style
    TimeZone defaultTimeZone

    /**
     * Construct a date/time parser with a default timezone
     *
     * @param style The formatting style (defaults to {@link Style#DATETIME})
     * @param defaultTimeZone The default timezone (defaults to {@link TimeZone#getDefault})
     */
    DateTimeParser(Style style = Style.DATETIME, TimeZone defaultTimeZone = TimeZone.default) {
        this.style = style
        this.defaultTimeZone = defaultTimeZone
    }


    /**
     * Parse the date/time, if possible
     * <p>
     * If the lat/lng is supplied then the timezone is set to the timezone of the lat/lng.
     * Otherwise the default timezone is used.
     *
     * @param value The date/time
     * @param lat The event latitude, if known, defaults to null
     * @param lng The event longitude, if known, defaults to null
     *
     * @return If the date is parseable then a {@link Calendar} with the parsed date, otherwise null
     */
    Calendar parse(Date value, Double lat = null, Double lng = null) {
        Calendar date = null
        def tz = defaultTimeZone
        if (value == null)
            return null
        if (lat != null && lng != null) {
            def zoneId = TimezoneMapper.latLngToTimezoneString(lat, lng)
            if (zoneId && TimeZone.availableIDs.contains(zoneId)) {
                tz = TimeZone.getTimeZone(zoneId)
            }
        }
        date = Calendar.getInstance(tz)
        date.setTime(value)
        return date
    }

    /**
     * Parse the date/time, if possible.
     * <p>
     * If the lat/lng is supplied then the timezone is set to the timezone of the lat/lng.
     * Otherwise the default timezone is used.
     *
     * @param value The date/time string
     * @param lat The event latitude, if known, defaults to null
     * @param lng The event longitude, if known, defaults to null
     *
     * @return If the date is parseable then a {@link Calendar} with the parsed date, otherwise null
     */
    Calendar parse(String value, Double lat = null, Double lng = null) {
        Calendar date = null
        def tz = defaultTimeZone
        if (value == null || value.isEmpty())
            return null
        DATE_FORMATS.find {
            try {
                DateFormat parser = it.clone()
                Date parsed = parser.parse(value)
                if (lat != null && lng != null) {
                    def zoneId = TimezoneMapper.latLngToTimezoneString(lat, lng)
                    if (zoneId && TimeZone.availableIDs.contains(zoneId)) {
                        tz = TimeZone.getTimeZone(zoneId)
                    }
                }
                date = Calendar.getInstance(tz)
                date.setTime(parsed)
            } catch (ParseException _ex) {
                date = null
            }
            date != null
        }
        return date
    }

    /**
     * Format a calendar.
     * <p>
     * The resuling formatted string will be in the calendar timezone
     *
     * @param calendar The calendar object
     *
     * @return The formatted strin
     */
    String format(Calendar calendar) {
            DateFormat format = style.format.clone()
            format.setCalendar(calendar)
            return format.format(calendar.getTime())
    }

    /**
     * Format a calendar.
     * <p>
     * The resuling formatted string will be in the supplied default timezone
     *
     * @param date The date
     *
     * @return The formatted strin
     */
    String format(Date date) {
        DateFormat format = style.format.clone()
        format.setTimeZone(defaultTimeZone)
        return format.format(value)
    }

    /**
     * Format a non date-line object
     *
     * @param value The value to format
     *
     * @return The object's string representation
     */
    String format(Object value) {
        return value ? value.toString() : ""
    }

    static enum Style {
        DATETIME(new SimpleDateFormat("yyyy-MM-dd HH:mm:SS")),
        DATE(new SimpleDateFormat("yyyy-MM-dd")),
        TIME(new SimpleDateFormat("HH:mm:SS"))

        DateFormat format

        Style(DateFormat format) {
            this.format = format
        }
    }


}
