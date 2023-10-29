package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.Site
import au.org.ala.ecodata.SiteService
import org.locationtech.jts.geom.Coordinate
import grails.converters.JSON
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.geotools.data.DataStore
import org.geotools.data.DataStoreFinder
import org.geotools.data.FeatureSource

import org.opengis.feature.Feature
import org.opengis.feature.GeometryAttribute
import org.opengis.geometry.Geometry
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Specification for the ShapefileBuilder.
 */
class ShapefileBuilderSpec extends Specification {

    def projectService = Stub(ProjectService)
    def siteService = new SiteService()

    File tempDir

    ShapefileBuilder shapefileBuilder

    def setup() {
        tempDir = File.createTempDir()
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
        shapefileBuilder = new ShapefileBuilder(projectService, siteService)
    }

    def cleanup() {
        if (tempDir.exists()) {
            tempDir.deleteDir()
        }
    }

    def "it can export projects sites as a shapefile"() {

        setup:
        projectService.get(_) >> project()

        when:
        shapefileBuilder.setName("testShapefile")
        shapefileBuilder.addProject('1234')

        File tempDir = File.createTempDir()
        File shapeOut = new File(tempDir, "testShapefile.zip")
        shapeOut.withOutputStream {
            shapefileBuilder.writeShapefile(it)
        }

        unzip(shapeOut, tempDir)
        def shapefile = new File(tempDir, "testShapefile.shp")

        then:

        shapefile.exists()
        FeatureSource features = readShapefile(shapefile)
        try {
            features.features.size() == 1

            Feature f = features.features.features().next()
            f.getProperty("name") == "Site name"
            f.getProperty('externalId') == 'External ID'
            f.getProperty('projectName') == 'Project name'
            f.getProperty('workOrderId') == '1234'

            GeometryAttribute geom = f.getDefaultGeometryProperty()
            geom.value.geometryType  == "MultiPolygon"
            Coordinate[] coordinates = geom.value.coordinates

            coordinates.length == 5
            coordinates[0].x == 137
            coordinates[0].y == -34
            coordinates[1].x == 137
            coordinates[1].y == -35
            coordinates[2].x == 136
            coordinates[2].y == -35
            coordinates[3].x == 136
            coordinates[3].y == -34
            coordinates[4].x == 137
            coordinates[4].y == -34

            features.features.features().close()
        }
        finally {
            features.getDataStore().dispose()
        }

    }

    def "it will export the features of a compound site separately"() {
        setup:
        projectService.get(_) >> projectWithCompoundSites()

        when:
        shapefileBuilder.setName("testShapefile")
        shapefileBuilder.addProject('1234')

        File tempDir = File.createTempDir()
        File shapeOut = new File(tempDir, "testShapefile.zip")
        shapeOut.withOutputStream {
            shapefileBuilder.writeShapefile(it)
        }

        unzip(shapeOut, tempDir)
        def shapefile = new File(tempDir, "testShapefile.shp")

        then:
        shapefile.exists()
        FeatureSource features = readShapefile(shapefile)
        try {
            features.features.size() == 1

            Feature f = features.features.features().next()
            f.getProperty("name") == "Site name"
            f.getProperty('externalId') == 'External ID'
            f.getProperty('projectName') == 'Project name'
            f.getProperty('workOrderId') == '1234'
            f.getProperty('featureId') == 'feature 1'
            f.getProperty('featureName') == 'feature-0-1'

            GeometryAttribute geom = f.getDefaultGeometryProperty()
            geom.value.geometryType  == "MultiPolygon"
            Coordinate[] coordinates = geom.value.coordinates

            coordinates.length == 5
            coordinates[0].x == 137
            coordinates[0].y == -34
            coordinates[1].x == 137
            coordinates[1].y == -35
            coordinates[2].x == 136
            coordinates[2].y == -35

            features.features.features().close()
        }
        finally {
            features.getDataStore().dispose()
        }
    }

    private Map project() {
        return [grantId:'Grant ID', externalId:'External ID', name:'Project name', sites:sites(), workOrderId:'1234']
    }

    private Map projectWithCompoundSites() {
        return [grantId:'Grant ID', externalId:'External ID', name:'Project name', sites:compoundSites(), workOrderId:'1234']
    }

    private List sites() {
        return [
                [name:'Site name', extent:[source:'drawn', geometry: [type:'Polygon', coordinates: [[137, -34], [137,-35], [136, -35], [136, -34], [137, -34]]]]]
        ]
    }

    private List compoundSites() {
        Map site = [
                name:'Compound site',
                type: Site.TYPE_COMPOUND,
                extent:[source:'calculated', geometry: [type:'Polygon', coordinates: [[137, -34], [137,-35], [136, -35], [136, -34], [137, -34]]]],
                features:[[
                            type:'Feature',
                            properties:[name:'feature 1', id:'feature-0-1'],
                            geometry: [
                                  type:'Point',
                                  coordinates:[137,-34]
                            ]]
                ]
        ]
        [site]
    }

    private void unzip(File zipfile, File destinationFolder) {

        int BUFFER_SIZE=1024
        zipfile.withInputStream {
            ZipInputStream zipin = new ZipInputStream(it)
            ZipEntry nextEntry = zipin.nextEntry
            while (nextEntry) {
                byte[] buff = new byte[BUFFER_SIZE]
                int offset = 0
                File entry = new File(destinationFolder, nextEntry.name)
                entry.withOutputStream { out ->
                    int numRead = zipin.read(buff, 0, buff.length)
                    while (numRead != -1) {
                        out.write(buff, 0, numRead)
                        offset += numRead
                        numRead = zipin.read(buff, 0, buff.length)
                    }
                    zipin.closeEntry()

                }
                nextEntry = zipin.nextEntry
            }
        }
    }

    private FeatureSource readShapefile(File shapefile) {

        Map<String, Serializable> map = [:]
        map.put( "url", shapefile.toURI().toURL() )

        DataStore dataStore =  DataStoreFinder.getDataStore( map )
        String typeName = dataStore.getTypeNames()[0]
        FeatureSource source = dataStore.getFeatureSource( typeName )

        return source;
    }


}
