package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

class OutputDataPropertiesBuilder extends OutputModelProcessor implements OutputModelProcessor.Processor<Value>, Getter<String> {

    private String[] nameParts
    private List outputDataModel

    public OutputDataPropertiesBuilder(String name, outputDataModel) {
        this.nameParts = name.tokenize('.');
        this.outputDataModel = outputDataModel;
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
    def date(Object node, Value outputValue) {
        return new Value(outputValue ?: "") // dates are UTC formatted strings already
    }

    @Override
    def image(Object node, Value outputValue) {
        return ""
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
            val = val.join(',')
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