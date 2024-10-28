package au.org.ala.ecodata.spatial

import com.google.common.io.Files
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.tuple.Pair
import org.geotools.data.FileDataStore
import org.geotools.data.FileDataStoreFinder
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.data.simple.SimpleFeatureIterator
import org.geotools.data.simple.SimpleFeatureSource
import org.opengis.feature.Property
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.type.GeometryType

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
/**
 * Utilities for converting spatial data between formats
 *
 * @author ChrisF
 */
@Slf4j
@CompileStatic
class SpatialConversionUtils {
    static Pair<String, File> extractZippedShapeFile(File zippedShpFile) throws IOException {

        File tempDir = Files.createTempDir()

        // Unpack the zipped shape file into the temp directory
        ZipFile zf = null
        File shpFile = null
        try {
            zf = new ZipFile(zippedShpFile)

            boolean shpPresent = false
            boolean shxPresent = false
            boolean dbfPresent = false

            Enumeration<? extends ZipEntry> entries = zf.entries()

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement()
                InputStream inStream = zf.getInputStream(entry)
                File f = new File(tempDir, entry.getName())
                if (!f.getName().startsWith(".")) {
                    if (entry.isDirectory()) {
                        f.mkdirs()
                    } else {
                        FileOutputStream outStream = new FileOutputStream(f)
                        IOUtils.copy(inStream, outStream)

                        if (entry.getName().endsWith(".shp")) {
                            shpPresent = true
                            shpFile = f
                        } else if (entry.getName().endsWith(".shx") && !f.getName().startsWith("/")) {
                            shxPresent = true
                        } else if (entry.getName().endsWith(".dbf") && !f.getName().startsWith("/")) {
                            dbfPresent = true
                        }
                    }
                }
            }

            if (!shpPresent || !shxPresent || !dbfPresent) {
                throw new IllegalArgumentException("Invalid archive. Must contain .shp, .shx and .dbf at a minimum.")
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e)
        } finally {
            if (zf != null) {
                try {
                    zf.close()
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                }
            }
        }

        if (shpFile == null) {
            return null
        } else {
            return Pair.of(shpFile.getParentFile().getName(), shpFile)
        }
    }

    static List<List<Pair<String, Object>>> getShapeFileManifest(File shpFile) throws IOException {
        List<List<Pair<String, Object>>> manifestData = new ArrayList<List<Pair<String, Object>>>()

        FileDataStore store = FileDataStoreFinder.getDataStore(shpFile)

        SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0])
        SimpleFeatureCollection featureCollection = featureSource.getFeatures()
        SimpleFeatureIterator it = featureCollection.features()

        while (it.hasNext()) {
            SimpleFeature feature = it.next()
            List<Pair<String, Object>> pairList = new ArrayList<Pair<String, Object>>()
            for (Property prop : feature.getProperties()) {
                if (!(prop.getType() instanceof GeometryType)) {
                    Pair<String, Object> pair = Pair.of(prop.getName().toString(), feature.getAttribute(prop.getName()))
                    pairList.add(pair)
                }
            }
            manifestData.add(pairList)
        }

        return manifestData
    }
}

