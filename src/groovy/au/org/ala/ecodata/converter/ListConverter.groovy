package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.JSON

class ListConverter implements RecordConverter {

    @Override
    List<Map> convert(Activity activity, Map data, Map outputMetadata = [:]) {
//        List<Map> records = []
//
//        Map dwcMappings = [:]
//        outputMetadata.columns.each {
//            dwcMappings << extractDwcMapping(it)
//        }
//
//        data.data[outputMetadata.name].eachWithIndex { it, index ->
//            Map record = extractActivityDetails(activity)
//
//            record << getDwcAttributes(it, dwcMappings)
//
//            record.json = (it as JSON).toString()
//
//            record.outputItemId = index
//
//            records << record
//        }


        // delegate the conversion of each column to a specific converter for the column type

        List<Map> records = []

        data[outputMetadata.name].eachWithIndex { row, index ->
            Map record = [:]
            row.each { col ->
                Map columnMetadata = outputMetadata.columns.find { colDef -> colDef.name == col.key }
                RecordConverter converter = RecordConverterFactory.getConverter(columnMetadata.dataType)
                List<Map> fields = converter.convert(activity, [data: col], columnMetadata as Map)
                fields?.each { record << col }
            }

            record.json = (row as JSON).toString()
            record.outputItemId = index

            records << record
        }

        records
    }
}
