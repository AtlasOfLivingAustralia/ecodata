package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooProject
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class DataSetSummaryControllerSpec extends Specification implements ControllerUnitTest<DataSetSummaryController>, DataTest {

    ProjectService projectService = Mock(ProjectService)
    UserService userService = Mock(UserService)
    ParatooService paratooService = Mock(ParatooService)
    SiteService siteService = Mock(SiteService)

    def setup() {
        controller.projectService = projectService
        controller.userService = userService
        controller.paratooService = paratooService
        controller.siteService = siteService
        mockDomain(Project)
    }

    def cleanup() {
    }

    void "The update method delegates to the projectService"() {
        setup:
        String projectId = 'p1'
        Map dataSetSummary = [dataSetId:'d1', name:'Data set 1']

        when:
        request.method = 'POST'
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
        request.method = 'POST'
        request.json = dataSetSummary
        controller.update()

        then:
        0 * projectService.updateDataSet(_, _)
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    void "The delete method delegates to the projectService"() {
        setup:
        String projectId = 'p1'
        String dataSetSummaryId = 'd1'

        when:
        request.method = 'DELETE'
        controller.delete(projectId, dataSetSummaryId)

        then:
        1 * projectService.deleteDataSet(projectId, dataSetSummaryId) >> [status:'ok']
        response.json == ['status':'ok']
    }

    void "The bulkUpdate method delegates to the projectService"() {
        setup:
        String projectId = 'p1'
        Map postBody = [dataSets:[[dataSetId:'d1', name:'Data set 1']]]

        when:
        request.method = 'POST'
        request.json = postBody
        controller.bulkUpdate(projectId)

        then:
        1 * projectService.updateDataSets(projectId, postBody.dataSets) >> [status:'ok']
        response.json == ['status':'ok']
    }

    void "If a projectId is present in a dataSet it much match the projectId parameter in bulkUpdate"() {
        setup:
        String projectId = 'p1'
        Map postBody = [dataSets:[[dataSetId:'d1', name:'Data set 1', projectId:'p1'], [dataSetId:'d2', name:'Data set 2', projectId:'p2']]]

        when:
        request.method = 'POST'
        request.json = postBody
        controller.bulkUpdate(projectId)

        then:
        0 * projectService.updateDataSets(_, _)
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    void "The resync method submits a collection and returns success"() {
        setup:
        String projectId = 'p1'
        String dataSetId = 'd1'
        userService.currentUserDetails >> [userId: 'u1']

        Project project = new Project(projectId: projectId, name:'Project 1', custom: [dataSets: [[dataSetId: dataSetId, siteId: 's1', surveyId:[survey_metadata:[provenance:[:], survey_details:[protocol_id:'p1']]]]]])
        project.save(failOnError: true)
        ParatooProject paratooProject = new ParatooProject()
        paratooProject.project = project

        def site = [siteId: 's1']

        when:
        controller.resync(projectId, dataSetId)

        then:
        1 * paratooService.protocolWriteCheck('u1', projectId,'p1') >> true
        siteService.get('s1') >> site
        1 * projectService.canModifyDataSetSite(site, project) >> true
        1 * paratooService.paratooProjectFromProject(project, null) >> paratooProject
        paratooService.submitCollection({it.orgMintedUUID == dataSetId}, paratooProject, 'u1', true) >> null

        response.status == HttpStatus.SC_OK
        response.json.message == "Submitted request to fetch data for dataSet d1 in project p1 by user u1"
    }

    void "The resync method returns not found if project or dataset is missing"() {
        setup:
        String projectId = 'p1'
        String dataSetId = 'd1'
        def userService = Mock(UserService)
        controller.userService = userService
        userService.currentUserDetails >> [userId: 'u1']

        when:
        controller.resync(projectId, dataSetId)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.json.message == "Project not found"
    }
}
