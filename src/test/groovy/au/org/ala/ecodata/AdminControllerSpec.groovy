package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooInvocationContext
import au.org.ala.ecodata.paratoo.ParatooProject
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class AdminControllerSpec extends Specification implements ControllerUnitTest<AdminController> {

    ParatooService paratooService = Mock(ParatooService)
    UserService userService = Mock(UserService)

    def setup() {
        controller.paratooService = paratooService
        controller.userService = userService
        ParatooInvocationContext.removeCurrent()
    }

    def cleanup() {
        ParatooInvocationContext.removeCurrent()
    }

    void "reSubmitDataSet submits the dataset for the supplied user"() {
        setup:
        String projectId = 'p1'
        String dataSetId = 'd1'
        String userId = 'u1'
        ParatooProject project = new ParatooProject(project: new Project(projectId: projectId))

        when:
        params.id = projectId
        params.dataSetId = dataSetId
        params.userId = userId
        controller.reSubmitDataSet()

        then:
        0 * userService._
        1 * paratooService.userProjects() >> {
            assert ParatooInvocationContext.current?.userId == userId
            assert ParatooInvocationContext.current?.operationType == Permission.WRITE
            assert ParatooInvocationContext.current?.apiVersion == 'v2'
            [project]
        }
        1 * paratooService.submitCollection({ it.orgMintedUUID == dataSetId && it.coreProvenance == [:] }, project, userId)

        and:
        response.status == HttpStatus.SC_OK
        response.json.message == "Submitted request to fetch data for dataSet d1 in project p1 by user u1"
        ParatooInvocationContext.current == null
    }

    void "reSubmitDataSet returns bad request when required params are missing"() {
        when:
        params.id = 'p1'
        controller.reSubmitDataSet()

        then:
        0 * userService._
        0 * paratooService._

        and:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.json.message == 'Bad request'
        ParatooInvocationContext.current == null
    }

    void "reSubmitDataSet returns not found when the project is unavailable"() {
        setup:
        String projectId = 'p1'
        String dataSetId = 'd1'
        String userId = 'u1'

        when:
        params.id = projectId
        params.dataSetId = dataSetId
        params.userId = userId
        controller.reSubmitDataSet()

        then:
        0 * userService._
        1 * paratooService.userProjects() >> [new ParatooProject(project: new Project(projectId: 'other-project'))]
        0 * paratooService.submitCollection(_, _, _)

        and:
        response.status == HttpStatus.SC_NOT_FOUND
        response.json.message == 'Project not found'
        ParatooInvocationContext.current == null
    }
}
