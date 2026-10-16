package au.org.ala.ecodata

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.bson.BsonDocument
import org.bson.BsonValue
import org.grails.datastore.bson.codecs.CodecExtensions
import org.springframework.core.convert.converter.Converter

import java.lang.reflect.Field

/**
 * Workaround for a Groovy 5 incompatibility in GORM for MongoDB (grails-data-mongodb-bson 8.0.0).
 *
 * The BsonDocument -> Map converter registered by {@link CodecExtensions} reads entries using source[key].  In Groovy 5
 * the subscript operator resolves the key "properties" to the bean properties of the BsonDocument rather than to the
 * map entry, which then fails a cast to BsonValue.  This breaks decoding of any untyped List/Map property containing
 * embedded documents with a "properties" key (e.g. GeoJSON features in Site.features).
 *
 * CodecExtensions uses the first converter registered for a BsonValue class, so this installs a replacement converter
 * at the front of the BsonDocument converter list.
 */
@Slf4j
@CompileStatic
class BsonDocumentConverterWorkaround {

    private static volatile boolean installed = false

    static synchronized void install() {
        if (installed) {
            return
        }
        try {
            Field field = CodecExtensions.getDeclaredField('BSON_VALUE_CONVERTERS')
            field.setAccessible(true)
            Map<Class, List<Converter>> converters = (Map<Class, List<Converter>>) field.get(null)
            converters.get(BsonDocument).add(0, new BsonDocumentToMapConverter(converters))
            installed = true
        }
        catch (Exception e) {
            log.error("Unable to install BsonDocument converter workaround", e)
        }
    }

    static class BsonDocumentToMapConverter implements Converter<BsonDocument, Map<String, Object>> {
        private final Map<Class, List<Converter>> converters

        BsonDocumentToMapConverter(Map<Class, List<Converter>> converters) {
            this.converters = converters
        }

        @Override
        Map<String, Object> convert(BsonDocument source) {
            Map<String, Object> map = new LinkedHashMap<String, Object>()
            for (Map.Entry<String, BsonValue> entry : source.entrySet()) {
                map.put(entry.key, convertValue(entry.value))
            }
            map
        }

        private Object convertValue(BsonValue value) {
            if (value == null) {
                return null
            }
            List<Converter> valueConverters = converters.get(value.getClass())
            Converter converter = valueConverters ? valueConverters.first() : null
            converter != null ? converter.convert(value) : value
        }
    }
}
