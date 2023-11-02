package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.DateUtil
import au.org.ala.ecodata.metadata.PropertyAccessor
import groovy.util.logging.Slf4j

/**
 * Configuration about how to work with a Paratoo/Monitor protocol
 */
@Slf4j
class ParatooProtocolConfig {

    String apiEndpoint
    boolean usesPlotLayout = true
    String geometryType = 'Polygon'

    String geometryPath
    String startDatePath = 'attributes.start_date_time'
    String endDatePath = 'attributes.end_date_time'
    String surveyIdPath = 'attributes.surveyId'
    String plotLayoutIdPath = 'attributes.plot_visit.data.attributes.plot_layout.data.id'
    String plotLayoutPointsPath = 'attributes.plot_visit.data.attributes.plot_layout.data.attributes.plot_points'
    String plotSelectionPath = 'attributes.plot_visit.data.attributes.plot_layout.data.attributes.plot_selection.data.attributes'
    String plotLayoutDimensionLabelPath = 'attributes.plot_visit.data.attributes.plot_layout.data.attributes.plot_dimension.data.attributes.label'
    String plotLayoutTypeLabelPath = 'attributes.plot_visit.data.attributes.plot_layout.data.attributes.plot_type.data.attributes.label'
    String getApiEndpoint(ParatooSurveyId surveyId) {
        apiEndpoint ?: defaultEndpoint(surveyId)
    }

    private static String removeMilliseconds(String isoDateWithMillis) {
        if (!isoDateWithMillis) {
            return isoDateWithMillis
        }
        DateUtil.format(DateUtil.parseWithMilliseconds(isoDateWithMillis))
    }

    String getStartDate(Map surveyData) {
        removeMilliseconds(getProperty(surveyData, startDatePath))
    }

    String getEndDate(Map surveyData) {
        removeMilliseconds(getProperty(surveyData, endDatePath))
    }

    Map getSurveyId(Map surveyData) {
        getProperty(surveyData, surveyIdPath)
    }

    private Map extractSiteDataFromPath(Map surveyData) {
        Map geometry = null
        def geometryData = getProperty(surveyData, geometryPath)
        if (geometryData) {
            switch (geometryType) {
                case 'Point':
                    geometry = [type:'Point', coordinates:[geometryData.lng, geometryData.lat]]
                    break
            }
        }
        else {
            log.warn("Unable to get spatial data from survey: "+apiEndpoint+", "+geometryPath)
        }
        geometry
    }

    private static String defaultEndpoint(ParatooSurveyId surveyId) {
        String apiEndpoint = surveyId.surveyType
        if (!apiEndpoint.endsWith('s')) {
            apiEndpoint += 's'
        } // strapi makes the endpoint plural sometimes?
        apiEndpoint
    }

    private static def getProperty(Map surveyData, String path) {
        if (!path) {
            return null
        }
        new PropertyAccessor(path).get(surveyData)
    }

    Map getGeoJson(Map survey) {
        if (!survey) {
            return null
        }

        Map geoJson = null
        if (usesPlotLayout) {
            geoJson = extractSiteDataFromPlotVisit(survey)
        }
        else if (geometryPath) {
            geoJson = extractSiteDataFromPath(survey)
        }
        geoJson
    }

    boolean matches(Map surveyData, ParatooSurveyId surveyId) {
        Map tmpSurveyId = getSurveyId(surveyData)
        tmpSurveyId.surveyType == surveyId.surveyType &&
                tmpSurveyId.time == surveyId.timeAsISOString() &&
                tmpSurveyId.randNum == surveyId.randNum
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

    private static List closePolygonIfRequired(List points) {
        if (points[0][0] != points[-1][0] || points[0][1] != points[-1][1]) {
            points << points[0]
        }
        points
    }

    private static boolean exclude(Map point) {
        point.name?.data?.attributes?.symbol == "C" // The plot layout has a centre point that we don't want
    }

    static Map plotSelectionToGeoJson(Map plotSelectionData) {
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
                notes: plotSelectionData.comment
        ]
        geoJson
    }
}
