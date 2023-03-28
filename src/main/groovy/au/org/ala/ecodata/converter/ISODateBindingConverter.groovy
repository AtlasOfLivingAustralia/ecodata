package au.org.ala.ecodata.converter

import au.org.ala.ecodata.DateUtil
import grails.databinding.converters.FormattedValueConverter

/**
 * Implements a data binder that can convert ISO 8601 dates in the UTC timezone (Z) as is used by ecodata.
 */
class ISODateBindingConverter implements FormattedValueConverter {

    static final String FORMAT = 'iso8601'

    private static final String EXAMPLE_DATE_WITH_MILLIS = '2020-01-01T00:00:00.000Z'
    @Override
    Object convert(Object value, String format) {
        Date result = null
        if (format == FORMAT) {
            String valueStr = (String)value
            if (valueStr.length() == EXAMPLE_DATE_WITH_MILLIS.length()) {
                result = DateUtil.parseWithMilliseconds(value)
            }
            else {
                result = DateUtil.parse(value)
            }
        }
        result
    }

    @Override
    Class<?> getTargetType() {
        return Date
    }

}
