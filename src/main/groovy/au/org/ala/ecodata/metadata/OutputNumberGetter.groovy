package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

class OutputNumberGetter extends OutputDataExtractor implements Getter<Number> {
    OutputNumberGetter(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone){
        super(propertyName, dataNode, documentMap, timeZone)
    }

    @Override
    def number(Object node, Value outputValue) {
        def val = outputValue.value?.toString()
        try {
            if (val instanceof String) {
                return Double.parseDouble(val)
            }

            return val
        } catch (NumberFormatException nfe){
            log.debug("Double conversion error - ${val}", nfe)
        }
    }

    @Override
    def integer(Object node, Value outputValue) {
        def val = outputValue.value?.toString()
        try {
            if (val instanceof String) {
                return Integer.parseInt(val)
            }

            return val
        } catch (NumberFormatException nfe){
            log.debug("Integer conversion error - ${val}", nfe)
        }
    }

    // Implementation of Getter<String>
    @Override
    String getPropertyName() {
        getPropName()
    }

    @Override
    Number getFormattedValue(Object output) {
        try {
            return processNode(this, getDataNode(), getValue(output))
        }
        catch (Exception e) {
            OutputModelProcessor.log.error("Error getting value from output: ${output?.outputId}, property: ${getPropName()}", e)
        }
    }
}
