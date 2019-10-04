package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter


/**
 * Get a date from a property and present it as a timezone appropriate string.
 * <p>
 * To do this, we need to have an idea about what timezone the date is in.
 * To get the timezone, we see if there is a latitude/longitude available and use that to provide a timezone.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 CSIRO
 */
class DatePropertyGetter implements Getter<String> {
    DateTimeParser parser
    PropertyAccessor property
    Getter<Double> latitudeGetter
    Getter<Double> longitudeGetter

    DatePropertyGetter(String propertyName, DateTimeParser.Style style = DateTimeParser.Style.DATETIME, Getter<Double> latitudeGetter = null, Getter<Double> longitudeGetter = null, TimeZone defaultTimeZone = TimeZone.default) {
        property = new PropertyAccessor(propertyName)
        this.latitudeGetter = latitudeGetter
        this.longitudeGetter = longitudeGetter
        this.parser = new DateTimeParser(style, defaultTimeZone)
    }

    @Override
    String getPropertyName() {
        return property.propertyName
    }

    @Override
    String getFormattedValue(Object object) {
        def lat = latitudeGetter?.getFormattedValue(object)
        def lng = longitudeGetter?.getFormattedValue(object)
        def val = property.get(object)
        if (!val)
            return val
        def calendar = parser.parse(val, lat, lng) ?: val
        return calendar ? parser.format(calendar) : null
    }
}
