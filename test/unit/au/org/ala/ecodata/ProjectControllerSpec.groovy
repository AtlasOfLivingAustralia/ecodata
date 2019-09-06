package au.org.ala.ecodata

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(ProjectController)
@Mock(Project)
class ProjectControllerSpec1 extends Specification {

    ProjectService projectService

    def setup() {
        projectService = Mock(ProjectService)
        controller.projectService = projectService
    }


    def "clients can request different views of project data according to their needs"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save()

        when:
        controller.get(projectId)

        then:
        1 * projectService.toMap(p, [], false, null)

        when:
        params.view = 'all'
        controller.get(projectId)

        then:
        1 * projectService.toMap(p, ProjectService.ALL, false, null)
    }
}
