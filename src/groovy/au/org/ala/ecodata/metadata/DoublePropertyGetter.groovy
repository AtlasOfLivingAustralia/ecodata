package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.PropertyGetter

/**
 * File description.
 * <p>
 * More description.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 CSIRO
 */
class DoublePropertyGetter extends PropertyGetter<Object, Double> {
    DoublePropertyGetter(String propertyName) {
        super(propertyName)
    }

    @Override
    protected format(Object value) {
        if (value instanceof Number)
            return ((Number) value.doubleValue())
        value = value.toString()
        try {
            return Double.parseDouble(value)
        } catch (any) {
            return null
        }
    }
}
