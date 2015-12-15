package au.org.ala.ecodata.converter

class MasterDetailConverter implements RecordFieldConverter {

    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        // delegate the conversion to a specific converter for the DETAIL portion of the master/detail
        RecordFieldConverter converter = RecordConverter.getFieldConverter(outputMetadata.detail.dataType)

        List<Map> records = []

        data[outputMetadata.name].each {
            records.addAll converter.convert(it, outputMetadata.detail as Map)
        }

        records
    }
}
