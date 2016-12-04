package au.org.ala.ecodata.metadata

/**
 * Get a property from an object.
 * <p>
 * A utility class for dealing with assorted property names
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 CSIRO
 */
class PropertyAccessor {
    private List<String> path

    /**
     * Construct a property getter from a list of
     *
     * @param path The path, eg ['foo', 'bar']
     */
    PropertyAccessor(List<String> path) {
        this.path = path
    }

    /**
     * Construct a property getter from a dot-separated property name
     *
     * @param property The property accesso, eg. 'foo.bar'
     */
    PropertyAccessor(String property) {
        this.path = property.split('\\.') as List<String>
    }

    def getPropertyName() {
        return path.join('.')
    }

    def get(object) {
        for (String property: path) {
            if (object == null)
                return null
            object = object[property]
        }
        return object
    }
}
