package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.test.spock.IntegrationSpec

/**
 * Helper base class for integration tests.  Cleans up the database after every test by default.
 */
class IntegrationTestHelper extends IntegrationSpec {

    private boolean cleanup = true

    /** Delete everything from the database after running the tests */
    def cleanup() {
        if (cleanup) {
            Site.collection.remove(new BasicDBObject())
            Activity.collection.remove(new BasicDBObject())
            Output.collection.remove(new BasicDBObject())
            Project.collection.remove(new BasicDBObject())
            Organisation.collection.remove(new BasicDBObject())
        }
    }

    def setupPost(request, Object domainObject) {
        request.contentType = 'application/json;charset=UTF-8'
        request.method = 'POST'
        def bodyContent = convertDomainObjectToJSON(domainObject)
        request.content = bodyContent.toString().getBytes('UTF-8')
    }

    String convertDomainObjectToJSON(domainObject) {
        // Remove automatically updated properties
        def bodyContent = JSON.parse((domainObject as JSON).toString())
        bodyContent.remove('dateCreated')
        bodyContent.remove('dateUpdated')
        bodyContent.remove('class')
        return bodyContent.toString()
    }

}
