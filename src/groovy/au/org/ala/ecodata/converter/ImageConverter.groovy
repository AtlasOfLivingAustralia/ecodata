package au.org.ala.ecodata.converter

class ImageConverter implements RecordFieldConverter {

    private static final String DEFAULT_RIGHTS_STATEMENT = "The rights to all uploaded images are held equally, under a Creative Commons Attribution (CC-BY v3.0) license, by the contributor of the image and the primary organisation responsible for the project to which they are contributed."
    private static final String DEFAULT_LICENCE = "Creative Commons Attribution"

    List<Map> convert(Map data, Map metadata = [:]) {
        Map record = [:]

        record.multimedia = data[metadata.name]
        record.multimedia?.each {
            if (it instanceof Map) {
                it.identifier = it.identifier ?: it.url
                it.creator = it.creator ?: it.attribution
                it.title = it.title ?: it.filename
                it.type = it.type ?: it.contentType
                it.rightsHolder = it.creator ?: it.attribution
                it.rights = DEFAULT_RIGHTS_STATEMENT
                // note: the US spelling of licenSe is expected for the dublin core standard, so don't fix it
                it.license = DEFAULT_LICENCE
            }
        }

        [record]
    }
}
