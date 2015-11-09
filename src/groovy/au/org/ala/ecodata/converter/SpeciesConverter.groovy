package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SpeciesConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.json = (data.data as JSON).toString()

        record.guid = data.species.guid
        record.name = data.species.name
        record.scientificName = data.species.name

        [record]
    }
}
