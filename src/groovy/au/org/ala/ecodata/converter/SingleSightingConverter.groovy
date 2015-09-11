package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Record
import net.sf.json.JSON

class SingleSightingConverter implements RecordConverter {
    @Override
    List<Record> convert(Map data, Map outputMetadata = [:]) {
        Record record = new Record()

        record.decimalLatitude = Double.parseDouble(data.data.decimalLatitude)
        record.decimalLongitude = Double.parseDouble(data.data.decimalLongitude)
        record.eventDate = data.data.eventDate
        record.individualCount = Integer.parseInt(data.data.individualCount)
        record.userId = data.data.userId

        record.json = (data as JSON).toString()

        [record]
    }
}
