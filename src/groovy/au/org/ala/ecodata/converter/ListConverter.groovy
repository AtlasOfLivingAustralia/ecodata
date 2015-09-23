package au.org.ala.ecodata.converter

import net.sf.json.JSON

class ListConverter implements RecordConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        List<Map> records = []

        Map dwcMappings = [:]
        outputMetadata.columns.each {
            if (it.containsKey(DWC_ATTRIBUTE_NAME)) {
                dwcMappings[it.dwcAttribute] = it.name
            }
        }

        data.data[outputMetadata.name].eachWithIndex { it, index ->
            Map record = [:]
            record.json = (it as JSON).toString()

            if (dwcMappings.containsKey("individualCount")) {
                record.individualCount = Integer.parseInt(it[dwcMappings["individualCount"]])
            }

            if (dwcMappings.containsKey("decimalLatitude")) {
                record.decimalLatitude = Double.parseDouble(it[dwcMappings["decimalLatitude"]])
            }
            if (dwcMappings.containsKey("decimalLongitude")) {
                record.decimalLongitude = Double.parseDouble(it[dwcMappings["decimalLongitude"]])
            }

            if (dwcMappings.containsKey("creator")) {
                record.userId = it[dwcMappings["creator"]]
            }

            record.outputItemId = index

            records << record
        }

        records
    }
}
