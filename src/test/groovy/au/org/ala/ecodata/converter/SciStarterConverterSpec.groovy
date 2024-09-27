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


        Map props = [difficulty: [value: 2, label: 'Medium']]

        when:
        def result = SciStarterConverter.convert(props, [:])

        then:
        result.difficulty == "Medium"

        when:
        result = SciStarterConverter.convert([difficulty: [value: 1, label: 'Easy']])

        then:
        result.difficulty == "Easy"

        when:
        result = SciStarterConverter.convert([difficulty: null])

        then:
        result.difficulty == null
    }


}