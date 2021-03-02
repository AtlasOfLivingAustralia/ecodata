package au.org.ala.ecodata

import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import org.grails.gorm.graphql.plugin.testing.GraphQLSpec

@Integration
class GraphqlIntegrationSpec extends GraphqlSpecHelper implements GraphQLSpec{

    HubService hubService

    def setup() {
        Map alaHubData = new JsonSlurper().parseText(getClass().getResourceAsStream("/data/alaHub.json").getText())
        hubService.create(alaHubData)
    }

    def cleanup() {
        Project.findAll().each { it.delete(flush:true) }
        Activity.findAll().each { it.delete(flush:true) }
        Output.findAll().each { it.delete(flush:true) }
    }

    def "Attempt the api as admin"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.project.name == "graphqlProject1"
    }

    def "Attempt the api as other role than admin"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }""", "test.user@ala.org.au")

        then:
        resp.statusCode.toString() == "401"
        resp.statusCode.name() == "UNAUTHORIZED"
    }

    def "Get project by project Id"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.project.name == "graphqlProject1"
    }

    def "Get project by project Id without mandatory fields"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project{
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data == null
        result.errors[0].message == "Validation error of type MissingFieldArgument: Missing field argument projectId"
    }

    def "Get project by project Id returns only the requested data"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.project.size() == 1
        result.data.project.name == "graphqlProject1"
    }

    def "Get meriplan of a project"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1", custom: [details :[description:"test"]]).save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    meriPlan
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.project.meriPlan.details != null
        result.data.project.meriPlan.details.description == "test"
    }

    def "Get full  activity detail list of a project"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)
        Activity activity = new Activity(projectId: "graphqlProject1", activityId: "activity1", type: "Project Administration").save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    activities {
                        type
                    }
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.project.activities != null
        result.data.project.activities[0].type == activity.type
    }

    def "Get specific activity details of a project"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)
        Activity activity = new Activity(projectId: "graphqlProject1", activityId: "activity1", type: "Project Administration").save(failOnError: true, flush: true)
        Output output = new Output(outputId: "output1", activityId: "activity1", name: "Administration Activities", data: [hoursAdminTotal:5]).save(failOnError: true, flush: true)

        when:
        def resp = graphqlRequest("""
            query{
                project(projectId:"graphqlProject1"){
                    Activity_ProjectAdministration{
                      OutputType_AdministrationActivities {
                        hoursAdminTotal
                      }
                    }
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.project.Activity_ProjectAdministration != null
        result.data.project.Activity_ProjectAdministration.OutputType_AdministrationActivities != null
    }

    def "Get merit projects"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchMeritProject{
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.searchMeritProject != null
    }

    def "Get merit projects with facet filters"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchMeritProject(organisation:"Test Org"){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.searchMeritProject != null
    }

    def "Get merit projects with invalid facet filters"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchMeritProject(organisation:"test"){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.searchMeritProject == null
        result.errors[0].message == "Exception while fetching data (/searchMeritProject) : Invalid organisationFacet : suggested values are : [, Test Org]"
    }

    def "Get merit projects with activity filters"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchMeritProject( activities:[{activityType:"Project Administration"}]){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data != null
    }


    def "Get biocollect projects based on hub"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchBioCollectProject(hub:"ala"){
                    projectId
                    name
                    }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.searchBioCollectProject != null
    }

    def "Get biocollect projects without hub specified"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchBioCollectProject{
                    projectId
                    name
                    }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data == null
        result.errors[0].message == "Validation error of type MissingFieldArgument: Missing field argument hub"
    }

    def "Get biocollect projects with an invalid hub"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchBioCollectProject(hub:"test"){
                    projectId
                    name
                    }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.searchBioCollectProject == null
        result.errors[0].message == "Exception while fetching data (/searchBioCollectProject) : Invalid hub, suggested values are : [ala]"
    }

    def "Get biocollect projects with invalid facet filters"() {

        when:
        def resp = graphqlRequest("""
            query{
                searchBioCollectProject(hub:"ala", organisation:"test"){
                    name
                }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.searchBioCollectProject == null
        result.errors[0].message == "Exception while fetching data (/searchBioCollectProject) : Invalid organisationFacet : suggested values are : []"
    }

    def "Get activity output dashboard data"() {
        when:
        def resp = graphqlRequest("""
            query{
              activityOutput {
                outputData {
                  category
                  outputType
                  result {
                    label
                    result
                    resultList
                    groups {
                      group
                      results {
                        count
                        result
                      }
                    }
                  }
                }
              }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.activityOutput.outputData != null
    }

    def "Get activity output dashboard data of a specific activity type"() {
        when:
        def resp = graphqlRequest("""
            query{
              activityOutput(activityOutputs: [{category: "Community Engagement and Capacity Building"}]) {
                outputData {
                  category
                  outputType
                  result {
                    label
                    result
                    resultList
                    groups {
                      group
                      results {
                        count
                        result
                      }
                    }
                  }
                }
              }
            }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data != null
    }

    def "Get output targets"() {
        when:
        def resp = graphqlRequest("""
        {
          outputTargetsByProgram {
            targets {
              program
              outputTargetMeasure {
                outputTarget
                count
                total
              }
            }
          }
        }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.outputTargetsByProgram.targets != null
    }

    def "Get output targets of a specific program"() {
        when:
        def resp = graphqlRequest("""
        {
          outputTargetsByProgram(programs: ["Reef Trust - Reef Trust Phase 1 Investment", "Reef Trust - Reef Trust Phase 5 Investment"], outputTargetMeasures: ["Tonnes per year of fine suspended sediment prevented from reaching the Great Barrier Reef Lagoon approved"]) {
            targets {
              program
              outputTargetMeasure {
                outputTarget
                count
                total
              }
            }
          }
        }""", "yasima.kankanamge@csiro.au")
        def result = resp.json

        then:
        result.data.outputTargetsByProgram.targets != null
    }

}
