package au.org.ala.ecodata

class GormMongoUtil {

    // convert object to map
    static Map extractDboProperties(obj) {
        obj.collectEntries { field ->
            [field.key, field.value]
        }
    }

    static Map extractDboPropertiesWithDateConversion(obj) {
        obj.collectEntries { field ->
            def value = field.value
            if (field.value instanceof Date) {
                value = DateUtil.format(field.value)
            }
            [field.key, value]
        }
    }

    // Deep Prune a hash of all empty [:], [] preserving false and other invalid values
    static Map deepPrune(Map map) {
        map.collectEntries { k, v ->
            [k, v instanceof Map ? deepPrune(v) : v]
        }.findAll { k, v -> v != [:] && v != [] && v != null && v != 'null' && !(v instanceof org.bson.BsonUndefined) }
    }

}
