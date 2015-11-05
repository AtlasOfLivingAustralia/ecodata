package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.JSON

class GenericConverter implements RecordConverter {

    List<Map> convert(Activity activity, Map data, Map metadata = [:]) {
        Map record = extractActivityDetails(activity)

        record.json = (data as JSON).toString()

        Map dwcMappings = extractDwcMapping(metadata)

        record << getDwcAttributes(data, dwcMappings)

        if (data.dwcAttribute) {
            record[data.dwcAttribute] = data.value
        }

        [record]
    }
}
