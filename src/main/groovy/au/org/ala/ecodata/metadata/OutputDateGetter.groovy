package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

class OutputDateGetter  extends OutputDataExtractor implements Getter<Date> {
    OutputDateGetter(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone){
        super(propertyName, dataNode, documentMap, timeZone)
    }

    @Override
    def date(Object node, Value outputValue) {
        def val = outputValue.value
        if (!val)
            return null
        def date = getDateTimeParser().parse(val)
        return date ? date?.getTime() : null
    }


    // Implementation of Getter<String>
    @Override
    String getPropertyName() {
        getPropName()
    }

    @Override
    Date getFormattedValue(Object output) {
        try {
            if(getDataNode().dataType in ['date', 'simpleDate']) {
                return date(getDataNode(), getValue(output))
            }
        }
        catch (Exception e) {
            OutputModelProcessor.log.error("Error getting value from output: ${output?.outputId}, property: ${getPropName()}", e)
        }
    }

}
