package au.org.ala.ecodata

import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class DataSetSummaryControllerSpec extends Specification implements ControllerUnitTest<DataSetSummaryController> {

    ProjectService projectService = Mock(ProjectService)
    def setup() {
        controller.projectService = projectService
    }

    def cleanup() {
    }

    void "The update method delegates to the projectService"() {
        setup:
        String projectId = 'p1'
        Map dataSetSummary = [dataSetId:'d1', name:'Data set 1']

        when:
        request.json = dataSetSummary
        controller.update(projectId)

        then:
        1 * projectService.updateDataSet(projectId, dataSetSummary) >> [status:'ok']
        response.json == ['status':'ok']

    }

    void "A project id must be specified either in the path or as part of the data set summary"() {
        setup:
        Map dataSetSummary = [dataSetId: 'd1', name: 'Data set 1']

        when:
        request.json = dataSetSummary
        controller.update()

        then:
        0 * projectService.updateDataSet(_, _)
        response.status == HttpStatus.SC_BAD_REQUEST
    }
}
