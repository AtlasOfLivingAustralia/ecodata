package au.org.ala.ecodata.reporting

import org.apache.log4j.Logger

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException

/**
 * Helper class for accessing data from an activity or output.
 */
class PropertyAccessor {

    static def log = Logger.getLogger(getClass())

    static ThreadLocal<NumberFormat> numberFormat = new ThreadLocal<NumberFormat>()

    private String[] propertyToGroupOn

    public PropertyAccessor(String nestedProperty) {
        this.propertyToGroupOn = nestedProperty.split('\\.')
    }

    /** Returns the value of the nested property from the supplied data object */
    def getPropertyValue(data) {
        for (String prop : propertyToGroupOn) {
            if (!data) {
                return null
            }
            data = data[prop]
        }
        return data
    }

    BigDecimal getPropertyAsNumeric(data) {
        return getValueAsNumeric(getPropertyValue(data))
    }

    BigDecimal getValueAsNumeric(value) {
        def numeric = null
        if (value instanceof String) {

            try {
                numeric = getFormatter().parse(value)
            }
            catch (ParseException e) {
                log.warn("Attemping to access non-numeric value: ${value} for property: ${propertyToGroupOn.join('.')}")
            }
        }
        else if (value instanceof Number) {
            numeric = value
        }
        else {
            log.warn("Attemping to aggregate non-numeric value: ${value} for property: ${propertyToGroupOn.join('.')}")
        }
        return numeric
    }

    private static NumberFormat getFormatter() {
        DecimalFormat formatter = numberFormat.get()
        if (!formatter) {
            formatter =  DecimalFormat.getInstance(Locale.default)
            formatter.setParseBigDecimal(true)
            numberFormat.set(formatter)
        }
        return formatter
    }


}
