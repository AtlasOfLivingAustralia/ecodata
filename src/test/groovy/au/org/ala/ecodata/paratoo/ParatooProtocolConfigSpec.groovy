package au.org.ala.ecodata.paratoo

import groovy.json.JsonSlurper
import spock.lang.Specification

class ParatooProtocolConfigSpec extends Specification {

    private Map readSurveyData(String name) {
        URL url = getClass().getResource("/paratoo/${name}.json")
        new JsonSlurper().parse(url)
    }

    def "The vegetation-mapping-survey can be used with this config"() {

        setup:
        Map surveyData = readSurveyData('vegetationMappingSurvey')
        Map vegetationMappingConfig = [
                apiEndpoint:'vegetation-mapping-observations',
                usesPlotLayout:false,
                geometryType:'Point',
                geometryPath:'attributes.position',
                startDatePath:'attributes.vegetation_mapping_survey.data.attributes.start_date_time',
                endDatePath: null
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(vegetationMappingConfig)

        expect:
        config.getStartDate(surveyData) == '2023-09-08T23:39:00.520Z'
        config.getEndDate(surveyData) == null
        config.getGeometry(surveyData) == [type:'Point', coordinates:[ 149.0651536, -35.2592398]]
    }
}
