package au.org.ala.ecodata.converter

import groovy.util.logging.Log4j
import org.apache.commons.lang.StringUtils

@Log4j
class RecordConverterFactory {

    static RecordConverter getConverter(String outputDataType) {
        String packageName = RecordConverter.class.package.getName()
        String className = "${StringUtils.capitalize(outputDataType).replaceAll("[ _\\-]", "")}Converter"

        RecordConverter converter
        try {
            converter = Class.forName("${packageName}.${className}")?.newInstance()
        } catch (ClassNotFoundException e) {
            log.debug "No specific converter found for output data type ${outputDataType} with class name ${packageName}.${className}, using generic converter"
            converter = new GenericConverter()
        }

        converter
    }
}
