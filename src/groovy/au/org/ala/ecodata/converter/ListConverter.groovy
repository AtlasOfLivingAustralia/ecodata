package au.org.ala.ecodata.converter

import net.sf.json.JSON

class ListConverter implements RecordFieldConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        List<Map> records = []

        // delegate the conversion of each column in each row to a specific converter for the column type
        data[outputMetadata.name].eachWithIndex { row, index ->
            Map record = [:]
            row.each { col ->
                Map columnMetadata = outputMetadata.columns.find { colDef -> colDef.name == col.key }
                RecordFieldConverter converter = RecordConverter.getFieldConverter(columnMetadata.dataType)
                List<Map> fields = converter.convert([(col.key): col.value], columnMetadata as Map)
                fields?.each { record << it }
            }

            record.json = (row as JSON).toString()
            record.outputItemId = index

            records << record
        }

        records
    }
}
