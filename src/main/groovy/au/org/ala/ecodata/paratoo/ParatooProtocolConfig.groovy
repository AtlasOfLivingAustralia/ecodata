package au.org.ala.ecodata.paratoo

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
    String startDatePath
    String endDatePath
    String surveyIdPath = 'attributes.surveyId'
    String plotLayoutPath = 'attributes.plot_visit.data.attributes.plot_layout.data.attributes'
    String getApiEndpoint(ParatooSurveyId surveyId) {
        apiEndpoint ?: defaultEndpoint(surveyId)
    }

    String getStartDate(Map surveyData) {
        getProperty(surveyData, startDatePath)
    }

    String getEndDate(Map surveyData) {
        getProperty(surveyData, endDatePath)
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

        Map geometry = null
        if (usesPlotLayout) {
            geometry = extractSiteDataFromPlotVisit(survey)
        }
        else if (geometryPath) {
            geometry = extractSiteDataFromPath(survey)
        }
        geometry
    }

    boolean matches(Map surveyData, ParatooSurveyId surveyId) {
        Map tmpSurveyId = getSurveyId(surveyData)
        tmpSurveyId.surveyType == surveyId.surveyType &&
                tmpSurveyId.time == surveyId.timeAsISOString() &&
                tmpSurveyId.randNum == surveyId.randNum
    }

    private Map extractSiteDataFromPlotVisit(Map survey) {
        Map plotLayout = getProperty(survey, plotLayoutPath)

        if (!plotLayout) {
            log.warn("No plot_layout found in survey at path ${plotLayoutPath}")
            return null
        }

        //Map plotSelection = getProperty(survey, plotSelectionPath)
        //Map plotVisit = getProperty(survey, plotVisitPath)
        // Plot selection & plot visit will be useful for metadata - name, comments, description etc.

        Map plotGeoJson = toGeoJson(plotLayout.plot_points)
        Map faunaPlotGeoJson = toGeoJson(plotLayout.fauna_plot_point)

        // TODO maybe turn this into a feature with properties to distinguish the fauna plot?
        // Or a multi-polygon?

        plotGeoJson
    }

    private static Map toGeoJson(List points) {
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
        if (points[0][0] != points[-1][0] && points[0][1] != points[-1][1]) {
            points << points[0]
        }
        points
    }

    private static boolean exclude(Map point) {
        point.name?.data?.attributes?.symbol == "C" // The plot layout has a centre point that we don't want
    }
}
