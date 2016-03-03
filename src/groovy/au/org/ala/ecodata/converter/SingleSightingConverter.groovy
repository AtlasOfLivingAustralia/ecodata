package au.org.ala.ecodata.converter

import net.sf.json.JSON

class SingleSightingConverter implements RecordFieldConverter {
    @Override
    List<Map> convert( Map data, Map outputMetadata = [:]) {
        Map record = [:]


        record.eventDate = data.eventDate
        record.individualCount = Integer.parseInt(data.individualCount)
        record.userId = data.userId
        record.multimedia = data.multimedia
        record.name = data.scientificName
        record.guid = data.guid
        record.outputSpeciesId = data.outputSpeciesId

        record.json = (data as JSON).toString()

        [record]
    }
}
