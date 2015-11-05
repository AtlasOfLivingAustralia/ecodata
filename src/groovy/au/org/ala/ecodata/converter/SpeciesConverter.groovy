package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Activity
import net.sf.json.JSON

class SpeciesConverter implements RecordConverter {

    List<Map> convert(Activity activity, Map data, Map metadata = [:]) {
        Map record = extractActivityDetails(activity)

        record.json = (data.data as JSON).toString()

        record.guid = data.data.value.guid
        record.name = data.data.value.name
        record.scientificName = data.data.value.name

        [record]
    }
}
