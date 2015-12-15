package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SingleSightingConverter implements RecordFieldConverter {
    @Override
    List<Map> convert( Map data, Map outputMetadata = [:]) {
        Map record = [:]

        record.decimalLatitude = getLatitude(data)
        record.decimalLongitude = getLongitude(data)
        
        record.eventDate = data.eventDate
        record.individualCount = Integer.parseInt(data.individualCount)
        record.userId = data.userId
        record.multimedia = data.multimedia
        record.name = data.scientificName
        record.guid = data.guid

        record.json = (data as JSON).toString()

        [record]
    }

    private Double getLatitude(Map data) {
        Double lat = null

        if (data.decimalLatitude) {
            lat = toDouble(data.decimalLatitude)
        } else if (data.locationLatitude) {
            lat = toDouble(data.locationLatitude)
        }

        lat
    }

    private Double getLongitude(Map data) {
        Double lng = null

        if (data.decimalLongitude) {
            lng = toDouble(data.decimalLongitude)
        } else if (data.locationLongitude) {
            lng = toDouble(data.locationLongitude)
        }

        lng
    }
}
