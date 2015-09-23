package au.org.ala.ecodata.converter

import net.sf.json.JSON

class GenericConverter implements RecordConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        [[json: (data.data as JSON).toString(), userId: data.data.userId]]
    }
}
