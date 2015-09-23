package au.org.ala.ecodata.converter

class MasterDetailConverter implements RecordConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        // delegate the conversion to a specific converter for the DETAIL portion of the master/detail
        RecordConverter converter = RecordConverterFactory.getConverter(outputMetadata.detail.dataType)

        List<Map> records = []

        data.data[outputMetadata.name].each {
            records.addAll converter.convert([data: it], outputMetadata.detail as Map)
        }

        records
    }
}
