package au.org.ala.ecodata.converter

class GenericFieldConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]


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
        Double latitude = null

        if (data.decimalLatitude) {
            latitude = toDouble(data.decimalLatitude)
        } else if (data.locationLatitude) {
            latitude = toDouble(data.locationLatitude)
        }

        latitude
    }

    private Double getLongitude(Map data) {
        Double longitude = null

        if (data.decimalLongitude) {
            longitude = toDouble(data.decimalLongitude)
        } else if (data.locationLongitude) {
            longitude = toDouble(data.locationLongitude)
        }

        longitude
    }
}
