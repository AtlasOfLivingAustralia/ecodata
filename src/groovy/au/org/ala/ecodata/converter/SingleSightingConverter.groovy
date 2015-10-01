package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SingleSightingConverter implements RecordConverter {
    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        Map record = [:]

        record.decimalLatitude = Double.parseDouble(data.data.decimalLatitude)
        record.decimalLongitude = Double.parseDouble(data.data.decimalLongitude)
        record.eventDate = data.data.eventDate
        record.individualCount = Integer.parseInt(data.data.individualCount)
        record.userId = data.data.userId
        record.multimedia = data.data.multimedia
        record.name = data.data.scientificName
        record.guid = data.data.guid

        record.json = (data as JSON).toString()

        [record]
    }
}
