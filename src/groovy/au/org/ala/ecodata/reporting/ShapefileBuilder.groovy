package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.GeometryUtils
import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.SiteService
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.MultiPolygon
import grails.converters.JSON
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.geotools.data.FeatureStore
import org.geotools.data.FeatureWriter
import org.geotools.data.FileDataStoreFactorySpi
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType
import org.opengis.feature.type.FeatureType
import org.opengis.referencing.crs.CoordinateReferenceSystem

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds a shapefile from supplied projects and their associated sites.
 */
class ShapefileBuilder {

    static Log log = LogFactory.getLog(ShapefileBuilder.class)

    static CoordinateReferenceSystem DEFAULT_CRS = DefaultGeographicCRS.WGS84
    /** Attributes of each site to write to the shape file */
    static
    def DEFAULT_SITE_PROPERTIES = [[property: 'name', attribute: 'name'], [property: 'description', attribute: 'description'], [property: 'siteId', attribute:'siteId'], [property:'type', attribute:'type'], [property:'dateCreated', attribute:'dateCreated'], [property:'lastUpdated', attribute:'lastUpdated']]

    /** Attributes of each project to write to the shape file */
    static
    def DEFAULT_PROJECT_PROPERTIES = [[property: 'name', attribute: 'projectName'], [property: 'grantId', attribute: 'grantId'], [property: 'externalId', attribute: 'externalId']]

    static String GEOMETRY_ATTRIBUTE_NAME = "site"

    static String DEFAULT_NAME = "projectSites"

    /** The file extensions for the files created as a part of the shape file */
    static def fileExtensions = ['.shp', '.dbf', '.fix', '.prj', '.shx']

    ProjectService projectService
    SiteService siteService

    /** Used as the file name for each of the files in the shapefile as well as the schema name in the shapefile */
    def name = DEFAULT_NAME

    private FeatureWriter<SimpleFeatureType, SimpleFeature> writer
    private File shapefile
    private int featureCount = 0

    public ShapefileBuilder(projectService, siteService) {
        this.projectService = projectService
        this.siteService = siteService

    }

    /**
     * Sets the schema name and filename for the shapefile.  This method must be called before any
     * invocations of the addProject method.
     * @param name the name for the shapefile
     */
    public void setName(String name) {
        if (writer) {
            throw new IllegalArgumentException("The schema name must be assigned before any projects are added")
        }
        this.name = name
    }

    /**
     * Writes each of the sites for the supplied projectId into the shapefile.
     */
    public void addProject(String projectId) {

        def project = projectService.get(projectId)

        if (!project) {
            return
        }

        project.sites?.each { site ->
            addGeometry(site, project)
        }
    }

    private void addGeometry(site, project) {
        if (!writer) {
            createShapefile()
        }
        try {
            // Currently necessary as not all our sites store valid geojson as their geometry.
            def siteGeom = siteService.geometryAsGeoJson(site)
            if (siteGeom && !siteGeom.error) {

                // All geometries in a shapefile need to be of the same type, so we convert everything to
                // multi-polygons
                Geometry geom = GeometryUtils.geoGsonToMultiPolygon((siteGeom as JSON).toString())

                SimpleFeature siteFeature = writer.next()

                def attributes = [geom]
                attributes += getAttributes(site, DEFAULT_SITE_PROPERTIES)
                attributes += getAttributes(project, DEFAULT_PROJECT_PROPERTIES)

                siteFeature.setAttributes(attributes)

                try {
                    writer.write()
                    featureCount++
                }
                catch (Exception e) {
                    writer.remove()
                    featureCount--
                    log.error("Unable to write feature for site: ${site.siteId}", e)
                }

            } else {
                log.warn("Unable to get geometry for site: ${site.siteId}")
            }
        }
        catch (Exception e) {
            log.error("Error getting geomerty for site: ${site.siteId}", e)
        }
    }

    public void addSite(String siteId) {
        def site = siteService.get(siteId)
        if (!site) {
            return
        }
        site.projects?.each { project ->
            addGeometry(site, project)
        }

    }

    /**
     * Writes the zipped shape file to the supplied OutputStream.
     */
    public void writeShapefile(OutputStream toWriteTo) {
        if (featureCount > 0) {
            if (writer != null) {
                writer.close()
            }
            buildZip(shapefile, toWriteTo)

            cleanup()
        }
        else {
            throw new RuntimeException("No features have been added to the shapefile")
        }
    }

    private void createShapefile() {

        FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();

        shapefile = File.createTempFile(name, ".shp")

        ShapefileDataStore store = factory.createNewDataStore(["url": shapefile.toURI().toURL()])
        FeatureType featureType = buildTypeDef(name)

        store.createSchema(featureType);

        writer = store.getFeatureWriterAppend(((FeatureStore) store.getFeatureSource()).getTransaction())
    }

    private List getAttributes(entity, attributeDefinitions) {
        List attributes = []

        for (Map attribute : attributeDefinitions) {
            attributes << entity[attribute['property']]
        }
        return attributes
    }

    private SimpleFeatureType buildTypeDef(String name) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder()

        builder.setName(name)
        builder.setCRS(DEFAULT_CRS)
        builder.add(GEOMETRY_ATTRIBUTE_NAME, MultiPolygon.class)
        DEFAULT_SITE_PROPERTIES.each {
            builder.add(it.attribute, String.class)
        }
        DEFAULT_PROJECT_PROPERTIES.each {
            builder.add(it.attribute, String.class)
        }
        builder.setDefaultGeometry(GEOMETRY_ATTRIBUTE_NAME)
        return builder.buildFeatureType()
    }

    def buildZip(File shapeFile, outputStream) {

        ZipOutputStream zipFile = new ZipOutputStream(outputStream)

        String filename = getTempFilePrefix()
        String path = shapeFile.getParent()

        fileExtensions.each { extension ->
            File file = new File(path, filename + extension)
            zipFile.putNextEntry(new ZipEntry(name + extension))
            file.withInputStream {
                zipFile << it
            }
            zipFile.closeEntry()
        }
        zipFile.finish()

    }

    private String getTempFilePrefix() {
        return shapefile.getName().substring(0, shapefile.getName().indexOf(".shp"))
    }

    private void cleanup() {
        def filePrefix = getTempFilePrefix()
        fileExtensions.each {
            File f = new File(shapefile.getParentFile(), filePrefix+it)
            if (f.exists()) {
                f.delete()
            }

        }
    }



}
