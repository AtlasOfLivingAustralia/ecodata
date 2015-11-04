package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity

/**
 * Converts an Output's data model into one or more Records.
 *
 * To trigger a conversion, the data model in dataModel.json must have the 'record: true' attribute.
 *
 * Individual fields from the data can be mapped to specific Record attributes by adding a 'dwcAttribute' attribute to
 * an item in the data model. NOTE: the dwcAttribute value should be a standard Darwin Core Archive Term, even if the
 * target Record attribute is different (the converter is responsible for mapping one to the other).
 */
trait RecordConverter {
    String DWC_ATTRIBUTE_NAME = "dwcAttribute"

    abstract List<Map> convert(Activity activity, Map data)

    abstract List<Map> convert(Activity activity, Map data, Map outputMetadata)

    Map extractActivityDetails(Activity activity) {
        Map dwcFields = [:]
        dwcFields.userId = activity.userId
        dwcFields.recordedBy = activity.userId

        dwcFields
    }

    Double toDouble(val) {
        Double result = null
        if (val) {
            result = val instanceof Number ? val : Double.parseDouble(val.toString())
        }
        result
    }
}