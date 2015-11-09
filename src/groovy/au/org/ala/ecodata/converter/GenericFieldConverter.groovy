package au.org.ala.ecodata.converter

import net.sf.json.JSON

class GenericFieldConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.json = (data as JSON).toString()

        Map dwcMappings = extractDwcMapping(metadata)

        record << getDwcAttributes(data, dwcMappings)

        if (data.dwcAttribute) {
            record[data.dwcAttribute] = data.value
        }

        [record]
    }
}
