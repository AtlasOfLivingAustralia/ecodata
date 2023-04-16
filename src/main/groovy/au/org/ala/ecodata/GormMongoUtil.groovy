package au.org.ala.ecodata

import grails.gorm.DetachedCriteria
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.springframework.validation.Errors

class GormMongoUtil {

    // convert object to map
    @Deprecated
    /** The dbo property of domain objects is deprecated */
    static Map extractDboProperties(obj) {
        obj.collectEntries { field ->
            [field.key, field.value]
        }
    }

    // Deep Prune a hash of all empty [:], [] preserving false and other invalid values
    static Map deepPrune(Map map) {
        map.collectEntries { k, v ->
            [k, v instanceof Map ? deepPrune(v) : v]
        }.findAll { k, v -> v != [:] && v != [] && v != null && v != 'null' && !(v instanceof org.bson.BsonUndefined) }
    }

    /**
     * Helper method to compare the value of a property with the value currently stored in the database and
     * reject it if it is different.
     * If the entity is not currently in the database, or the value of the property in the database is null
     * this method will not result in a new Error.
     * Otherwise, an error will be added with the key "<entity class name>.<property name>.cannotBeReassigned"
     * @param entity The entity instance to validate
     * @param identifier The unique id to use when querying the database
     * @param writeOnceProperty The property to validate
     * @param errors THe Errors object to use.
     */
    static void validateWriteOnceProperty(Object entity, String identifier, String writeOnceProperty, Errors errors) {
        if (entity[identifier]) {

            // A criteria query is used to bypass any caching that occurs by GORM. (e.g. entity.findByXXX will use
            // the cache if available)
            Object storedPropertyValue = new DetachedCriteria(entity.class).get {
                eq(identifier, entity[identifier])
                projections {
                    property(writeOnceProperty)
                }
            }

            if (storedPropertyValue && storedPropertyValue != entity[writeOnceProperty]) {
                errors.rejectValue(writeOnceProperty, "${entity.class.name}.${writeOnceProperty}.cannotBeReassigned")
            }
        }
    }

}
