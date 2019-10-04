package au.org.ala.ecodata.converter

//import org.grails.web.json.JSON

class SpeciesConverter implements RecordFieldConverter {

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.guid = data[metadata.name].guid
        record.name = data[metadata.name].name

        record.scientificName = data[metadata.name].name

        // Force outputSpeciesId generation if not coming in the original data
        if(!data[metadata.name].outputSpeciesId) {
            data[metadata.name].outputSpeciesId = UUID.randomUUID().toString()
        }

        record.outputSpeciesId = data[metadata.name].outputSpeciesId

        [record]
    }
}
