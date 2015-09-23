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
interface RecordConverter {
    String DWC_ATTRIBUTE_NAME = "dwcAttribute"

    List<Map> convert(Map data)

    List<Map> convert(Map data, Map outputMetadata)
}