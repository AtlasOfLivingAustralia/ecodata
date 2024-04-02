package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.*
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.PropertyAccessor
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.util.logging.Slf4j
import org.locationtech.jts.geom.Geometry
/**
 * Configuration about how to work with a Paratoo/Monitor protocol
 */
@Slf4j
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooProtocolConfig {

    String name
    String apiEndpoint
    boolean usesPlotLayout = true
    List tags
    String geometryType = 'Polygon'

    String geometryPath
    String startDatePath = 'start_date_time'
    String endDatePath = 'end_date_time'
    String surveyIdPath = 'survey_metadata'
    String plotVisitPath = 'plot_visit'
    String plotLayoutPath = "${plotVisitPath}.plot_layout"
    String plotLayoutIdPath = "${plotLayoutPath}.id"
    String plotLayoutPointsPath = "${plotLayoutPath}.plot_points"
    String plotSelectionPath = "${plotLayoutPath}.plot_selection"
    String plotLayoutDimensionLabelPath = "${plotLayoutPath}.plot_dimensions.label"
    String plotLayoutTypeLabelPath = "${plotLayoutPath}.plot_type.label"
    String getApiEndpoint(ParatooCollectionId surveyId) {
        apiEndpoint ?: defaultEndpoint(surveyId)
    }
    Map overrides = [dataModel: [:], viewModel: [:]]

    ParatooCollectionId surveyId

    private static String removeMilliseconds(String isoDateWithMillis) {
        if (!isoDateWithMillis) {
            return isoDateWithMillis
        }
        DateUtil.format(DateUtil.parseWithMilliseconds(isoDateWithMillis))
    }

    String getStartDate(Map surveyData) {
        if(startDatePath == null || surveyData == null) {
            return null
        }

        String date = getProperty(surveyData, startDatePath)
        if (date == null) {
            date = getPropertyFromSurvey(surveyData, startDatePath)
        }

        removeMilliseconds(date)
    }

    def getPropertyFromSurvey(Map surveyData, String path) {
        surveyData = getSurveyDataFromObservation(surveyData)
        path = path.replaceFirst("^attributes.", '')
        getProperty(surveyData, path)
    }

    String getEndDate(Map surveyData) {
        if(endDatePath == null || surveyData == null) {
            return null
        }

        String date = getProperty(surveyData, endDatePath)
        if (date == null) {
            date = getPropertyFromSurvey(surveyData, endDatePath)
        }

        removeMilliseconds(date)
    }

    Map getSurveyId(Map surveyData) {
        if(surveyIdPath == null || surveyData == null) {
            return null
        }

        Map result = getProperty(surveyData, surveyIdPath)
        if (result == null) {
            result = getPropertyFromSurvey(surveyData, surveyIdPath)
        }

        result
    }

    private Map extractSiteDataFromPath(Map surveyData) {
        def geometryData = getProperty(surveyData, geometryPath)
        extractGeometryFromSiteData(geometryData)
    }

    private List extractFeatures (Map observation, ActivityForm form) {
        List features = []
        form.sections.each { FormSection section ->
            OutputMetadata om = new OutputMetadata(section.template)
            Map paths = om.getNamesForDataType("feature", null )
            features.addAll(getFeaturesFromPath(observation, paths))
        }

        features
    }

    private List getFeaturesFromPath (Map output, Map paths) {
        List features = []
        paths.each { String name, node ->
            if (node instanceof Boolean) {
                features.add(output[name])
                // todo later: add featureIds and modelId for compliance with feature behaviour of reports
            }

            // recursive check for feature
            if (node instanceof Map) {
                if (output[name] instanceof Map) {
                    features.addAll(getFeaturesFromPath(output[name], node))
                }

                if (output[name] instanceof List) {
                    output[name].eachWithIndex { row, index ->
                        features.addAll(getFeaturesFromPath(row, node))
                    }
                }
            }
        }

        features
    }

    private Map extractGeometryFromSiteData(geometryData) {
        Map geometry = null
        if (geometryData) {
            switch (geometryType) {
                case 'Point':
                    geometry = [type: 'Point', coordinates: [geometryData.lng, geometryData.lat]]
                    break
            }
        } else {
            log.warn("Unable to get spatial data from survey: " + apiEndpoint + ", " + geometryPath)
        }

        geometry
    }

    private static String defaultEndpoint(ParatooCollectionId surveyId) {
        String apiEndpoint = surveyId.survey_metadata?.survey_details?.survey_model
        if (!apiEndpoint.endsWith('s')) {
            apiEndpoint += 's'
        } // strapi makes the endpoint plural sometimes?
        apiEndpoint
    }

    def getProperty(Map surveyData, String path) {
        if (surveyId) {
            path = surveyId.survey_metadata.survey_details.survey_model+'.'+path
        }

        if (!path) {
            return null
        }
        new PropertyAccessor(path).get(surveyData)
    }

    Map getGeoJson(Map survey, Map observation = null, ActivityForm form = null) {
        if (!survey) {
            return null
        }

        Map geoJson = null
        if (usesPlotLayout) {
            geoJson = extractSiteDataFromPlotVisit(survey)
            // get list of all features associated with observation
            if (geoJson && form && observation) {
                geoJson.features = extractFeatures(observation, form)
            }
        }
        else if (form && observation) {
            List features = extractFeatures(observation, form)
            if (features) {
                Geometry geometry = GeometryUtils.getFeatureCollectionConvexHull(features)
                geoJson = GeometryUtils.geometryToGeoJsonMap(geometry)
                geoJson.features = features
            }
        }
        else if (geometryPath) {
            geoJson = extractSiteDataFromPath(survey)
        }

        geoJson
    }

    Map getPlotVisit (Map surveyData) {
        Map plotVisit = getProperty(surveyData, plotVisitPath)
        copyWithExcludedProperty(plotVisit, ParatooService.PARATOO_DATAMODEL_PLOT_LAYOUT)
    }

    Map getPlotLayout (Map surveyData) {
        Map plotLayout = getProperty(surveyData, plotLayoutPath)
        copyWithExcludedProperty(plotLayout, ParatooService.PARATOO_DATAMODEL_PLOT_SELECTION)
    }

    Map getPlotSelection (Map surveyData) {
        Map plotSelection = getProperty(surveyData, plotSelectionPath)
        copyWithExcludedProperty(plotSelection)
    }

    private Map copyWithExcludedProperty(Map map, String property = null) {
        if (!map) {
            return [:]
        }
        List keys = map.keySet().toList()
        if (property) {
            keys = keys.minus(property)
        }
        Map result = [:]
        keys.each { key ->
            result[key] = map[key]
        }

        result
    }


    boolean matches(Map surveyData, ParatooCollectionId collectionId) {
        Map tmpSurveyId = getSurveyId(surveyData)
        if (!tmpSurveyId) {
            log.error("Cannot find surveyId:")
            log.debug(surveyData.toString())
            return false
        }

        surveyEqualityTest(tmpSurveyId, collectionId)
    }

    static boolean surveyEqualityTest(Map tmpSurveyId, ParatooCollectionId collectionId) {
        tmpSurveyId?.survey_details?.survey_model == collectionId.survey_metadata?.survey_details.survey_model &&
                tmpSurveyId?.survey_details?.time == collectionId.survey_metadata?.survey_details.time &&
                tmpSurveyId?.survey_details?.uuid == collectionId.survey_metadata?.survey_details.uuid
    }

    private Map extractSiteDataFromPlotVisit(Map survey) {

        def plotLayoutId = getProperty(survey, plotLayoutIdPath) // Currently an int, may become uuid?

        if (!plotLayoutId) {
            log.warn("No plot_layout found in survey at path ${plotLayoutIdPath}")
            return null
        }
        List plotLayoutPoints = getProperty(survey, plotLayoutPointsPath)
        Map plotSelection = getProperty(survey, plotSelectionPath)
        Map plotSelectionGeoJson = plotSelectionToGeoJson(plotSelection)

        String plotLayoutDimensionLabel = getProperty(survey, plotLayoutDimensionLabelPath)
        String plotLayoutTypeLabel = getProperty(survey, plotLayoutTypeLabelPath)

        String name = plotSelectionGeoJson.properties.name + ' - ' + plotLayoutTypeLabel + ' (' + plotLayoutDimensionLabel + ')'

        Map plotGeometory = toGeometry(plotLayoutPoints)
        Map plotGeoJson = [
            type: 'Feature',
            geometry: plotGeometory,
            properties: [
                    name: name,
                    externalId: plotLayoutId,
                    description: name,
                    notes: plotSelectionGeoJson?.properties?.notes
            ]
        ]

        //Map faunaPlotGeoJson = toGeometry(plotLayout.fauna_plot_point)

        // TODO maybe turn this into a feature with properties to distinguish the fauna plot?
        // Or a multi-polygon?

        plotGeoJson
    }

    static Map toGeometry(List points) {
        List coords = points?.findAll { !exclude(it) }.collect {
            [it.lng, it.lat]
        }
        Map plotGeometry = coords ? [
                type       : 'Polygon',
                coordinates: [closePolygonIfRequired(coords)]
        ] : null

        plotGeometry
    }

    Map getSurveyDataFromObservation(Map observation) {
        String surveyAttribute = apiEndpoint
        if(surveyAttribute?.endsWith('s')) {
            surveyAttribute = surveyAttribute.substring(0, surveyAttribute.length() - 1)
        }

        def survey = observation[surveyAttribute]
        if (survey instanceof List) {
            return survey[0]
        }

        survey
    }

    private static List closePolygonIfRequired(List points) {
        if (points[0][0] != points[-1][0] || points[0][1] != points[-1][1]) {
            points << points[0]
        }
        points
    }

    private static boolean exclude(Map point) {
        point.name?.data?.attributes?.symbol == "C" // The plot layout has a centre point that we don't want
    }

    // Accepts a Map or ParatooPlotSelectionData as this is used by two separate calls.
    static Map plotSelectionToGeoJson(def plotSelectionData) {
        Map geoJson = [:]
        geoJson.type = 'Feature'
        geoJson.geometry = [
                type:'Point',
                coordinates: [plotSelectionData.recommended_location.lng, plotSelectionData.recommended_location.lat]
        ]
        geoJson.properties = [
                name : plotSelectionData.plot_label,
                externalId: plotSelectionData.uuid,
                description: plotSelectionData.plot_label,
                notes: plotSelectionData.comments
        ]
        geoJson
    }
}
