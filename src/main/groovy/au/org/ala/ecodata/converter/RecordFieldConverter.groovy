package au.org.ala.ecodata.converter

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.expression.Expression
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
/**
 * Converts an Output's data model into one or more Records.
 *
 * To trigger a conversion, the data model in dataModel.json must have the 'record: true' attribute.
 *
 * Individual fields from the data can be mapped to specific Record attributes by adding a 'dwcAttribute' attribute to
 * an item in the data model. NOTE: the dwcAttribute value should be a standard Darwin Core Archive Term, even if the
 * target Record attribute is different (the converter is responsible for mapping one to the other).
 */
trait RecordFieldConverter {
    static Log log = LogFactory.getLog(RecordFieldConverter.class)
    static String DWC_ATTRIBUTE_NAME = "dwcAttribute"
    static String DWC_EXPRESSION = "dwcExpression"
    static String DWC_DEFAULT = "dwcDefault"
    static String DWC_MEASUREMENT_VALUE = "measurementValue"
    static String DWC_MEASUREMENT_TYPE = "measurementType"
    static String DWC_MEASUREMENT_TYPE_ID = "measurementTypeID"
    static String DWC_MEASUREMENT_ACCURACY = "measurementAccuracy"
    static String DWC_MEASUREMENT_UNIT = "measurementUnit"
    static String DWC_MEASUREMENT_UNIT_ID = "measurementUnitID"
    static String PROP_MEASUREMENTS_OR_FACTS = "measurementsorfacts"

    abstract List<Map> convert(Map data)

    abstract List<Map> convert(Map data, Map outputMetadata)
    abstract List<Map> convert(Map data, Map outputMetadata, Map context)

    Double toDouble(val) {
        Double result = null
        if (val) {
            result = val instanceof Number ? val : Double.parseDouble(val.toString())
        }
        result
    }

    Map extractDwcMapping(Map dataModel) {
        Map dwcMappings = [:]

        if (dataModel?.containsKey(DWC_ATTRIBUTE_NAME)) {
            dwcMappings[dataModel.dwcAttribute] = dataModel.name
        }

        dwcMappings
    }

    // write test cases for this method

    Map getDwcAttributes(Map data, Map dwcMappings, Map metadata = null, Map context = [:]) {

        Map fields = [:]
        dwcMappings.each { dwcAttribute, fieldName ->
            if(metadata?.containsKey(DWC_EXPRESSION)) {
                data[fieldName] = evaluateExpression((String)metadata[DWC_EXPRESSION], data, metadata[DWC_DEFAULT], metadata, context)
            }

            fields[dwcAttribute] = data[fieldName]
        }

        if (dwcMappings.containsKey("species")) {
            fields.name = data[dwcMappings["species"]].name
            fields.scientificName = data[dwcMappings["species"]].scientificName
            fields.guid = data[dwcMappings["species"]].guid
        }

        // Add measurements of facts if metadata dwcAttribute value is 'measurementValue'.
        // It dwcAttribute measurementValue is taken from output data.
        // If measurementType is provided in metadata, its event core value is created by binding metadata value and
        // output data. Rest of the attributes are provided by metadata.
        String fieldName = dwcMappings[DWC_MEASUREMENT_VALUE]
        def value = data[fieldName]
        if (dwcMappings.containsKey(DWC_MEASUREMENT_VALUE) && ![null, ""].contains(value)) {
            if (!fields[PROP_MEASUREMENTS_OR_FACTS])
                fields[PROP_MEASUREMENTS_OR_FACTS] = []

            Map measurement = [:]
            measurement[DWC_MEASUREMENT_VALUE] = value
            measurement[DWC_MEASUREMENT_TYPE] = getMeasurementType(metadata, data, context)

            if (metadata?.containsKey(DWC_MEASUREMENT_TYPE_ID)) {
                measurement[DWC_MEASUREMENT_TYPE_ID] = metadata[DWC_MEASUREMENT_TYPE_ID]
            }

            if (metadata?.containsKey(DWC_MEASUREMENT_ACCURACY)) {
                measurement[DWC_MEASUREMENT_ACCURACY] = metadata[DWC_MEASUREMENT_ACCURACY]
            }

            if (metadata?.containsKey(DWC_MEASUREMENT_UNIT)) {
                measurement[DWC_MEASUREMENT_UNIT] = metadata[DWC_MEASUREMENT_UNIT]
            }

            if (metadata?.containsKey(DWC_MEASUREMENT_UNIT_ID)) {
                measurement[DWC_MEASUREMENT_UNIT_ID] = metadata[DWC_MEASUREMENT_UNIT_ID]
            }

            fields[PROP_MEASUREMENTS_OR_FACTS].add(measurement)
        }

        fields
    }

    /**
     * Create metadata type value for event core's measurement or fact table.
     * 1. If measurementType is provided by metadata i.e. dataModel, then bind output data to it. It uses groovy's SimpleTemplateEngine.
     * 2. Otherwise, uses one of description, value or name found in metadata.
     * @param metadata
     * @param data
     * @return
     */
    def getMeasurementType(Map metadata, Map data, Map context = [:]) {
        def defaultValue = metadata?.description ?: metadata?.value ?: metadata?.name
        if (metadata?.measurementType && data) {
            return evaluateExpression(metadata.measurementType, data, defaultValue, metadata, context)
        } else {
            return defaultValue
        }
    }

    def evaluateExpression (String expression, Map data = [:], def defaultValue, Map metadata = null, Map context = [:]) {
        try {
            context.metadata = metadata
            String returnType = metadata.returnType ?: "java.lang.String"
            Class returnClass = Class.forName(returnType)
            data.context = context
            ExpressionParser expressionParser = new SpelExpressionParser()
            Expression expObject = expressionParser.parseExpression(expression)
            def value = expObject.getValue(data, returnClass)
            data.remove('context')
            return value
        }
        catch (Exception ex) {
            log.error(ex.message, ex)
            data.remove('context')
            return defaultValue == null ? expression : defaultValue
        }
    }

}