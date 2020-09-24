package au.org.ala.ecodata.converter

import grails.util.Holders
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification


class SciStarterConverterSpec extends Specification implements GrailsUnitTest {

    def "difficulty parameter should ignore mixed content in list"() {
        setup:
        config.merge([
                scistarter: [forceHttpsUrls: false, baseUrl: "https://scistarer.org" ],
                countries:[], uNRegions: [], biocollect: [ scienceType: [] ]])


        Map props = [difficulty: [1,2, 3, 'ma', 'medium', 'Easy']]

        when:
        def result = SciStarterConverter.convert(props, [:])

        then:
        result.difficulty == "Medium"

        when:
        result = SciStarterConverter.convert([difficulty: 'Easy'])

        then:
        result.difficulty == "Easy"

        when:
        result = SciStarterConverter.convert([difficulty: null])

        then:
        result.difficulty == null
    }


}