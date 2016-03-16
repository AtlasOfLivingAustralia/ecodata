package au.org.ala.ecodata.converter

import net.sf.json.JSON

class ListConverter implements RecordFieldConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        List<Map> records = []

        // delegate the conversion of each column in each row to a specific converter for the column type
        data[outputMetadata.name].eachWithIndex { row, index ->
            Map record = [:]

            //Use the same approach as au.org.ala.ecodata.converter.RecordConverter.convertRecords() to convert
            // singleItemModels, ie, iterate over the datatype definitions rather than the values (data)
            // It is important to capture null values that will  override values from the baseRecord again in
            // au.org.ala.ecodata.converter.RecordConverter.convertRecords()
            outputMetadata?.columns?.each { Map dataModel ->
                RecordFieldConverter converter = RecordConverter.getFieldConverter(dataModel.dataType)
                List<Map> recordFieldSets = converter.convert(row, dataModel)
                record << recordFieldSets[0]
            }

            record.outputItemId = index

            records << record
        }

        records
    }
}
