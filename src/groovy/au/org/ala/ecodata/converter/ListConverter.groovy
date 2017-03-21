package au.org.ala.ecodata.converter

import groovy.util.logging.Log4j


@Log4j
class ListConverter implements RecordFieldConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        List<Map> records = []
        int index = 0

        // delegate the conversion of each column in each row to a specific converter for the column type
        data[outputMetadata.name].each { row ->
            Map baseRecord = [:]

            //Use the same approach as au.org.ala.ecodata.converter.RecordConverter.convertRecords() to convert
            // singleItemModels, ie, iterate over the datatype definitions rather than the values (data)
            // It is important to capture null values that will  override values from the baseRecord again in
            // au.org.ala.ecodata.converter.RecordConverter.convertRecords()


            // Split data in fields that constitute the base record and the different species field
            // Each species field will end up generating a record entry.
            // All records will share the same base record data
            List baseRecordModels
            List speciesModels
            (baseRecordModels, speciesModels) = outputMetadata?.columns?.split {
                it.dataType.toLowerCase() != "species"
            }


            // For each singleItemModel, get the appropriate field converter for the data type, generate the individual
            // Record fields and add them to the skeleton Record
            baseRecordModels?.each { Map dataModel ->
                RecordFieldConverter converter = RecordConverter.getFieldConverter(dataModel.dataType)
                List<Map> recordFieldSets = converter.convert(row, dataModel)
                baseRecord << recordFieldSets[0]
            }

            // For each species dataType, where present we will generate a new record
            speciesModels?.each { Map dataModel ->
                RecordFieldConverter converter = RecordConverter.getFieldConverter(dataModel.dataType)
                List<Map> recordFieldSets = converter.convert(row, dataModel)
                Map speciesRecord = RecordConverter.overrideFieldValues(baseRecord, recordFieldSets[0])

                // We want to create a record in the DB only if species information is present
                if(speciesRecord.outputSpeciesId) {
                    speciesRecord.outputItemId = index++
                    records << speciesRecord
                } else {
                    log.warn("Record [${speciesRecord}] does not contain full species information. " +
                            "This is most likely a bug.")
                }
            }
        }

        records
    }
}
