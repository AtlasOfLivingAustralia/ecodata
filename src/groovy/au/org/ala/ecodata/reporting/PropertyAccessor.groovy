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
        if (!nestedProperty) {
            throw new IllegalArgumentException("nestedProperty cannot be null")
        }
        this.propertyToGroupOn = nestedProperty.split('\\.')
    }

    public boolean isNested(Map output) {
        int i=0
        def data = output
        while (i < propertyToGroupOn.length - 1) {
            if (!data) {
                return false
            }

            data = data[propertyToGroupOn[i]]
            if (data instanceof List) {
                return true
            }
            i++
        }
        return false
    }

    public List<Map> unroll(Map output) {
        List<Map> unrolled = []
        int i = 0
        def value = output

        while (i < propertyToGroupOn.length - 1) {
            String propertyName = propertyToGroupOn[i]
            value = value[propertyName]

            if (value instanceof List) {
                value.each {
                    Map unrolledCopy = new HashMap(output)
                    Map currentPropertyValue = unrolledCopy
                    for (int j=0; j<i; j ++) {
                        def copy = new HashMap(currentPropertyValue[propertyToGroupOn[j]])
                        currentPropertyValue[propertyToGroupOn[j]] = copy
                        currentPropertyValue = copy
                    }
                    currentPropertyValue.remove(propertyName)
                    currentPropertyValue[propertyName] = it
                    unrolled << unrolledCopy

                }
                break // Only support one level of list
            }
            i++
        }
        unrolled
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
