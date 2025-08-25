package au.org.ala.ecodata

import au.org.ala.ecodata.spatial.SpatialConversionUtils
import au.org.ala.ecodata.spatial.SpatialUtils
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.tuple.Pair
import org.locationtech.jts.geom.Geometry
import org.springframework.web.multipart.MultipartFile

import javax.servlet.http.HttpServletResponse
@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class SpatialController {
    SpatialService spatialService
    static responseFormats = ['json', 'xml']
    static allowedMethods = [uploadShapeFile: "POST", getShapeFileFeatureGeoJson: "GET"]

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def uploadShapeFile() {
        // Use linked hash map to maintain key ordering
        Map<Object, Object> retMap = new LinkedHashMap<Object, Object>()

        File tmpZipFile = File.createTempFile("shpUpload", ".zip")

        if (ServletFileUpload.isMultipartContent(request)) {
            // Parse the request
            Map<String, MultipartFile> items = request.getFileMap()

            if (items.size() == 1) {
                MultipartFile fileItem = items.values()[0]
                IOUtils.copy(fileItem.getInputStream(), new FileOutputStream(tmpZipFile))
                retMap.putAll(handleZippedShapeFile(tmpZipFile))
                response.setStatus(HttpServletResponse.SC_OK)
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                retMap.put("error", "Multiple files sent in request. A single zipped shape file should be supplied.")
            }
        }

        respond retMap
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def getShapeFileFeatureGeoJson() {
        Map retMap
        String shapeId = params.shapeFileId
        String featureIndex = params.featureId
        if (featureIndex != null && shapeId != null) {

            retMap = processShapeFileFeatureRequest(shapeId, featureIndex)
            if(retMap.geoJson == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            }
            else {
                response.setStatus(HttpServletResponse.SC_OK)
            }
        }
        else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            retMap = ["error": "featureId and shapeFileId must be supplied"]
        }

        respond retMap
    }

    def features() {
        def retVariable
        if (!params.layerId) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            retVariable = ["error": "layerId must be supplied"]
        }
        else {
            List<String> intersectWith = params.intersectWith ? params.intersectWith.split(",") : []
            retVariable = spatialService.features(params.layerId, intersectWith)
        }

        respond retVariable
    }

    private Map<String, String> processShapeFileFeatureRequest(String shapeFileId, String featureIndex) {
        Map<String, Object> retMap = new HashMap<String, Object>()

        try {
            File shpFileDir = new File(System.getProperty("java.io.tmpdir"), shapeFileId)
            Geometry geoJson = SpatialUtils.getShapeFileFeaturesAsGeometry(shpFileDir, featureIndex)

            if (geoJson == null) {
                retMap.put("error", "Invalid geometry")
                return retMap
            }
            else {
                if (geoJson.getCoordinates().flatten().size() > grailsApplication.config.getProperty("shapefile.simplify.threshhold", Integer,  50_000)) {
                    geoJson = GeometryUtils.simplifyGeometry(geoJson, grailsApplication.config.getProperty("shapefile.simplify.tolerance", Double, 0.0001))
                }

                retMap.put("geoJson", GeometryUtils.geometryToGeoJsonMap(geoJson, grailsApplication.config.getProperty("shapefile.geojson.decimal", Integer, 20)))
            }
        } catch (Exception ex) {
            log.error("Error processsing shapefile feature request", ex)
            retMap.put("error", ex.getMessage())
        }

        return retMap
    }

    private static Map<Object, Object> handleZippedShapeFile(File zippedShp) throws IOException {
        // Use linked hash map to maintain key ordering
        Map<Object, Object> retMap = new LinkedHashMap<Object, Object>()

        Pair<String, File> idFilePair = SpatialConversionUtils.extractZippedShapeFile(zippedShp)
        String uploadedShpId = idFilePair.getLeft()
        File shpFile = idFilePair.getRight()

        retMap.put("shp_id", uploadedShpId)

        List<List<Pair<String, Object>>> manifestData = SpatialConversionUtils.getShapeFileManifest(shpFile)

        int featureIndex = 0
        for (List<Pair<String, Object>> featureData : manifestData) {
            // Use linked hash map to maintain key ordering
            Map<String, Object> featureDataMap = new LinkedHashMap<String, Object>()

            for (Pair<String, Object> fieldData : featureData) {
                featureDataMap.put(fieldData.getLeft(), fieldData.getRight())
            }

            retMap.put(featureIndex, featureDataMap)

            featureIndex++
        }

        return retMap
    }
}
