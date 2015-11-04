package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.JSON

class ListConverter implements RecordConverter {

    @Override
    List<Map> convert(Activity activity, Map data, Map outputMetadata = [:]) {
        List<Map> records = []

        Map dwcMappings = [:]
        outputMetadata.columns.each {
            if (it.containsKey(DWC_ATTRIBUTE_NAME)) {
                dwcMappings[it.dwcAttribute] = it.name
            }
        }

        data.data[outputMetadata.name].eachWithIndex { it, index ->
            Map record = extractActivityDetails(activity)
            record.json = (it as JSON).toString()

            dwcMappings.each { dwcAttribute, fieldName ->
                record[dwcAttribute] = it[fieldName]
            }

            if(dwcMappings.containsKey("species")){
                record.name = it[dwcMappings["species"]].name
                record.guid = it[dwcMappings["species"]].guid
            }

            record.outputItemId = index

            records << record
        }

        records
    }
}
