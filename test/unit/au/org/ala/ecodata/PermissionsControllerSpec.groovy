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
@TestFor(PermissionsController)
class PermissionsControllerSpec extends Specification {
    PermissionService permissionService = Stub(PermissionService);

    def setup() {
        controller.permissionService = permissionService
    }

    def cleanup() {
    }

    void "canUserEditProjects: when not all parameters are passed"() {
        when:
        params.userId = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    void "canUserEditProjects: when nothing is returned"() {
        given:
        String [] projects = ['1']
        permissionService.isUserEditorForProjects('1', projects) >> [:]
        when:
        params.userId = '1'
        params.projectIds = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_OK
        response.json.size() == 0
    }

    void "canUserEditProjects: when mongo throws exception"() {
        given:
        String [] ids = ['1']
        String userId = '1'
        permissionService.isUserEditorForProjects(userId,ids) >> {throw new MongoExecutionTimeoutException(123,'Cannot execute query!')}
        when:
        params.userId = userId
        params.projectIds = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text.contains('Internal server error')
    }

    void "canUserEditProjects: when working perfectly"() {
        given:
        String [] ids = ['1']
        String userId = '1'
        permissionService.isUserEditorForProjects(userId, ids) >> ['1':true]
        when:
        params.userId = userId
        params.projectIds = '1'
        controller.canUserEditProjects();
        then:
        response.status == HttpStatus.SC_OK
        response.json['1'] == true
    }
}
