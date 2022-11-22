package au.org.ala.ecodata.converter

class SpeciesConverter implements RecordFieldConverter {
    List<String> REPLACE_PATTERN = [
            "(Unmatched taxon)"
    ]

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.scientificNameID = record.guid = data[metadata.name].guid
        record.scientificName = getScientificName(data, metadata)
        record.vernacularName = data[metadata.name].commonName
        record.name = record.scientificName ?: record.vernacularName

        // Force outputSpeciesId generation if not coming in the original data
        if(!data[metadata.name].outputSpeciesId) {
            data[metadata.name].outputSpeciesId = UUID.randomUUID().toString()
        }

        record.outputSpeciesId = data[metadata.name].outputSpeciesId

        [record]
    }

    /**
     * Get scientific name based on following conditions
     * 1. get value of scientificName property
     * 2. get value of name property
     * @param data
     * @param metadata
     * @return
     */
    String getScientificName(Map data, Map metadata) {
        data[metadata.name].scientificName ?: cleanName(data[metadata.name].name)
    }

    String cleanName (String name) {
        REPLACE_PATTERN.each {
            name = name?.replaceAll(it, "")
        }

        name?.trim()
    }
}
