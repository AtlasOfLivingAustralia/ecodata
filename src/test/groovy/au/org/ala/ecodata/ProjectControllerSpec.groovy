package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class ProjectControllerSpec extends Specification implements ControllerUnitTest<ProjectController>, DomainUnitTest<Project>{

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
        1 * projectService.toMap(p, [], false, null) >> [projectId:projectId, name:"Project 1"]

        when:
        params.view = 'all'
        controller.get(projectId)

        then:
        1 * projectService.toMap(p, ProjectService.ALL, false, null) >> [projectId:projectId, name:"Project 1", activities: [activityId: "activityId1"]]
    }
}