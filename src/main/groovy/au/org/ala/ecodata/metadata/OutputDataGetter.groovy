package au.org.ala.ecodata.metadata


import pl.touk.excel.export.getters.Getter

class OutputDataGetter extends OutputDataExtractor implements Getter<String> {
    OutputDataGetter(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone){
        super(propertyName, dataNode, documentMap, timeZone)
    }

    // Implementation of Getter<String>
    @Override
    String getPropertyName() {
        return getPropName()
    }

    @Override
    String getFormattedValue(Object output) {
        String result = ''
        try {
            result = processNode(this, getDataNode(), getValue(output))
        }
        catch (Exception e) {
            OutputModelProcessor.log.error("Error getting value from output: ${output?.outputId}, property: ${getPropName()}", e)
        }
        result
    }

}
