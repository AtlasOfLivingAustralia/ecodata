package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SpeciesConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.guid = data[metadata.name].guid
        record.name = data[metadata.name].name

        // if there is a valid guid then pass on scientific name (if it is valid)
        if (record.guid && record.guid != "") {
          if (data[metadata.name].scientificName) {
              record.scientificName = data[metadata.name].scientificName
          }
        }

        // Force outputSpeciesId generation if not coming in the original data
        if(!data[metadata.name].outputSpeciesId) {
            data[metadata.name].outputSpeciesId = UUID.randomUUID().toString()
        }

        record.outputSpeciesId = data[metadata.name].outputSpeciesId

        [record]
    }
}
