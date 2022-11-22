package au.org.ala.ecodata.metadata

import grails.util.Holders

abstract class OutputDataExtractor extends OutputModelProcessor implements OutputModelProcessor.Processor<Value> {
    static DateTimeParser TIME_PARSER = new DateTimeParser(DateTimeParser.Style.TIME)
    private DateTimeParser DATE_PARSER = new DateTimeParser(DateTimeParser.Style.DATE, timeZone)
    private static final int MAX_CELL_LENGTH = 32767

    private String propName
    private String constraint
    private Map dataNode
    private Map<String, Object> documentMap
    private def imageMapper = {
        if (it.imageId)
            return Holders.grailsApplication.config.imagesService.baseURL + "/image/details?imageId=" + it.imageId
        def doc = getDocumentMap()[it.documentId]
        return doc?.externalUrl ?: doc?.identifier ?: doc?.thumbnail ?: it.identifier ?: it.documentId
    }

    private timeZone

    static int getMaxCellLength() {
        this.MAX_CELL_LENGTH
    }

    OutputDataExtractor(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone) {
        if (!propertyName) {
            throw new IllegalArgumentException("Name cannot be null")
        }
        if (propertyName.endsWith(']')) {
            constraint = propertyName.substring(propertyName.indexOf('[')+1, propertyName.indexOf(']'))
            this.propName = propertyName.substring(0, propertyName.indexOf('['))
        }
        else {
            this.propName = propertyName
        }
        this.dataNode = dataNode
        this.documentMap = documentMap
        this.timeZone = timeZone
    }

    void init (String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone) {
        if (!propertyName) {
            throw new IllegalArgumentException("Name cannot be null")
        }
        if (propertyName.endsWith(']')) {
            constraint = propertyName.substring(propertyName.indexOf('[')+1, propertyName.indexOf(']'))
            this.propName = propertyName.substring(0, propertyName.indexOf('['))
        }
        else {
            this.propName = propertyName
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
        return val ? truncate(val as String) : ""
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
        return val ? truncate(val) : ""
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

    String getPropName(){
        this.propName
    }

    Map getDataNode(){
        this.dataNode
    }

    DateTimeParser getDateTimeParser() {
        this.DATE_PARSER
    }

    Map<String, Object> getDocumentMap() {
        this.documentMap
    }


    def getValue(outputModelOrData) {
        def value = outputModelOrData[propName]
        new Value(value)
    }

    String toString() {
        return propName
    }

    private String truncate(String value) {
        if (value.size() > MAX_CELL_LENGTH) {
            value = value.substring(0, MAX_CELL_LENGTH)
        }
        value
    }

    static class Value implements OutputModelProcessor.ProcessingContext {
        public Value(value) {
            this.value = value
        }
        def value
    }
}
