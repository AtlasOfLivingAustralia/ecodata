package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SingleSightingConverter implements RecordConverter {
    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        Map record = [:]

        if (data.data.decimalLatitude) {
            record.decimalLatitude = Double.parseDouble(data.data.decimalLatitude)
        } else if (data.data.locationLatitude) {
            record.decimalLatitude = Double.parseDouble(data.data.locationLatitude)
        }

        if (data.data.decimalLongitude) {
            record.decimalLongitude = Double.parseDouble(data.data.decimalLongitude)
        } else if (data.data.locationLongitude) {
            record.decimalLongitude = Double.parseDouble(data.data.locationLongitude)
        }
        
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
