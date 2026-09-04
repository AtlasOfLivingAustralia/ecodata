package au.org.ala.ecodata


import org.apache.grails.testing.mongo.AbstractMongoGrailsExtension
import org.apache.grails.testing.mongo.AutoStartedMongoSpec
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.Shared

/**
 * This class is a workaround for classloading issues encountered in AutoStartedMongoSpec
 * that prevent the GORM environment from being initialised correctly.
 *
 */
abstract class ContainerisedMongoSpec extends AutoStartedMongoSpec {

    @Shared MongoDatastore mongoDatastore

    void setupSpec() {
        Map<String, Object> configuration = ['grails.mongodb.url': createConnectionString(dbContainer.getHost(), dbContainer.getMappedPort(AbstractMongoGrailsExtension.DEFAULT_MONGO_PORT))]
        List packages = getMongoPackages()
        Package[] packagesArray = packages.toArray(new Package[packages.size()])
        mongoDatastore = new MongoDatastore(configuration, packagesArray)

    }

    protected String createConnectionString(String host, int port) {
        "mongodb://${host}:${port as String}/myDb" as String
    }
}