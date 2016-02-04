package au.org.ala.ecodata
import com.mongodb.MongoExecutionTimeoutException
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.commons.httpclient.HttpStatus
import spock.lang.Specification
/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(SiteController)
class SiteControllerSpec extends Specification {

    SiteService siteService = Stub(SiteService);

    def setup() {
        controller.siteService = siteService
    }

    def cleanup() {
    }

    void "getImages: when site id is not passed"() {
        when:
        controller.getImages();
        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    void "getImages: when no image is returned"() {
        given:
        siteService.getImages() >> []
        when:
        params.id = '1'
        controller.getImages();
        then:
        response.status == HttpStatus.SC_OK
        response.json.size() == 0
    }

    void "getImages: when mongo throws exception"() {
        given:
        Set<String> ids = ['1']
        params.max = 5
        params.offset= 0
        siteService.getImages(ids,[:],null,'dateTaken', 'desc',5,0) >> {throw new MongoExecutionTimeoutException(123,'Cannot execute query!')}
        when:
        params.id = '1'
        controller.getImages();
        then:
        response.status == HttpStatus.SC_REQUEST_TIMEOUT
        response.text.contains('Cannot execute query!')
    }

    void "getImages: when working perfectly"() {
        given:
        Set<String> ids = ['1']
        params.max = 5
        params.offset= 0
        params.userId = 1
        siteService.getImages(ids,[:],1,'dateTaken', 'desc',5,0) >> [["siteId": "1", "name": "Rubicon Sanctuary, Port Sorell, Tasmania",
                                     "poi": [[poiId:'2',docs:[documents:[[role:'photoPoint',type:'image']],count:1]]]
                                    ]]
        when:
        params.id = '1'
        controller.getImages();
        then:
        response.status == HttpStatus.SC_OK
        response.json.size() == 1
        response.json[0].poi[0].docs.documents.size() == 1
    }
}
