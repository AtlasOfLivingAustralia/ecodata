package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooInvocationContext
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooProtocol
import grails.plugin.json.view.test.JsonRenderResult
import grails.plugin.json.view.test.JsonViewTest
import grails.plugin.json.view.test.TestRequestConfigurer
import grails.testing.web.GrailsWebUnitTest
import org.bouncycastle.util.test.TestRandomBigInteger
import spock.lang.Specification

class ParatooJsonViewSpec extends Specification implements JsonViewTest {

    static List DUMMY_POLYGON = [[[1,2], [2,2], [2, 1], [1,1], [1,2]]]

    def cleanup() {
        ParatooInvocationContext.removeCurrent()
    }

    def "The /user-projects v2 response is rendered correctly"() {
        setup:
        ParatooInvocationContext context = new ParatooInvocationContext(userId:"user1", apiVersion:"v2")
        ParatooInvocationContext.setCurrent(context)
        List projectSpec = [[3, 1, false, true], [0, 0, true, false], [1, 0, false, false]]
        Map expectedResult = [
                projects: [[
                    id:"p1", name:"Project 1", grantID:"g1", protocols: [
                        [id:1, identifier: "guid-1", name: "Protocol 1", version: 1, module: "module-1", client_meta: [allow_ui_data_collection: true]],
                        [id:2, identifier: "guid-2", name: "Protocol 2", version: 1, module: "module-2", client_meta: [allow_ui_data_collection: true]],
                        [id:3, identifier: "guid-3", name: "Protocol 3", version: 1, module: "module-3", client_meta: [allow_ui_data_collection: true]]],
                    project_area:null,
                    plot_selections:[
                       [uuid:'s1', name:"Site 1"]
                    ],
                    roles:["project_admin"]
                   ],[
                    id:"p2", name:"Project 2", grantID:"g2", protocols:[], plot_selections:[],
                    project_area:[type:"Polygon", coordinates: DUMMY_POLYGON[0].collect{[lat:it[1], lng:it[0]]}],
                    roles:["authenticated"]
                  ],[
                     id:"p3", name:"Project 3", grantID:"g3", protocols:[
                        [id:1, identifier: "guid-1", name: "Protocol 1", version: 1, module: 'module-1']
                     ], project_area:null, plot_selections:[], roles:['authenticated']
                  ]
                ]]

        when: "The results of /paratoo/user-projects is rendered from an apiVersion v2 request, including the clientMeta for a write operation"

        List projects = buildProjectsForRendering(projectSpec)
        projects[1].roles = [ParatooService.EDITOR]
        projects[2].roles = [ParatooService.EDITOR]
        def result = render([view: "/paratoo/userProjects", model:[projects:projects]], {
            params(apiVersion:"v2", operationType:"write")
        })

        then:"The json is correct"
        result.json.projects.size() == expectedResult.projects.size()
        result.json.projects[0] == expectedResult.projects[0]
        result.json.projects[1] == expectedResult.projects[1]
        result.json.projects[2] == expectedResult.projects[2]

        when: "The clientMeta is not included for a read operation"
        projectSpec = [[3, 1, false, false], [0, 0, true, false], [1, 0, false, false]]
        projects = buildProjectsForRendering(projectSpec)
        projects[1].roles = [ParatooService.EDITOR]
        projects[2].roles = [ParatooService.EDITOR]
        expectedResult.projects[0].protocols.each { Map protocol ->
            protocol.remove("client_meta")
        }
        context.operationType = Permission.READ
        result = render([view: "/paratoo/userProjects", model:[projects:projects]], {
            params(apiVersion:"v2", operationType:"read")
        })

        then:"The json is correct"
        result.json.projects.size() == expectedResult.projects.size()
        result.json.projects[0] == expectedResult.projects[0]
        result.json.projects[1] == expectedResult.projects[1]
        result.json.projects[2] == expectedResult.projects[2]

        when: "The results of /paratoo/user-projects is rendered from an apiVersion v1 request"
        context.apiVersion = "v1"
        expectedResult.projects.each{ Map project ->
            List roles = project.remove('roles')
            project.role = roles[0]
        }
        result = render([view: "/paratoo/userProjects", model:[projects:projects]], {
            params(apiVersion:"v1")
        })

        then:"The json is correct"
        result.json.projects.size() == expectedResult.projects.size()
        result.json.projects[0] == expectedResult.projects[0]
        result.json.projects[1] == expectedResult.projects[1]
        result.json.projects[2] == expectedResult.projects[2]

        when: "The results of /paratoo/user-projects is rendered from a request with no apiVersion"
        context.apiVersion = null
        result = render([view: "/paratoo/userProjects", model:[projects:projects]])

        then:"The view defaults to v1 of the API"
        result.json.projects.size() == expectedResult.projects.size()
        result.json.projects[0] == expectedResult.projects[0]
        result.json.projects[1] == expectedResult.projects[1]
        result.json.projects[2] == expectedResult.projects[2]

    }

    private List<ParatooProject> buildProjectsForRendering(List projectSpec) {

        List projects = []
        projectSpec.eachWithIndex { List spec, int i ->
            projects << buildProject(i+1, spec[0], spec[1], spec[2], spec[3] )
        }
        projects
    }

    private ParatooProject buildProject(int projectIndex, int numberOfProtocols, int numberOfPlots, boolean includeProjectArea, boolean includeClientMeta) {
        List protocols = []
        for (int i = 0; i<numberOfProtocols; i++) {
            protocols << buildParatooProtocol(i+1, includeClientMeta)
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
        new ParatooProject(id:"p$projectIndex", name:"Project $projectIndex", grantID:"g$projectIndex", protocols: protocols, projectArea: projectArea, plots:plots, roles: [ParatooService.ADMIN])
    }

    private ParatooProtocol buildParatooProtocol(int i, boolean includeClientMeta = false) {
        ActivityForm activityForm = new ActivityForm(externalIds: [new ExternalId(idType:ExternalId.IdType.MONITOR_PROTOCOL_GUID, externalId:"guid-$i"), new ExternalId(idType:ExternalId.IdType.MONITOR_PROTOCOL_INTERNAL_ID, externalId:i)], name:"Protocol $i", formVersion: 1, category: "module-$i")
        new ParatooProtocol(activityForm, includeClientMeta ? [allowUIDataCollection: true] : null)
    }

    private Site buildSite(i) {
        new Site(siteId:"s$i", externalIds:[[idType:ExternalId.IdType.MONITOR_PROTOCOL_GUID, externalId:"s$i"]], name:"Site $i", extent:[geometry:[type:'Polygon', coordinates:DUMMY_POLYGON]])
    }
}
