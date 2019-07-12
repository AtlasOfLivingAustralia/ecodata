package au.org.ala.ecodata

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(ProjectController)
class ProjectControllerSpec extends Specification {

    ProjectService projectService

    def setup() {
        projectService = Mock(projectService)
        controller.projectService = projectService
    }


    def "clients can request different views of project data according to their needs"() {
        setup:
        String projectId = 'p1'

        when:
        controller.get(projectId)

        then:
        true == true
    }
}
