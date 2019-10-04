package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

/**
 * Get a (list of) documents containing multimedia and map them onto paths.
 * <p>
 * The multimedia getter uses a closure to map the supplied information into
 * a local path or URL, as required. For example:
 * <pre>
 *     new MultimediaGetter("foo", { doc -> doc.filePath })
 * </pre>
 * <p>
 * If the property returns a collection or iterable result, the result is mapped onto a collection
 * that can be used to
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 CSIRO
 */
class MultimediaGetter implements Getter<Object> {
    PropertyAccessor property
    Closure pathMapper

    /**
     * Construct with a property accessor.
     *
     * @param property The property
     * @param pathMapper The
     */
    MultimediaGetter(PropertyAccessor property, Closure pathMapper) {
        this.property = property
        this.pathMapper = pathMapper
    }

    MultimediaGetter(String propertyName, Closure pathMapper) {
        this.property = new PropertyAccessor(propertyName)
        this.pathMapper = pathMapper
    }

    @Override
    String getPropertyName() {
        return property.propertyName
    }

    @Override
    Object getFormattedValue(Object object) {
        def value = property.get(object)

        if (!value)
            return null
        if (value instanceof Iterable) {
            def result = ((Iterable) value).collect(pathMapper).findAll { it }
            return result.isEmpty() ? null : result.size() == 1 ? result[0] : result
        }
        return pathMapper.call(value)
    }
}
