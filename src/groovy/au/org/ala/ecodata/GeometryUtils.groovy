package au.org.ala.ecodata

import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.util.GeometricShapeFactory
import grails.converters.JSON
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

        GeodeticCalculator gc = new GeodeticCalculator(sourceCRS)
        Coordinate[] triangleCoords = new Coordinate[4]
        double distance = 1d/Math.sqrt(3)
        gc.setStartingGeographicPoint(point.x, point.y)
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

        return polygonToMultiPolygon(geometryFactory.createPolygon(triangleCoords))
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
        factory.size = radiusInMetres
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


}
