package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.JSON

class SingleSightingConverter implements RecordConverter {
    @Override
    List<Map> convert(Activity activity, Map data, Map outputMetadata = [:]) {
        Map record = extractActivityDetails(activity)

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

    private Double getLatitude(Map data) {
        Double lat = null

        if (data.data.decimalLatitude) {
            lat = toDouble(data.data.decimalLatitude)
        } else if (data.data.locationLatitude) {
            lat = toDouble(data.data.locationLatitude)
        }

        lat
    }

    private Double getLongitude(Map data) {
        Double lng = null

        if (data.data.decimalLongitude) {
            lng = toDouble(data.data.decimalLongitude)
        } else if (data.data.locationLongitude) {
            lng = toDouble(data.data.locationLongitude)
        }

        lng
    }
}
