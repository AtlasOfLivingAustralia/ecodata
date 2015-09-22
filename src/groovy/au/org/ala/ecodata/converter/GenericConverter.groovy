package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Record
import net.sf.json.JSON

class GenericConverter implements RecordConverter {

    List<Record> convert(Map data, Map metadata = [:]) {
        [new Record(json: (data as JSON).toString())]
    }
}
