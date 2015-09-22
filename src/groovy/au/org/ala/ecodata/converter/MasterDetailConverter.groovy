package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Record

class MasterDetailConverter implements RecordConverter {

    @Override
    List<Record> convert(Map data, Map outputMetadata = [:]) {
        // delegate the conversion to a specific converter for the DETAIL portion of the master/detail
        RecordConverter converter = RecordConverterFactory.getConverter(outputMetadata.detail.dataType)

        List<Record> records = []

        data.data[outputMetadata.name].each {
            records.addAll converter.convert([data: it], outputMetadata.detail as Map)
        }

        records
    }
}
