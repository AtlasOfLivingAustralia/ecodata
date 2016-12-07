package au.org.ala.ecodata.metadata

import grails.util.Holders
import pl.touk.excel.export.getters.Getter

class OutputDataPropertiesBuilder extends OutputModelProcessor implements OutputModelProcessor.Processor<Value>, Getter<String> {
    static DateTimeParser TIME_PARSER = new DateTimeParser(DateTimeParser.Style.TIME)
    static DateTimeParser DATE_PARSER = new DateTimeParser(DateTimeParser.Style.DATE)

    private String[] nameParts
    private String constraint
    private List outputDataModel
    private Map<String, Object> documentMap
    private def imageMapper = {
        if (it.imageId)
            return Holders.grailsApplication.config.imagesService.baseURL + "/image/details?imageId=" + it.imageId
        def doc = documentMap[it.documentId]
        return doc?.externalUrl ?: doc?.identifier ?: doc?.thumbnail ?: it.identifier ?: it.documentId
    }


    public OutputDataPropertiesBuilder(String name, outputDataModel, Map<String, Object> documentMap) {
        if (!name) {
            throw new IllegalArgumentException("Name cannot be null")
        }
        if (name.endsWith(']')) {
            constraint = name.substring(name.indexOf('[')+1, name.indexOf(']'))
            name = name.substring(0, name.indexOf('['))
        }
        this.nameParts = name.tokenize('.');

        this.outputDataModel = outputDataModel;
        this.documentMap = documentMap
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
        return val ?: ""
    }

    // Implementation of Getter<String>
    @Override
    String getPropertyName() {
        return nameParts.join('.');
    }

    @Override
    String getFormattedValue(Object output) {
        def result = ''
        def node = outputDataModel
        for (String part : nameParts) {
            def tmpNode = node.find { it.name == part }
            // List typed model elements have a cols element containing nested nodes.
            node = tmpNode.columns ?: tmpNode
        }
        try {
            result = processNode(this, node, getValue(output))
        }
        catch (Exception e) {
            log.error("Error getting value from output: ${output?.outputId}, property: ${nameParts.join('.')}", e)
        }
        result
    }

    def getValue(outputModelOrData) {
        def value = outputModelOrData[nameParts[nameParts.size() - 1]]
        new Value(value)
    }

    String toString() {
        return nameParts?.join(", ")
    }

    static class Value implements OutputModelProcessor.ProcessingContext {
        public Value(value) {
            this.value = value
        }
        def value
    }
}