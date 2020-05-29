package au.org.ala.ecodata.converter

import grails.util.Holders
import net.sf.json.groovy.JsonSlurper
import spock.lang.Specification
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

class SciStarterConverterSpec extends Specification {
    def "difficulty parameter should ignore mixed content in list"() {
        setup:
        Holders.grailsApplication = new DefaultGrailsApplication()
        Holders.grailsApplication.setConfig(new ConfigObject())
        Holders.grailsApplication.config = [
                scistarter: [forceHttpsUrls: false, baseUrl: "https://scistarer.org" ],
                countries:[], uNRegions: [], biocollect: [ scienceType: [] ]
        ]

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