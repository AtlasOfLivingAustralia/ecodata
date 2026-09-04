package au.org.ala.ecodata


import grails.core.GrailsApplication
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.testing.GrailsUnitTest
import spock.lang.Shared
import spock.lang.Specification

/**
 * This class initialises a MongoDatastore for use in Spock tests.
 * It is intended to be used as a base class for Spock specifications that require access to a MongoDB datastore.
 * The MongoDatastore is configured using the Grails application configuration and is intended to behave like the
 * MongoSpec in grails 6.x
 */
abstract class MongoSpec extends Specification implements GrailsUnitTest {

    @Shared
    protected MongoDatastore mongoDatastore

    void setupSpec() {
        String host = config.getProperty("grails.mongodb.host", String, "localhost")
        int port = config.getProperty("grails.mongodb.port", Integer, 27017)
        String databaseName = config.getProperty("grails.mongodb.database", String, "test")
        Map<String, Object> configuration = ['grails.mongodb.url': createConnectionString(host, port, databaseName)]
        Package[] packages = new Package[1]
        packages[0] = getClass().getPackage()
        mongoDatastore = new MongoDatastore(configuration, packages)

    }

    protected String createConnectionString(String host, int port, String databaseName) {
        "mongodb://${host}:${port as String}/${databaseName}" as String
    }
}