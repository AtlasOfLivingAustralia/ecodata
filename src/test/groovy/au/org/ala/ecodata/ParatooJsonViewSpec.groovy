package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooProject
import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification

class ParatooJsonViewSpec extends Specification implements JsonViewTest {

    static List DUMMY_POLYGON = [[[1,2], [2,2], [2, 1], [1,1], [1,2]]]
    def "The /user-projects response is rendered correctly"() {
        setup:
        int[][] projectSpec = [[3, 1, 0], [0, 0, 1], [1, 0, 0]] as int[][]
        Map expectedResult = [
                projects: [[
                    id:"p1", name:"Project 1", protocols: [
                        [id: 1, name: "Protocol 1", version: 1, module: "module-1"],
                        [id: 2, name: "Protocol 2", version: 1, module: "module-2"],
                        [id: 3, name: "Protocol 3", version: 1, module: "module-3"]],
                    project_area_geo_json:null,
                    project_plots:[
                       [uuid:'s1', plot_name:"Site 1"]
                    ]
                   ],[
                    id:"p2", name:"Project 2", protocols:[], project_plots:[], project_area_geo_json:[type:"Polygon", coordinates: DUMMY_POLYGON]
                  ],[
                     id:"p3", name:"Project 3", protocols:[
                        [id: 1, name: "Protocol 1", version: 1, module: 'module-1']
                     ], project_area_geo_json:null, project_plots:[]
                  ]
                ]]

        when: "The results of /paratoo/user-projects is rendered"
        List projects = buildProjectsForRendering(projectSpec)
        def result = render(view: "/paratoo/userProjects", model:[projects:projects])

        then:"The json is correct"
        result.json.projects.size() == expectedResult.projects.size()
        result.json.projects[0] == expectedResult.projects[0]
        result.json.projects[1] == expectedResult.projects[1]
        result.json.projects[2] == expectedResult.projects[2]


    }

    private List<ParatooProject> buildProjectsForRendering(int[][] projectSpec) {

        List projects = []
        for (int i=0; i<projectSpec.length; i++) {
            projects << buildProject(i+1, projectSpec[i][0], projectSpec[i][1], projectSpec[i][2] as boolean)
        }
        projects
    }

    private ParatooProject buildProject(int projectIndex, int numberOfProtocols, int numberOfPlots, boolean includeProjectArea) {
        List protocols = []
        for (int i = 0; i<numberOfProtocols; i++) {
            protocols << buildActivityForm(i+1)
        }
        List plots = []
        for (int i=0; i<numberOfPlots; i++) {
            plots << buildSite(i+1)
        }
        Map projectArea = null
        if (includeProjectArea) {
            Site tmp = buildSite(numberOfPlots+2)
            projectArea = [type:tmp.extent.geometry.type, coordinates:tmp.extent.geometry.coordinates]
        }
        new ParatooProject(id:"p$projectIndex", name:"Project $projectIndex", protocols: protocols, projectArea: projectArea, plots:plots)
    }

    private ActivityForm buildActivityForm(int i) {
        new ActivityForm(externalId: i, name:"Protocol $i", formVersion: 1, category: "module-$i")
    }

    private Site buildSite(i) {
        new Site(siteId:"s$i", name:"Site $i", extent:[geometry:[type:'Polygon', coordinates:DUMMY_POLYGON]])
    }
}
