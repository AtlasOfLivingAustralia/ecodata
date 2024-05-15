package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.DateUtil
import au.org.ala.ecodata.ExternalId
import au.org.ala.ecodata.FormSection
import au.org.ala.ecodata.ParatooService
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

class ParatooProtocolConfigSpec extends Specification {
    ParatooService paratooService

    def setup() {
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
        paratooService = new ParatooService()
    }

    private Map readSurveyData(String name) {
        URL url = getClass().getResource("/paratoo/${name}.json")
        new JsonSlurper().parse(url)
    }

    def "The vegetation-mapping-survey can be used with this config"() {

        setup:
        Map surveyData = readSurveyData('vegetationMappingObservationReverseLookup')
        Map observation = surveyData?.collections
        Map vegetationMappingConfig = [
                apiEndpoint               : 'vegetation-mapping-surveys',
                usesPlotLayout            : false,
                geometryType              : 'Point'
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(vegetationMappingConfig)
        config.surveyId = ParatooCollectionId.fromMap([survey_metadata: surveyData.survey_metadata])
        ActivityForm activityForm = new ActivityForm(
                name: "aParatooForm 1",
                type: 'EMSA',
                category: 'protocol category 1',
                external: true,
                sections: [
                        [
                                name    : "section 1",
                                template: [
                                        dataModel    : [
                                                [
                                                        name    : "vegetation-mapping-survey",
                                                        dataType: "list",
                                                        columns : [

                                                        ]
                                                ],
                                                [
                                                        name    : "vegetation-mapping-observation",
                                                        dataType: "list",
                                                        columns : [
                                                                [
                                                                        dataType: "feature",
                                                                        name    : "position"
                                                                ]
                                                        ]
                                                ]
                                        ],
                                        relationships: [
                                                ecodata  : [:],
                                                apiOutput: [:]
                                        ]
                                ]
                        ]
                ]
        )
        activityForm.externalIds = [new ExternalId(externalId: "guid-3", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]

        when:
        transformData(observation, activityForm, config)

        then:
        config.getStartDate(observation) == '2023-09-08T23:39:00Z'
        config.getEndDate(observation) == null
        config.getGeoJson(observation, activityForm).features == [
                [
                        type      : "Feature",
                        geometry  : [type: 'Point', coordinates: [149.0651536, -35.2592398]],
                        properties: [
                                name      : "Point aParatooForm 1-1",
                                externalId: 44,
                                id        : "aParatooForm 1-1"
                        ]
                ]
        ]
    }

    def "The floristics-standard survey can be used with this config"() {
        setup:
        Map apiOutput = readSurveyData('floristicsStandardReverseLookup')
        Map observation = apiOutput.collections
        Map floristicsSurveyConfig = [
                apiEndpoint:'floristics-veg-survey-lites',
                usesPlotLayout:true
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(floristicsSurveyConfig)
        config.setSurveyId(ParatooCollectionId.fromMap([survey_metadata: apiOutput.survey_metadata]))
        ActivityForm activityForm = new ActivityForm(
                name: "aParatooForm 1",
                type: 'EMSA',
                category: 'protocol category 1',
                external: true,
                sections: [
                        [
                                name    : "section 1",
                                template: [
                                        dataModel    : [[
                                                                name    : "floristics-veg-survey-lite",
                                                                dataType: "list",
                                                                columns : []
                                                        ]],
                                        relationships: [
                                                ecodata  : [:],
                                                apiOutput: [:]
                                        ]
                                ]
                        ]
                ]
        )


        when:
        transformData(observation, activityForm, config)

        then:
        config.getStartDate(observation) == "2022-09-21T01:55:44Z"
        config.getEndDate(observation) == "2022-09-21T01:55:44Z"
        config.getGeoJson(observation) == [
                type: "Feature",
                geometry: [
                        type: "Polygon",
                        coordinates: [[[152.880694, -27.388252], [152.880651, -27.388336], [152.880518, -27.388483], [152.880389, -27.388611], [152.88028, -27.388749], [152.880154, -27.388903], [152.880835, -27.389463], [152.880644, -27.389366], [152.880525, -27.389248], [152.88035, -27.389158], [152.880195, -27.389021], [152.880195, -27.389373], [152.880797, -27.388316], [152.881448, -27.388909], [152.881503, -27.388821], [152.881422, -27.388766], [152.881263, -27.388644], [152.881107, -27.388549], [152.880939, -27.388445], [152.881314, -27.389035], [152.88122, -27.389208], [152.881089, -27.389343], [152.880973, -27.389472], [152.880916, -27.389553], [152.880694, -27.388252]]]
                ],
                properties: [name: "QDASEQ0001 - Control (100 x 100)", externalId: "1", description: "QDASEQ0001 - Control (100 x 100)", notes: "Core monitoring plot some comment"]
        ]
    }

    def "The basal-area-dbh-measure-survey can be used with this config"() {
        setup:
        Map surveyData = readSurveyData('basalAreaDbhReverseLookup')
        Map basalAreaDbhMeasureSurveyConfig = [
                apiEndpoint:'basal-area-dbh-measure-surveys',
                usesPlotLayout:true
        ]
        ParatooProtocolConfig config = new ParatooProtocolConfig(basalAreaDbhMeasureSurveyConfig)
        config.setSurveyId(ParatooCollectionId.fromMap([survey_metadata: surveyData.survey_metadata]))
        Map observation = surveyData?.collections
        ActivityForm activityForm = new ActivityForm(name: "aParatooForm 2", type: 'EMSA', category: 'protocol category 2', external: true,
                sections: [
                        new FormSection(name: "section 1", type: "section", template: [
                                dataModel    : [
                                        [
                                                dataType: "list",
                                                name    : "basal-area-dbh-measure-survey",
                                                columns : [

                                                ]
                                        ],
                                        [
                                                dataType: "list",
                                                name    : "basal-area-dbh-measure-observation",
                                                columns : [
                                                        [
                                                                dataType: "feature",
                                                                name    : "location"
                                                        ]
                                                ]
                                        ]
                                ],
                                viewModel    : [],
                                relationships: [
                                        ecodata  : [:],
                                        apiOutput: [:]
                                ]
                        ]
                        )
                ])
        activityForm.externalIds = [new ExternalId(externalId: "guid-3", idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID)]
        transformData(observation, activityForm, config)

        expect:
        config.getStartDate(observation) == "2023-09-22T00:59:47Z"
        config.getEndDate(observation) == "2023-09-23T00:59:47Z"
        config.getGeoJson(observation) == [
                type:"Feature",
                geometry:[
                        coordinates:[[[138.6372, -34.9723], [138.6371, -34.9723], [138.6371, -34.9714], [138.6382, -34.9714], [138.6383, -34.9714], [138.6383, -34.9723], [138.6372, -34.9723]]],
                        type:"Polygon"
                ],
                properties:[name:"SATFLB0001 - Control (100 x 100)", externalId:"2", notes:"Core monitoring plot some comment", description:"SATFLB0001 - Control (100 x 100) (convex hull of all features)"],
                features:[
                        [
                                type:"Feature",
                                geometry:[
                                        type:"Polygon",
                                        coordinates:[[[138.63720760798054, -34.97222197296049], [138.63720760798054, -34.97204230990367], [138.63720760798054, -34.971862646846844], [138.63720760798054, -34.97168298379002], [138.63720760798054, -34.9715033207332], [138.63720760798054, -34.971413489204785], [138.63731723494544, -34.971413489204785], [138.6375364888752, -34.971413489204785], [138.63775574280498, -34.971413489204785], [138.63797499673475, -34.971413489204785], [138.63819425066453, -34.971413489204785], [138.63830387762943, -34.971413489204785], [138.63830387762943, -34.9715033207332], [138.63830387762943, -34.97168298379002], [138.63830387762943, -34.971862646846844], [138.63830387762943, -34.97204230990367], [138.63830387762943, -34.97222197296049], [138.63830387762943, -34.9723118044889], [138.63819425066453, -34.9723118044889], [138.63797499673475, -34.9723118044889], [138.63775574280498, -34.9723118044889], [138.6375364888752, -34.9723118044889], [138.63731723494544, -34.9723118044889], [138.63720760798054, -34.9723118044889], [138.63720760798054, -34.97222197296049]]]
                                ],
                                properties:[name:"SATFLB0001 - Control (100 x 100)", externalId:"2", description:"SATFLB0001 - Control (100 x 100)", notes:"Core monitoring plot some comment"]],
                        [
                                type:"Feature",
                                geometry:[
                                        type:"Polygon",
                                        coordinates:[[[138.6371026907952, -34.971403261821905], [138.63709732396242, -34.972304399720215], [138.6381916652405, -34.972304399720215], [138.63819166764344, -34.9714076576406], [138.6371026907952, -34.971403261821905]]]
                                ],
                                properties:[
                                        name:"SATFLB0001 - Control (100 x 100)", externalId:"2", description:"SATFLB0001 - Control (100 x 100)", notes:"Fauna plot some comment"
                                ]
                        ]
                ]
        ]
        config.getGeoJson(observation, activityForm).features == [
                [
                        type:"Feature",
                        geometry:[
                                type:"Polygon",
                                coordinates:[[[138.63720760798054, -34.97222197296049], [138.63720760798054, -34.97204230990367], [138.63720760798054, -34.971862646846844], [138.63720760798054, -34.97168298379002], [138.63720760798054, -34.9715033207332], [138.63720760798054, -34.971413489204785], [138.63731723494544, -34.971413489204785], [138.6375364888752, -34.971413489204785], [138.63775574280498, -34.971413489204785], [138.63797499673475, -34.971413489204785], [138.63819425066453, -34.971413489204785], [138.63830387762943, -34.971413489204785], [138.63830387762943, -34.9715033207332], [138.63830387762943, -34.97168298379002], [138.63830387762943, -34.971862646846844], [138.63830387762943, -34.97204230990367], [138.63830387762943, -34.97222197296049], [138.63830387762943, -34.9723118044889], [138.63819425066453, -34.9723118044889], [138.63797499673475, -34.9723118044889], [138.63775574280498, -34.9723118044889], [138.6375364888752, -34.9723118044889], [138.63731723494544, -34.9723118044889], [138.63720760798054, -34.9723118044889], [138.63720760798054, -34.97222197296049]]]
                        ],
                        properties:[
                                name:"SATFLB0001 - Control (100 x 100)",
                                externalId:"2",
                                description:"SATFLB0001 - Control (100 x 100)",
                                notes:"Core monitoring plot some comment"
                        ]
                ],
                [
                        type:"Feature",
                        geometry:[
                                type:"Polygon",
                                coordinates:[[[138.6371026907952, -34.971403261821905], [138.63709732396242, -34.972304399720215], [138.6381916652405, -34.972304399720215], [138.63819166764344, -34.9714076576406], [138.6371026907952, -34.971403261821905]]]
                        ],
                        properties:[
                                name:"SATFLB0001 - Control (100 x 100)",
                                externalId:"2",
                                description:"SATFLB0001 - Control (100 x 100)",
                                notes:"Fauna plot some comment"
                        ]
                ]
        ]
    }

    def "The observations from opportunistic-survey can be filtered"() {
        setup:
        Map response = readSurveyData('opportunisticSurveyObservationsReverseLookup')
        Map surveyObservations = response?.collections
        Map opportunisticSurveyConfig = [
                apiEndpoint   : 'opportunistic-surveys',
                usesPlotLayout: false,
                geometryType  : 'Point'
        ]
        ActivityForm activityForm = new ActivityForm(
                name: "aParatooForm 1",
                type: 'EMSA',
                category: 'protocol category 1',
                external: true,
                sections: [
                        [
                                name    : "section 1",
                                template: [
                                        dataModel    : [
                                                [
                                                        name    : "opportunistic-survey",
                                                        dataType: "list",
                                                        columns: []
                                                ],
                                                [
                                                        name    : "opportunistic-observation",
                                                        dataType: "list",
                                                        columns : [
                                                                [
                                                                        name    : "location",
                                                                        dataType: "feature",
                                                                        required: true,
                                                                        external: true
                                                                ]
                                                        ]
                                                ]
                                        ],
                                        relationships: [
                                                ecodata  : [:],
                                                apiOutput: [:]
                                        ]
                                ]
                        ]
                ]
        )

        when:
        ParatooProtocolConfig config = new ParatooProtocolConfig(opportunisticSurveyConfig)
        config.clientTimeZone = TimeZone.getTimeZone("Australia/Sydney")
        config.surveyId = ParatooCollectionId.fromMap([survey_metadata: response.survey_metadata])
        transformData(surveyObservations, activityForm, config)
        String start_date = config.getStartDate(surveyObservations)
        String startDateInDefaultTimeZone = DateUtil.convertUTCDateToStringInTimeZone(start_date, config.clientTimeZone)
        String end_date = config.getEndDate(surveyObservations)
        Map geoJson = config.getGeoJson(surveyObservations, activityForm)

        then:
        start_date == "2024-04-03T03:37:54Z"
        end_date == "2024-04-03T03:39:40Z"
        geoJson == [
                type      : "Feature",
                geometry  : [type: "Point", coordinates: [138.63, -35.0005]],
                features  : [[type: "Feature", geometry: [type: "Point", coordinates: [138.63, -35.0005]], properties:[name:"Point aParatooForm 1-1", externalId:40, id:"aParatooForm 1-1"]]],
                properties: [
                        name       : "aParatooForm 1 site - ${startDateInDefaultTimeZone}",
                        description: "aParatooForm 1 site - ${startDateInDefaultTimeZone} (convex hull of all features)",
                        externalId: "",
                        notes: "",
                ]
        ]
    }

    def "Should create line GeoJSON objects" () {
        when:
        def result = ParatooProtocolConfig.toLineStringGeometry([[lat: 1, lng: 2], [lat: 3, lng: 4]])

        then:
        result == [
                type       : 'LineString',
                coordinates: [[2, 1], [4, 3]]
        ]

        when:
        result = ParatooProtocolConfig.createLineStringFeatureFromGeoJSON([[lat: 1, lng: 2], [lat: 3, lng: 4], [lat: 5, lng: 6]], "test name", "1", "test notes")

        then:
        result == [
                "type"      : "Feature",
                "geometry"  : [
                        type       : 'LineString',
                        coordinates: [[2, 1], [4, 3], [6, 5]]
                ],
                "properties": [
                        "name"      : "test name",
                        "externalId" : "1",
                        "description": "test name",
                        "notes"     : "test notes"
                ]
        ]
    }

    def transformData(Map surveyDataAndObservations, ActivityForm form, ParatooProtocolConfig config) {
        ParatooService.addPlotDataToObservations(surveyDataAndObservations, config)
        paratooService.rearrangeSurveyData(surveyDataAndObservations, surveyDataAndObservations, form.sections[0].template.relationships.ecodata, form.sections[0].template.relationships.apiOutput)
        paratooService.recursivelyTransformData(form.sections[0].template.dataModel, surveyDataAndObservations, form.name)
    }
}
