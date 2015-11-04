package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity

class MasterDetailConverter implements RecordConverter {

    @Override
    List<Map> convert(Activity activity, Map data, Map outputMetadata = [:]) {
        // delegate the conversion to a specific converter for the DETAIL portion of the master/detail
        RecordConverter converter = RecordConverterFactory.getConverter(outputMetadata.detail.dataType)

        List<Map> records = []

        data.data[outputMetadata.name].each {
            records.addAll converter.convert(activity, [data: it], outputMetadata.detail as Map)
        }

        records
    }
}
