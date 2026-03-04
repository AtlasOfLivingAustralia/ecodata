package au.org.ala.ecodata.converter

import spock.lang.Specification

class SpeciesConverterSpec extends Specification {
    def "isRecordValid returns correct results for various input combinations"(Map record, boolean expected) {
        expect:
        SpeciesConverter.isRecordValid(record) == expected

        where:
        record                                                              | expected
        [scientificName: null, vernacularName: null]                        | false
        [scientificName: "", vernacularName: ""]                            | false
        [scientificName: "Acacia dealbata", vernacularName: null]           | true
        [scientificName: "", vernacularName: "Silver Wattle"]               | true
        [scientificName: "Eucalyptus globulus", vernacularName: "Blue Gum"]|| true
        [vernacularName: "Koala Tree"]                                      | true
        [:]                                                                 | false
        [scientificName: "Banksia", vernacularName: "", extraField: 123]    | true
        null                                                                | false
    }

    def "converter should sanitize scientificName and vernacularName"() {
        setup:
        Map data = [species: [scientificName: "<b>Acacia </b>dealbata", commonName: "<i>Silver Wattl</i>e", outputSpeciesId: "123"]]
        Map metadata = [name: "species"]

        when:
        List<Map> result = new SpeciesConverter().convert(data, metadata)

        then:
        result.size() == 1
        result[0].scientificName == "Acacia dealbata"
        result[0].vernacularName == "Silver Wattle"
    }

}
