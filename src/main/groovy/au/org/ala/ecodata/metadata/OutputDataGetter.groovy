package au.org.ala.ecodata.metadata

import grails.util.Holders
import pl.touk.excel.export.getters.Getter

class OutputDataGetter extends OutputModelProcessor implements OutputModelProcessor.Processor<Value>, Getter<String> {
    static DateTimeParser TIME_PARSER = new DateTimeParser(DateTimeParser.Style.TIME)
    private DateTimeParser DATE_PARSER = new DateTimeParser(DateTimeParser.Style.DATE, timeZone)

    private String propertyName
    private String constraint
    private Map dataNode
    private Map<String, Object> documentMap
    private def imageMapper = {
        if (it.imageId)
            return Holders.grailsApplication.config.imagesService.baseURL + "/image/details?imageId=" + it.imageId
        def doc = documentMap[it.documentId]
        return doc?.externalUrl ?: doc?.identifier ?: doc?.thumbnail ?: it.identifier ?: it.documentId
    }

    private timeZone


    OutputDataGetter(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone) {
        if (!propertyName) {
            throw new IllegalArgumentException("Name cannot be null")
        }
        if (propertyName.endsWith(']')) {
            constraint = propertyName.substring(propertyName.indexOf('[')+1, propertyName.indexOf(']'))
            this.propertyName = propertyName.substring(0, propertyName.indexOf('['))
        }
        else {
            this.propertyName = propertyName
        }
        this.dataNode = dataNode
        this.documentMap = documentMap
        this.timeZone = timeZone
    }

    @Override
    def number(Object node, Value outputValue) {
        def val = outputValue.value
        return val ? val as String : ""
    }

    @Override
    def integer(Object node, Value outputValue) {
        def val = outputValue.value
        return val ? val as String : ""
    }

    @Override
    def text(Object node, Value outputValue) {
        def val = outputValue.value
        return val ? val as String : ""
    }

    @Override
    def time(Object node, Value outputValue) {
        def val = outputValue.value
        if (!val)
            return ""
        def time = TIME_PARSER.parse(val)
        return time ? TIME_PARSER.format(time) : val
    }

    @Override
    def date(Object node, Value outputValue) {
        def val = outputValue.value
        if (!val)
            return ""
        def date = DATE_PARSER.parse(val)
        return date ? DATE_PARSER.format(date) : val
    }

    @Override
    def image(Object node, Value outputValue) {
        def val = outputValue.value
        if (!val)
            return ""
        if (val instanceof Iterable) {
            def result = ((Iterable) val).collect(imageMapper).findAll { it }
            return result.isEmpty() ? "" : result.size() == 1 ? result[0] : result
        }
        return pathMapper.call(val)
    }

    @Override
    def embeddedImages(Object node, Value outputValue) {
        return ""
    }

    @Override
    def species(Object node, Value outputValue) {
        def val = outputValue.value

        return val ? val.name : ""
    }

    @Override
    def stringList(Object node, Value outputValue) {
        def val = outputValue.value
        if (val instanceof List) {
            if (constraint) {
                val = val.contains(constraint) ? constraint : ''
            }
            else {
                val = val.join(',')
            }
        }
        return val ?: ""
    }

    @Override
    def booleanType(Object node, Value outputValue) {
        def val = outputValue.value
        if (val instanceof Boolean) {
            val = Boolean.parseBoolean("${val}")
        }
        return val ?: ""
    }

    @Override
    def document(Object node, Value outputValue) {
        def val = outputValue.value
        if (val instanceof Map) {
            val = val?.documentId
        }
        return val ?: ""
    }

    @Override
    def feature(Object node, Value outputValue) {
        return '' // the feature data type stores references to site data.
    }

    // Implementation of Getter<String>
    @Override
    String getPropertyName() {
        return propertyName
    }

    @Override
    String getFormattedValue(Object output) {
        String result = ''
        try {
            result = processNode(this, dataNode, getValue(output))
        }
        catch (Exception e) {
            OutputModelProcessor.log.error("Error getting value from output: ${output?.outputId}, property: ${propertyName}", e)
        }
        result
    }

    def getValue(outputModelOrData) {
        def value = outputModelOrData[propertyName]
        new Value(value)
    }

    String toString() {
        return propertyName
    }

    static class Value implements OutputModelProcessor.ProcessingContext {
        public Value(value) {
            this.value = value
        }
        def value
    }
}
