package au.org.ala.ecodata

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.LineString
import com.vividsolutions.jts.geom.MultiLineString
import com.vividsolutions.jts.geom.MultiPolygon
import com.vividsolutions.jts.geom.Point
import com.vividsolutions.jts.geom.Polygon
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import org.geotools.geojson.geom.GeometryJSON
import org.geotools.referencing.GeodeticCalculator

import java.awt.geom.Point2D
import org.geotools.referencing.CRS
import org.opengis.referencing.crs.CoordinateReferenceSystem

/**
 * Helper class for working with site geometry.
 */
class GeometryUtils {

    static CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326")
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

}
