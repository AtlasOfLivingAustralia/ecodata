package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SpeciesConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.json = (data.data as JSON).toString()

        record.guid = data[metadata.name].guid
        record.name = data[metadata.name].name
        record.outputSpeciesId = data[metadata.name].outputSpeciesId
        record.scientificName = data[metadata.name].name

        [record]
    }
}
