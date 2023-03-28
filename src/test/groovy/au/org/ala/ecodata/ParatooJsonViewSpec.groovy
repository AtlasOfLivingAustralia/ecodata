package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooProject
import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification

class ParatooJsonViewSpec extends Specification implements JsonViewTest {

    def "The /user-projects response is rendered correctly"() {
        setup:
        int[] projectSpec = [3, 0, 1] as int[]
        Map expectedResult = [
                projects: [[
                    id:"p1", name:"Project 1", protocols: [
                        [id: 1, name: "Protocol 1", version: 1],
                        [id: 2, name: "Protocol 2", version: 1],
                        [id: 3, name: "Protocol 3", version: 1]]
                   ],[
                    id:"p2", name:"Project 2", protocols:[]
                  ],[
                     id:"p3", name:"Project 3", protocols:[
                        [id: 1, name: "Protocol 1", version: 1]
                     ]
                  ]
                ]]

        when: "The results of /paratoo/user-projects is rendered"
        def result = render(view: "/paratoo/userProjects", model:[projects:buildProjectsForRendering(projectSpec)])

        then:"The json is correct"
        result.json.projects.size() == expectedResult.projects.size()
        result.json.projects[0] == expectedResult.projects[0]
        result.json.projects[1] == expectedResult.projects[1]
        result.json.projects[2] == expectedResult.projects[2]


    }

    private List<ParatooProject> buildProjectsForRendering(int[] projectSpec) {

        List projects = []
        for (int i=0; i<projectSpec.length; i++) {
            projects << buildProject(i+1, projectSpec[i])
        }
        projects
    }

    private ParatooProject buildProject(int projectIndex, int numberOfProtocols) {
        List protocols = []
        for (int i = 0; i<numberOfProtocols; i++) {
            protocols << buildActivityForm(i+1)
        }
        new ParatooProject(id:"p$projectIndex", name:"Project $projectIndex", protocols: protocols)
    }

    private ActivityForm buildActivityForm(int i) {
        new ActivityForm(externalId: i, name:"Protocol $i", formVersion: 1)
    }
}
