package au.org.ala.ecodata.converter

class FeatureConverter  implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]
        if (data[metadata.name]) {
            Double latitude = getDecimalLatitude(data[metadata.name])
            Double longitude = getDecimalLongitude(data[metadata.name])

            // Don't override decimalLongitud or decimalLatitude in case they are null, site info could've already set them
            if (latitude != null) {
                record.decimalLatitude = latitude
            }

            if (longitude != null) {
                record.decimalLongitude = longitude
            }


            Map dwcMappings = extractDwcMapping(metadata)

            record << getDwcAttributes(data, dwcMappings)

            if (data.dwcAttribute) {
                record[data.dwcAttribute] = data.value
            }
        }

        [record]
    }

    static Double getDecimalLatitude (Map data) {
        switch (data?.type) {
            case 'Point':
                return data.coordinates[1]
        }
    }

    static Double getDecimalLongitude (Map data) {
        switch (data?.type) {
            case 'Point':
                return data.coordinates[0]
        }

    }
}
