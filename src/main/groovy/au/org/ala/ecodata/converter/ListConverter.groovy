package au.org.ala.ecodata.converter

import groovy.util.logging.Slf4j


@Slf4j
class ListConverter implements RecordFieldConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        List<Map> records = []
        int index = 0

        // delegate the conversion of each column in each row to a specific converter for the column type
        data[outputMetadata.name].each { row ->
            if (row == null) {
                return
            }

            Map baseRecord = [:]
            List singleItemModels
            List multiItemModels
            (singleItemModels, multiItemModels) = outputMetadata?.columns?.split {
                //check if dataType is null
                !RecordConverter.MULTI_ITEM_DATA_TYPES.contains(it.dataType?.toLowerCase())
            }

            //Use the same approach as au.org.ala.ecodata.converter.RecordConverter.convertRecords() to convert
            // singleItemModels, ie, iterate over the datatype definitions rather than the values (data)
            // It is important to capture null values that will  override values from the baseRecord again in
            // au.org.ala.ecodata.converter.RecordConverter.convertRecords()


            // Split data in fields that constitute the base record and the different species field
            // Each species field will end up generating a record entry.
            // All records will share the same base record data
            List baseRecordModels
            List speciesModels
            (baseRecordModels, speciesModels) = singleItemModels?.split {
                it.dataType.toLowerCase() != "species"
            }


            // For each singleItemModel, get the appropriate field converter for the data type, generate the individual
            // Record fields and add them to the skeleton Record
            baseRecordModels?.each { Map dataModel ->
                RecordFieldConverter converter = RecordConverter.getFieldConverter(dataModel.dataType)
                List<Map> recordFieldSets = converter.convert(row, dataModel)

                Map recordFieldSet = recordFieldSets[0]
                baseRecord = RecordConverter.overrideAllExceptLists(baseRecord, recordFieldSet)
                RecordConverter.updateEventIdToMeasurements(baseRecord[PROP_MEASUREMENTS_OR_FACTS], baseRecord.activityId)

// TODO: delete? commented code was in dev branch, removed on merge.
//                if (recordFieldSets[0])
//                    baseRecord << recordFieldSets[0]
            }

            // For each species dataType, where present we will generate a new record
            speciesModels?.each { Map dataModel ->
                RecordFieldConverter converter = RecordConverter.getFieldConverter(dataModel.dataType)
                List<Map> recordFieldSets = converter.convert(row, dataModel)
                if (recordFieldSets) {
                    Map speciesRecord = RecordConverter.overrideFieldValues(baseRecord, recordFieldSets[0])
                    // We want to create a record in the DB only if species information is present
                    if (speciesRecord.outputSpeciesId) {
                        speciesRecord.outputItemId = index++
                        RecordConverter.updateSpeciesIdToMeasurements(speciesRecord[PROP_MEASUREMENTS_OR_FACTS], speciesRecord.outputSpeciesId)
                        records << speciesRecord
                    } else {
                        log.warn("Record [${speciesRecord}] does not contain full species information. " +
                                "This is most likely a bug.")
                    }
                }
            }

            if (multiItemModels) {
                // For each multiItemModel, get the appropriate field converter for the data type and generate the list of field
                // sets which will be converted into Records. For each field set, add a copy of the skeleton Record so it has
                // all the common fields
                multiItemModels?.each { Map dataModel ->
                    RecordFieldConverter converter = RecordConverter.getFieldConverter(dataModel.dataType)
                    List<Map> recordFieldSets = converter.convert(row, dataModel)

                    recordFieldSets.each {
                        Map rowRecord = RecordConverter.overrideFieldValues(baseRecord, it)
                        if(rowRecord.guid && rowRecord.guid != "") {
                            records << rowRecord
                        } else {
                            log.warn("Multi item Record [${rowRecord}] does not contain species information, " +
                                    "was the form intended to work like that?")
                        }
                    }
                }
            }

            if (!speciesModels) {
                records << baseRecord
            }
        }

        records
    }
}
