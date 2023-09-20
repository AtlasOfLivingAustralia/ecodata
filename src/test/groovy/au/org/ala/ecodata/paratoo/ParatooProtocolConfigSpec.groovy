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
        config.getStartDate(surveyData) == '2023-09-08T23:39:00Z'
        config.getEndDate(surveyData) == null
        config.getGeoJson(surveyData) == [type:'Point', coordinates:[149.0651536, -35.2592398]]
    }

    def "The floristics-standard survey can be used with this config"() {
        setup:
        Map surveyData = readSurveyData('floristicsStandard')
        Map floristicsSurveyConfig = [
                apiEndpoint:'floristics-veg-survey-lites',
                usesPlotLayout:true,
                startDatePath: 'attributes.start_date_time',
                endDatePath: 'attributes.end_date_time',
                surveyIdPath: 'attributes.surveyId'
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(floristicsSurveyConfig)

        expect:
        config.getStartDate(surveyData) == '2022-09-21T01:55:44Z'
        config.getEndDate(surveyData) == "2022-09-21T01:55:44Z"
        config.getGeoJson(surveyData) == [type:"Feature", geometry:[type:"Polygon", coordinates:[[[152.880694, -27.388252], [152.880651, -27.388336], [152.880518, -27.388483], [152.880389, -27.388611], [152.88028, -27.388749], [152.880154, -27.388903], [152.880835, -27.389463], [152.880644, -27.389366], [152.880525, -27.389248], [152.88035, -27.389158], [152.880195, -27.389021], [152.880195, -27.389373], [152.880797, -27.388316], [152.881448, -27.388909], [152.881503, -27.388821], [152.881422, -27.388766], [152.881263, -27.388644], [152.881107, -27.388549], [152.880939, -27.388445], [152.881314, -27.389035], [152.88122, -27.389208], [152.881089, -27.389343], [152.880973, -27.389472], [152.880916, -27.389553], [152.880694, -27.388252]]]], properties:[name:"QDASEQ0001 - Control (100 x 100)", externalId:1, description:"QDASEQ0001 - Control (100 x 100)", notes:"some comment"]]
    }
}
