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

}
