package au.org.ala.ecodata.converter

import au.org.ala.ecodata.DateUtil
import grails.databinding.converters.FormattedValueConverter

/**
 * Implements a data binder that can convert ISO 8601 dates in the UTC timezone (Z) as is used by ecodata.
 */
class ISODateBindingConverter implements FormattedValueConverter {

    static final String FORMAT = 'iso8601'

    @Override
    Object convert(Object value, String format) {
        Date result = null
        if (format == FORMAT) {
            result = DateUtil.parse(value)
        }
        result
    }

    @Override
    Class<?> getTargetType() {
        return Date
    }

}
