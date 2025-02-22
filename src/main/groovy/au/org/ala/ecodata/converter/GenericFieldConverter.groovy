package au.org.ala.ecodata.converter

class GenericFieldConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:], Map context = [:]) {
        Map record = [:]


        Double latitude = getLatitude(data)
        Double longitude = getLongitude(data)

        // Don't override decimalLongitude or decimalLatitude in case they are null, site info could've already set them
        if(latitude) {
            record.decimalLatitude = latitude
        }

        if(longitude) {
            record.decimalLongitude = longitude
        }


        Map dwcMappings = extractDwcMapping(metadata)
        context.record = record
        Map dwcAttributes = getDwcAttributes(data, dwcMappings, metadata, context)
        record = RecordConverter.overrideAllExceptLists(dwcAttributes, record)

        if (data?.dwcAttribute) {
            record[data.dwcAttribute] = data.value
        }

        [record]
    }


    private Double getLatitude(Map data) {
        Double latitude = null

        if (data?.decimalLatitude) {
            latitude = toDouble(data.decimalLatitude)
        } else if (data?.locationLatitude) {
            latitude = toDouble(data.locationLatitude)
        }

        latitude
    }

    private Double getLongitude(Map data) {
        Double longitude = null

        if (data?.decimalLongitude) {
            longitude = toDouble(data.decimalLongitude)
        } else if (data?.locationLongitude) {
            longitude = toDouble(data.locationLongitude)
        }

        longitude
    }
}
