package au.org.ala.ecodata

import groovy.transform.CompileStatic
import org.grails.web.json.JSONObject

/**
 * Key lookup helpers for code that navigates arbitrary Map structures (JSON schemas, GeoJSON, form data) using
 * keys only known at runtime.
 *
 * In Groovy 5 the property style and subscript forms (map.properties, map['properties'], map[key]) resolve the
 * keys "properties" and "metaClass" to the Map object's own bean properties rather than to the map entry.
 * In addition, the Grails {@link org.grails.web.json.JSONObject} has a strict get(String) overload that throws
 * when the key is missing.  These helpers always perform a lenient Map entry lookup (missing key returns null),
 * matching the behaviour of the code prior to the Grails 8 / Groovy 5 upgrade.
 */
class MapKeyAccess {

    /**
     * Returns the value for the key if the target is a Map, otherwise falls back to the standard Groovy
     * subscript/property access (e.g. for domain objects or lists).
     */
    static Object valueAt(Object target, Object key) {
        if (target == null) {
            return null
        }
        if (target instanceof Map) {
            return mapEntry((Map) target, key)
        }
        target[key]
    }

    /** Statically compiled so the call binds to Map.get(Object) rather than JSONObject.get(String) as JSONObject.get throws an Exception if the key is missing. */
    @CompileStatic
    static Object mapEntry(Map map, Object key) {
        map.get(key)
    }
}

