package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest


/**
 * Created by sat01a on 14/07/16.
 */
class RecordServiceSpec extends MongoSpec implements ServiceUnitTest<RecordService> {
    def recordService = new RecordService()

    void "test species name with display format - SCIENTIFICNAME(COMMONNAME) with scientific name in brackets"() {
        when:
        def pActivity = [species:[speciesDisplayFormat:'SCIENTIFICNAME(COMMONNAME)']]
        def record = [name:'Conostylis seorsiflora subsp. Nyabing (A. Coates s.n. 2/10/1988) (Ear rot)']
        String name = recordService.getSpeciesName(record, pActivity)
        then:
        name == 'Conostylis seorsiflora subsp. Nyabing (A. Coates s.n. 2/10/1988)'
    }

    void "test species name with display format - SCIENTIFICNAME(COMMONNAME)"() {
        when:
        def pActivity = [species:[speciesDisplayFormat:'SCIENTIFICNAME(COMMONNAME)']]
        def record = [name:'Conostylis seorsiflora subsp. Nyabing  (Ear rot)']
        String name = recordService.getSpeciesName(record, pActivity)
        then:
        name == 'Conostylis seorsiflora subsp. Nyabing'
    }

    void "test species name with display format - COMMONNAME(SCIENTIFICNAME)"() {
        when:
        def pActivity = [species:[speciesDisplayFormat:'COMMONNAME(SCIENTIFICNAME)']]
        def record = [name:'Ear rot (Colubotelson searli)']
        String name = recordService.getSpeciesName(record, pActivity)
        then:
        name == 'Ear rot'
    }

    void "test species name with display format - SCIENTIFICNAME"() {
        when:
        def pActivity = [species:[speciesDisplayFormat:'SCIENTIFICNAME']]
        def record = [name:'Colubotelson searli']
        String name = recordService.getSpeciesName(record, pActivity)
        then:
        name == 'Colubotelson searli'
    }

    void "test species name with display format - COMMONNAME"() {
        when:
        def pActivity = [species:[speciesDisplayFormat:'COMMONNAME']]
        def record = [name:'Ear rot']
        String name = recordService.getSpeciesName(record, pActivity)
        then:
        name == 'Ear rot'
    }

    void "The toMap service converts a Record to a Map"() {
        setup:
        String prefix = grailsApplication.config.getProperty("biocollect.activity.sightingsUrl")

        when:
        Record r = new Record(outputId:'r1', activityId:'a1')
        Map recordMap = service.toMap(r)

        then:
        recordMap instanceof Map
        recordMap.recordNumber == "${prefix}/bioActivity/index/a1"
    }

    def "formatTaxonName should format name based on displayType"() {
        setup:
        Map data = [commonName: commonName, scientificName: scientificName]

        when:
        String result = service.formatTaxonName(data, displayType)

        then:
        result == expectedName

        where:
        commonName         | scientificName       | displayType                         | expectedName
        'Blackbird'        | 'Turdus merula'      | service.COMMON_NAME_SCIENTIFIC_NAME       | 'Blackbird (Turdus merula)'
        'Blackbird'        | 'Turdus merula'      | service.SCIENTIFIC_NAME_COMMON_NAME       | 'Turdus merula (Blackbird)'
        'Blackbird'        | 'Turdus merula'      | service.COMMON_NAME                       | 'Blackbird'
        null               | 'Turdus merula'      | service.COMMON_NAME                       | 'Turdus merula'
        'Blackbird'        | 'Turdus merula'      | service.SCIENTIFIC_NAME                   | 'Turdus merula'
        null               | 'Turdus merula'      | service.SCIENTIFIC_NAME                   | 'Turdus merula'
        null               | 'Turdus merula'      | service.COMMON_NAME_SCIENTIFIC_NAME       | 'Turdus merula'
        null               | 'Turdus merula'      | service.SCIENTIFIC_NAME_COMMON_NAME       | 'Turdus merula'
        'Blackbird'        | null                 | service.COMMON_NAME_SCIENTIFIC_NAME       | 'Blackbird'
        'Blackbird'        | null                 | service.SCIENTIFIC_NAME_COMMON_NAME       | 'Blackbird'
        'Blackbird'        | null                 | service.COMMON_NAME                       | 'Blackbird'
        null               | null                 | service.COMMON_NAME                       | ''
        null               | null                 | service.SCIENTIFIC_NAME                   | ''
        null               | null                 | service.COMMON_NAME_SCIENTIFIC_NAME       | ''
        null               | null                 | service.SCIENTIFIC_NAME_COMMON_NAME       | ''
    }

}

