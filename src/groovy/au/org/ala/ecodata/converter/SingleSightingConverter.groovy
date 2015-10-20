package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SingleSightingConverter implements RecordConverter {
    @Override
    List<Map> convert(Map data, Map outputMetadata = [:]) {
        Map record = [:]

        record.decimalLatitude = getLatitude(data)

        record.decimalLongitude = getLongitude(data)
        
        record.eventDate = data.data.eventDate
        record.individualCount = Integer.parseInt(data.data.individualCount)
        record.userId = data.data.userId
        record.multimedia = data.data.multimedia
        record.name = data.data.scientificName
        record.guid = data.data.guid

        record.json = (data as JSON).toString()

        [record]
    }

    private static Double getLatitude(Map data) {
        Double lat = null

        if (data.data.decimalLatitude) {
            lat = toDouble(data.data.decimalLatitude)
        } else if (data.data.locationLatitude) {
            lat = toDouble(data.data.locationLatitude)
        }

        lat
    }

    private static Double getLongitude(Map data) {
        Double lng = null

        if (data.data.decimalLongitude) {
            lng = toDouble(data.data.decimalLongitude)
        } else if (data.data.locationLongitude) {
            lng = toDouble(data.data.locationLongitude)
        }

        lng
    }

    private static Double toDouble(val) {
        if (!val) {
            null
        } else {
            val instanceof Number ? val : Double.parseDouble(val.toString())
        }
    }
}
