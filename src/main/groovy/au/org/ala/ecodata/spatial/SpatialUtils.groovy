/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.ecodata.spatial


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.geotools.data.FileDataStore
import org.geotools.data.FileDataStoreFinder
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.data.simple.SimpleFeatureIterator
import org.geotools.data.simple.SimpleFeatureSource
import org.geotools.geometry.jts.JTS
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.referencing.CRS
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.opengis.feature.simple.SimpleFeature
import org.opengis.referencing.crs.CoordinateReferenceSystem

@CompileStatic
@Slf4j
class SpatialUtils {
    static Geometry getShapeFileFeaturesAsGeometry(File shpFileDir, String featureIndexes) throws IOException {

        if (!shpFileDir.exists() || !shpFileDir.isDirectory()) {
            throw new IllegalArgumentException("Supplied directory does not exist or is not a directory")
        }

        List<Geometry> geometries = new ArrayList<Geometry>()
        FileDataStore store = null
        SimpleFeatureIterator it = null

        try {

            File shpFile = null
            for (File f : shpFileDir.listFiles()) {
                if (f.getName().endsWith(".shp")) {
                    shpFile = f
                    break
                }
            }

            if (shpFile == null) {
                throw new IllegalArgumentException("No .shp file present in directory")
            }

            store = FileDataStoreFinder.getDataStore(shpFile)

            SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0])
            SimpleFeatureCollection featureCollection = featureSource.getFeatures()
            it = featureCollection.features()

            //transform CRS to the same as the shapefile (at least try)
            //default to 4326
            CoordinateReferenceSystem crs = null
            try {
                crs = store.getSchema().getCoordinateReferenceSystem()
                if (crs == null) {
                    //attempt to parse prj
                    try {
                        File prjFile = new File(shpFile.getPath().substring(0, shpFile.getPath().length() - 3) + "prj")
                        if (prjFile.exists()) {
                            String prj = prjFile.text

                            if (prj == "PROJCS[\"WGS_1984_Web_Mercator_Auxiliary_Sphere\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Mercator_Auxiliary_Sphere\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",0.0],PARAMETER[\"Standard_Parallel_1\",0.0],PARAMETER[\"Auxiliary_Sphere_Type\",0.0],UNIT[\"Meter\",1.0]]") {
                                //support for arcgis online default shp exports
                                crs = CRS.decode("EPSG:3857")
                            } else {
                                crs = CRS.parseWKT(prjFile.text)
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    if (crs == null) {
                        crs = DefaultGeographicCRS.WGS84
                    }
                }
            } catch (Exception ignored) {
            }

            int i = 0
            boolean all = "all".equalsIgnoreCase(featureIndexes)
            def indexes = []
            if (!all) featureIndexes.split(",").each { indexes.push(it.toInteger()) }
            while (it.hasNext()) {
                SimpleFeature feature = (SimpleFeature) it.next()
                if (all || indexes.contains(i)) {
                    geometries.add(feature.getDefaultGeometry() as Geometry)
                }
                i++
            }

            Geometry mergedGeometry

            if (geometries.size() == 1) {
                mergedGeometry = geometries.get(0)
            } else {
                GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null)
                GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(geometries)

                // note the following geometry collection may be invalid (say with overlapping polygons)
                mergedGeometry = geometryCollection.union()
            }

            try {
                return JTS.transform(mergedGeometry, CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84, true))
            } catch (Exception ignored) {
                return mergedGeometry
            }
        } catch (Exception e) {
            throw e
        } finally {
            if (it != null) {
                it.close()
            }
            if (store != null) {
                store.dispose()
            }
        }
    }
}
