package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.ExternalId
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

class ParatooProtocolConfigSpec extends Specification {

    def setup() {
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    private Map readSurveyData(String name) {
        URL url = getClass().getResource("/paratoo/${name}.json")
        new JsonSlurper().parse(url)
    }

    private Map readSurveyObservations(String name) {
        URL url = getClass().getResource("/paratoo/${name}.json")
        new JsonSlurper().parse(url)
    }

    def "The vegetation-mapping-survey can be used with this config"() {

        setup:
        Map surveyData = readSurveyData('vegetationMappingSurvey')
        Map observation = readSurveyData('vegetationMappingObservation')
        Map vegetationMappingConfig = [
                apiEndpoint:'vegetation-mapping-observations',
                usesPlotLayout:false,
                geometryType:'Point',
                geometryPath:'attributes.position',
                startDatePath:'attributes.vegetation_mapping_survey.data.attributes.start_date_time',
                endDatePath: null
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(vegetationMappingConfig)
        ActivityForm activityForm = new ActivityForm(
                name: "aParatooForm 1",
                type: 'EMSA',
                category: 'protocol category 1',
                external: true,
                sections: [
                        [
                            name: "section 1",
                            template: [
                                    dataModel: [[
                                        name: "position",
                                        dataType: "feature",
                                        required: true,
                                        external: true
                                    ]]
                            ]
                        ]
                ]
        )
        activityForm.externalIds = [new ExternalId(externalId: "guid-2", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]

        expect:
        config.getStartDate(surveyData) == '2023-09-08T23:39:00Z'
        config.getEndDate(surveyData) == null
        config.getGeoJson(surveyData) == [type:'Point', coordinates:[149.0651536, -35.2592398]]
        config.getGeoJson(surveyData, observation, activityForm).features == [[type:'Point', coordinates:[149.0651536, -35.2592398]]]
    }

    def "The floristics-standard survey can be used with this config"() {
        setup:
        Map surveyData = readSurveyData('floristicsStandardReverseLookup')
        Map floristicsSurveyConfig = [
                apiEndpoint:'floristics-veg-survey-lites',
                usesPlotLayout:true,
                startDatePath: 'plot_visit.start_date',
                endDatePath: 'plot_visit.end_date'
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(floristicsSurveyConfig)
        config.setSurveyId(ParatooCollectionId.fromMap([survey_metadata: surveyData.survey_metadata]))

        expect:
        config.getStartDate(surveyData.collections) == '2024-03-26T03:03:26Z'
        config.getEndDate(surveyData.collections) == '2024-03-26T03:03:26Z'
        config.getGeoJson(surveyData.collections) == [type:"Feature", geometry:[type:"Polygon", coordinates:[[[149.0651452, -35.2592569], [149.0651452, -35.259167068471584], [149.0651452, -35.258987405414764], [149.0651452, -35.25880774235794], [149.0651452, -35.25862807930111], [149.0651452, -35.25844841624429], [149.0651452, -35.25835858471588], [149.06525521373527, -35.25835858471588], [149.06547524120586, -35.25835858471588], [149.06569526867642, -35.25835858471588], [149.06591529614698, -35.25835858471588], [149.06613532361757, -35.25835858471588], [149.06624533735285, -35.25835858471588], [149.06624533735285, -35.25844841624429], [149.06624533735285, -35.25862807930111], [149.06624533735285, -35.25880774235794], [149.06624533735285, -35.258987405414764], [149.06624533735285, -35.259167068471584], [149.06624533735285, -35.2592569], [149.06613532361757, -35.2592569], [149.06591529614698, -35.2592569], [149.06569526867642, -35.2592569], [149.06547524120586, -35.2592569], [149.06525521373527, -35.2592569], [149.06569526867645, -35.25880774235794], [149.0651452, -35.2592569]]]], properties:[name:"CTMSEH4221 - Control (100 x 100)", externalId:12, description:"CTMSEH4221 - Control (100 x 100)", notes:"Test again 2024-03-26"]]
    }

    def "The basal-area-dbh-measure-survey can be used with this config"() {
        setup:
        Map surveyData = readSurveyData('basalAreaDbhReverseLookup')
        Map basalAreaDbhMeasureSurveyConfig = [
                apiEndpoint:'basal-area-dbh-measure-surveys',
                usesPlotLayout:true,
                startDatePath: 'start_date',
                endDatePath: 'start_date',
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(basalAreaDbhMeasureSurveyConfig)
        config.setSurveyId(ParatooCollectionId.fromMap([survey_metadata: surveyData.survey_metadata]))

        expect:
        config.getStartDate(surveyData.collections) == '2024-03-26T03:03:26Z'
        config.getEndDate(surveyData.collections) == "2024-03-26T03:03:26Z"
        config.getGeoJson(surveyData.collections) == [type:"Feature", geometry:[type:"Polygon", coordinates:[[[149.0651452, -35.2592569], [149.0651452, -35.259167068471584], [149.0651452, -35.258987405414764], [149.0651452, -35.25880774235794], [149.0651452, -35.25862807930111], [149.0651452, -35.25844841624429], [149.0651452, -35.25835858471588], [149.06525521373527, -35.25835858471588], [149.06547524120586, -35.25835858471588], [149.06569526867642, -35.25835858471588], [149.06591529614698, -35.25835858471588], [149.06613532361757, -35.25835858471588], [149.06624533735285, -35.25835858471588], [149.06624533735285, -35.25844841624429], [149.06624533735285, -35.25862807930111], [149.06624533735285, -35.25880774235794], [149.06624533735285, -35.258987405414764], [149.06624533735285, -35.259167068471584], [149.06624533735285, -35.2592569], [149.06613532361757, -35.2592569], [149.06591529614698, -35.2592569], [149.06569526867642, -35.2592569], [149.06547524120586, -35.2592569], [149.06525521373527, -35.2592569], [149.06569526867645, -35.25880774235794], [149.0651452, -35.2592569]]]], properties:[name:"CTMSEH4221 - Control (100 x 100)", externalId:12, description:"CTMSEH4221 - Control (100 x 100)", notes:"Test again 2024-03-26"]]
    }

    def "The observations from opportunistic-survey can be filtered" () {
        setup:
        Map surveyObservations = readSurveyObservations('opportunisticSurveyObservations')
        Map opportunisticSurveyConfig = [
                apiEndpoint:'opportunistic-surveys',
                usesPlotLayout:false,
                geometryType: 'Point',
                startDatePath: 'attributes.startdate',
                endDatePath: 'attributes.updatedAt'
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(opportunisticSurveyConfig)
        ParatooCollectionId paratooSurveyId = new ParatooCollectionId(
                survey_metadata: [
                        survey_details: [
                                survey_model: 'opportunistic-survey',
                                time: "2023-10-24T00:59:48.456Z",
                                uuid: '10a03062-2b0d-40bb-a6d7-e72f06788b94'
                        ]
                ]
        )
        def start_date = config.getStartDate(surveyObservations.data[0])
        def end_date = config.getEndDate(surveyObservations.data[0])

        expect:
        start_date == null
        end_date == "2023-10-24T01:01:56Z"

    }
}
