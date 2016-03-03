package au.org.ala.ecodata.converter

import net.sf.json.JSON

class GenericFieldConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.json = (data as JSON).toString()

        Double latitude = getLatitude(data)
        Double longitude = getLongitude(data)

        // Don't override decimalLongitud or decimalLatitude in case they are null, site info could've already set them
        if(latitude) {
            record.decimalLatitude = latitude
        }

        if(longitude) {
            record.decimalLongitude = longitude
        }

        Map dwcMappings = extractDwcMapping(metadata)

        record << getDwcAttributes(data, dwcMappings)

        if (data.dwcAttribute) {
            record[data.dwcAttribute] = data.value
        }

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
