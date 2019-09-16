package au.org.ala.ecodata

import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier
import com.vividsolutions.jts.util.GeometricShapeFactory
import grails.converters.JSON
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.geotools.geojson.geom.GeometryJSON
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.geotools.referencing.GeodeticCalculator
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.opengis.referencing.operation.MathTransform

import java.awt.geom.Point2D

/**
 * Helper class for working with site geometry.
 */
class GeometryUtils {

    static Log log = LogFactory.getLog(GeometryUtils.class)
    static CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326", true)
    static GeometryFactory geometryFactory = new GeometryFactory()

    static String wktToMultiPolygonWkt(String wkt) {
        Geometry geom = new WKTReader().read(wkt)

        MultiPolygon result = convertToMultiPolygon(geom)
        new WKTWriter().write(result)
    }

    static MultiPolygon geoGsonToMultiPolygon(String geojson) {
        Geometry geom = new GeometryJSON().read(geojson)
        MultiPolygon result = convertToMultiPolygon(geom)
        return result
    }

    private static MultiPolygon convertToMultiPolygon(Geometry geom) {
        MultiPolygon result
        switch (geom.geometryType) {
            case 'Point':
                result = pointToMultipolygon(geom)
                break
            case 'MultiPoint':
                result = multiPointToMultipolygon(geom)
                break
            case 'Polygon':
                result = polygonToMultiPolygon(geom)
                break
            case 'MultiLineString':
                result = multiLineStringToMultiPolygon(geom)
                break
            case 'LineString':
                result = lineStringToMultiPolygon(geom)
                break
            case 'MultiPolygon':
                result = geom
                break
            default:
                throw new IllegalArgumentException("Unsupported Geometry: " + geom.geometryType)
        }
        result
    }

    /**
     * Creates an equilateral triangle with sides of length 1 metre with the point at the centroid.
     * The coordinates of the supplied geometry must be in WGS84.
     */
    static MultiPolygon pointToMultipolygon(Point point) {
        return polygonToMultiPolygon(pointToPolygon(point.x, point.y))
    }

    static Polygon pointToPolygon(double x, double y) {
        GeodeticCalculator gc = new GeodeticCalculator(sourceCRS)
        Coordinate[] triangleCoords = new Coordinate[4]
        double distance = 1d/Math.sqrt(3)
        gc.setStartingGeographicPoint(x, y)
        gc.setDirection(0, distance)
        Point2D n = gc.getDestinationGeographicPoint()
        triangleCoords[0] = new Coordinate(n.x, n.y)
        gc.setDirection(120, distance)
        Point2D se = gc.getDestinationGeographicPoint()
        triangleCoords[1] = new Coordinate(se.x, se.y)
        gc.setDirection(-120, distance)
        Point2D sw = gc.getDestinationGeographicPoint()
        triangleCoords[2] = new Coordinate(sw.x, sw.y)
        // Close the polygon.
        triangleCoords[3] = new Coordinate(n.x, n.y)
        return geometryFactory.createPolygon(triangleCoords)
    }

    /**
     * Creates an equilateral triangle with sides of length 1 metre with the point at the centroid.
     * The coordinates of the supplied geometry must be in WGS84.
     */
    static MultiPolygon multiPointToMultipolygon(MultiPoint multiPoint) {

        Coordinate[] coords = multiPoint.getCoordinates()

        Polygon[] polygons = new Polygon[coords.length]
        for (int i=0; i<coords.length; i++) {
            polygons[i] = pointToPolygon(coords[i].x, coords[i].y)
        }

        return geometryFactory.createMultiPolygon(polygons)
    }

    static MultiPolygon multiLineStringToMultiPolygon(MultiLineString multiLineString) {

        Geometry result = multiLineString.convexHull()
        if (result.geometryType == 'Polygon') {
            return polygonToMultiPolygon(result)
        }
        else if (result.geometryType == 'LineString') {
            return lineStringToMultiPolygon(result)
        }
    }

    static MultiPolygon polygonToMultiPolygon(Polygon polygon) {
        Polygon[] multiPolygon = new Polygon[1]
        multiPolygon[0] = polygon
        return geometryFactory.createMultiPolygon(multiPolygon)
    }

    static MultiPolygon lineStringToMultiPolygon(LineString lineString) {
        Coordinate[] coordinates = lineString.coordinates

        if (coordinates.length == 2) {
            Polygon rectangle = lineToPolygon(coordinates)
            return polygonToMultiPolygon(rectangle)
        }
        else {
            Coordinate[] closedPolygon = new Coordinate[coordinates.length+1]
            System.arraycopy(coordinates, 0, closedPolygon, 0, coordinates.length)
            closedPolygon[coordinates.length] = new Coordinate(coordinates[0].x, coordinates[0].y)

            return polygonToMultiPolygon(geometryFactory.createPolygon(closedPolygon))
        }
    }

    private static Polygon lineToPolygon(Coordinate[] coordinates) {
        Coordinate[] rectangleCoords = new Coordinate[5]
        GeodeticCalculator gc = new GeodeticCalculator(sourceCRS)
        gc.setStartingGeographicPoint(coordinates[0].x, coordinates[0].y)
        gc.setDestinationGeographicPoint(coordinates[1].x, coordinates[1].y)
        double azimuth = gc.getAzimuth()
        double rightAngle = azimuth - 90
        if (rightAngle < -180) {
            rightAngle = -(rightAngle + 180)
        }
        gc.setDirection(rightAngle, 0.5)
        Point2D point1 = gc.getDestinationGeographicPoint()
        rectangleCoords[0] = new Coordinate(point1.x, point1.y)

        gc.setStartingGeographicPoint(coordinates[1].x, coordinates[1].y)
        gc.setDirection(rightAngle, 0.5)
        Point2D point2 = gc.getDestinationGeographicPoint()
        rectangleCoords[1] = new Coordinate(point2.x, point2.y)

        rightAngle = azimuth + 90
        if (rightAngle > 180) {
            rightAngle = -(rightAngle - 180)
        }
        gc.setDirection(rightAngle, 0.5)
        Point2D point3 = gc.getDestinationGeographicPoint()
        rectangleCoords[2] = new Coordinate(point3.x, point3.y)

        gc.setStartingGeographicPoint(coordinates[0].x, coordinates[0].y)
        gc.setDirection(rightAngle, 0.5)
        Point2D point4 = gc.getDestinationGeographicPoint()
        rectangleCoords[3] = new Coordinate(point4.x, point4.y)
        rectangleCoords[4] = new Coordinate(point1.x, point1.y)

        return geometryFactory.createPolygon(rectangleCoords)
    }

    /**
     * Projects the site geometry into the appropriate UTM zone based on the centroid and calculates the area
     * @param wgs84Geom Geometry with coordinates in WGS84 lon/lat
     * @return the area of the geometry in m2
     */
    static double area(Geometry wgs84Geom) {
        Geometry utmGeom = wgs84ToUtm(wgs84Geom)
        utmGeom.area
    }

    static Geometry wgs84ToUtm(Geometry wgs84Geom) {
        CoordinateReferenceSystem utm = CRS.decode("AUTO2:42001,"+wgs84Geom.centroid.x+","+wgs84Geom.centroid.y, true)
        MathTransform toMetres = CRS.findMathTransform(sourceCRS, utm)
        JTS.transform(wgs84Geom, toMetres)
    }

    static Geometry geometryForCircle(double centreLat, double centreLon, double radiusInMetres) {

        Geometry geometry = geometryFactory.createPoint(new Coordinate(centreLon, centreLat))
        Geometry utmGeom = wgs84ToUtm(geometry)

        GeometricShapeFactory factory = new GeometricShapeFactory()
        factory.size = radiusInMetres*2
        factory.centre = utmGeom.coordinate

        Geometry circle = factory.createCircle()
        CoordinateReferenceSystem utm = CRS.decode("AUTO2:42001,"+centreLon+","+centreLat, true)
        MathTransform toLatLon = CRS.findMathTransform(utm, sourceCRS)
        JTS.transform(circle, toLatLon)
    }

    static Map buffer(Map geom, int buffer) {
        Geometry input = geoJsonMapToGeometry(geom)
        Geometry output = input.buffer(buffer)
        geometryToGeoJsonMap(output)
    }

    static Geometry geoJsonMapToGeometry(Map geoJson) {
        String json = (geoJson as JSON).toString()
        new GeometryJSON().read(json)
    }

    static Map geometryToGeoJsonMap(Geometry input) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream()
        new GeometryJSON().write(input, new OutputStreamWriter(byteOut, 'UTF-8'))

        JSON.parse(byteOut.toString('UTF-8'))
    }

    /**
     * Simplifies the geometry in the supplied map according to the specified tolerance.
     * @param geoJson
     * @param tolerance
     * @return
     */
    static Map simplify(Map geoJson, double tolerance) {

        Geometry input = geoJsonMapToGeometry(geoJson)

        Geometry result = TopologyPreservingSimplifier.simplify(input, tolerance)
        geometryToGeoJsonMap(result)
    }


    /**
     * Iterates through the supplied features and determines which features are neighbours using an
     * intersection test.  The neighbours of each feature is stored as an array of feature ids in a
     * "neighbours" property of the feature.
     */
    static void assignNeighboursToFeatures(List features, String property = "neighbours") {

        Map featureMap = [:]
        features.each { Map feature ->
            Geometry geom = geoJsonMapToGeometry(feature.geometry)
            featureMap[feature] = geom
        }

        featureMap.each { Map feature, Geometry geom ->
            feature.properties[(property)] = featureMap.findAll { Map f, Geometry otherGeom -> otherGeom != geom && otherGeom.intersects(geom) }.collect {
                it.key.properties.id
            }
        }
    }

    /**
     * Takes a list of features and assigns a numerical value to the supplied property that is not shared by
     * any of the neighbouring features.  Used to provide a property that can be used to style a map such
     * that neighbouring polygons use different colours.
     */
    static void assignDistinctValuesToNeighbouringFeatures(List features, String property) {

        int maxDistinctValues = 5

        String neighboursProperty = 'neighbours'
        // Determine features that are adjacent to each other.
        assignNeighboursToFeatures(features, neighboursProperty)

        // for each feature,  assign it the first distinct value not used by any of it's neighbours.
        features.each { Map feature ->
            List neighbourIds = feature.properties.remove(neighboursProperty)

            Set availableValues = 1..maxDistinctValues
            neighbourIds.each {String id ->
                Map neighbouringFeature = features.find{it.properties.id == id}
                if (neighbouringFeature && neighbouringFeature.properties[(property)]) {
                    availableValues.remove(neighbouringFeature.properties[(property)])
                }
            }

            Integer value = availableValues.min()
            if (!value) {
                log.info("Unable to assign a distinct value <= ${maxDistinctValues}, increasing the maximum.")
                value = ++maxDistinctValues
            }
            feature.properties[(property)] = value
        }
    }

}
