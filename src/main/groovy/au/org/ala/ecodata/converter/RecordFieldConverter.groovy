package au.org.ala.ecodata.converter

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
    String DWC_ATTRIBUTE_NAME = "dwcAttribute"

    abstract List<Map> convert(Map data)

    abstract List<Map> convert(Map data, Map outputMetadata)

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

    Map getDwcAttributes(Map dataModel, Map dwcMappings) {
        Map fields = [:]
        dwcMappings.each { dwcAttribute, fieldName ->
            fields[dwcAttribute] = dataModel[fieldName]
        }

        if (dwcMappings.containsKey("species")){
            fields.name = dataModel[dwcMappings["species"]].name
            fields.scientificName = dataModel[dwcMappings["species"]].name
            fields.guid = dataModel[dwcMappings["species"]].guid
        }

        fields
    }
}