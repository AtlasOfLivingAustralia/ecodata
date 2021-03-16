package au.org.ala.ecodata

import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import org.grails.gorm.graphql.plugin.GraphqlController
import org.springframework.beans.factory.annotation.Autowired

@Integration
class GraphqlIntegrationSpec extends GraphqlSpecHelper{

    @Autowired
    HubService hubService
    GraphqlController graphqlController
    String controllerName = "graphql"

    def setup() {
//        Hub alaHub = Hub.findByUrlPath('ala')
//        if (!alaHub) {
//            Map alaHubData = new JsonSlurper().parseText(getClass().getResourceAsStream("/data/alaHub.json").getText())
//            hubService.create(alaHubData)
//        }

        graphqlController = autowire(GraphqlController)
    }

    def cleanup() {
        Project.findAll().each { it.delete(flush:true) }
        Activity.findAll().each { it.delete(flush:true) }
        Output.findAll().each { it.delete(flush:true) }
        ManagementUnit.findAll().each { it.delete(flush:true) }
    }

    def "Get project by project Id"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.project.name == "graphqlProject1"
    }

    def "Get project by project Id without mandatory fields"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                project{
                    name
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data == null
        result.errors[0].message == "Validation error of type MissingFieldArgument: Missing field argument projectId"
    }

    def "Get project by project Id returns only the requested data"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.project.size() == 1
        result.data.project.name == "graphqlProject1"
    }

    def "Get meriplan of a project"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1", custom: [details :[description:"test"]]).save(failOnError: true, flush: true)

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                project(projectId:"graphqlProject1"){
                    meriPlan
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.project.meriPlan.details != null
        result.data.project.meriPlan.details.description == "test"
    }

    def "Get full  activity detail list of a project"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)
        Activity activity = new Activity(projectId: "graphqlProject1", activityId: "activity1", type: "Project Administration").save(failOnError: true, flush: true)

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                project(projectId:"graphqlProject1"){
                    activities {
                        type
                    }
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.project.activities != null
        result.data.project.activities[0].type == activity.type
    }

//    def "Get specific activity details of a project"() {
//        setup:
//        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)
//        Activity activity = new Activity(projectId: "graphqlProject1", activityId: "activity1", type: "Project Administration").save(failOnError: true, flush: true)
//        Output output = new Output(outputId: "output1", activityId: "activity1", name: "Administration Activities", data: [hoursAdminTotal:5]).save(failOnError: true, flush: true)
//
//        when:
//        graphqlController.request.contentType = 'application/graphql'
//        graphqlController.request.method = 'POST'
//        def bodyContent = """
//            query{
//                project(projectId:"graphqlProject1"){
//                    Activity_ProjectAdministration{
//                      OutputType_AdministrationActivities {
//                        hoursAdminTotal
//                      }
//                    }
//                }
//            }"""
//        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
//        def result = graphqlController.index()
//
//        then:
//        result.data.project.Activity_ProjectAdministration != null
//        result.data.project.Activity_ProjectAdministration.OutputType_AdministrationActivities != null
//    }

//    def "Get merit projects"() {
//
//        when:
//        graphqlController.request.contentType = 'application/graphql'
//        graphqlController.request.method = 'POST'
//        def bodyContent = """
//            query{
//                searchMeritProject{
//                    name
//                }
//            }"""
//        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
//        def result = graphqlController.index()
//
//        then:
//        result.data.searchMeritProject != null
//    }

//    def "Get merit projects with facet filters"() {
//
//        when:
//        graphqlController.request.contentType = 'application/graphql'
//        graphqlController.request.method = 'POST'
//        def bodyContent = """
//            query{
//                searchMeritProject(organisation:"Test Org"){
//                    name
//                }
//            }"""
//        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
//        def result = graphqlController.index()
//
//        then:
//        result.data.searchMeritProject != null
//    }

    def "Get merit projects with invalid facet filters"() {

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                searchMeritProject(organisation:"test"){
                    name
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.searchMeritProject == null
        result.errors[0].message == "Exception while fetching data (/searchMeritProject) : Invalid organisationFacet : suggested values are : [, Test Org]"
    }

    def "Get merit projects with activity filters"() {

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                searchMeritProject( activities:[{activityType:"Project Administration"}]){
                    name
                }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data != null
    }


//    def "Get biocollect projects based on hub"() {
//
//        when:
//        graphqlController.request.contentType = 'application/graphql'
//        graphqlController.request.method = 'POST'
//        def bodyContent = """
//            query{
//                searchBioCollectProject(hub:"ala"){
//                    projectId
//                    name
//                    }
//            }"""
//        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
//        def result = graphqlController.index()
//
//        then:
//        result.data.searchBioCollectProject != null
//    }

    def "Get biocollect projects without hub specified"() {

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                searchBioCollectProject{
                    projectId
                    name
                    }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data == null
        result.errors[0].message == "Validation error of type MissingFieldArgument: Missing field argument hub"
    }

//    def "Get biocollect projects with an invalid hub"() {
//
//        when:
//        graphqlController.request.contentType = 'application/graphql'
//        graphqlController.request.method = 'POST'
//        def bodyContent = """
//            query{
//                searchBioCollectProject(hub:"test"){
//                    projectId
//                    name
//                    }
//            }"""
//        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
//        def result = graphqlController.index()
//
//        then:
//        result.data.searchBioCollectProject == null
//        result.errors[0].message == "Exception while fetching data (/searchBioCollectProject) : Invalid hub, suggested values are : [ala]"
//    }

//    def "Get biocollect projects with invalid facet filters"() {
//
//        when:
//        graphqlController.request.contentType = 'application/graphql'
//        graphqlController.request.method = 'POST'
//        def bodyContent = """
//            query{
//                searchBioCollectProject(hub:"ala", organisation:"test"){
//                    name
//                }
//            }"""
//        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
//        def result = graphqlController.index()
//
//        then:
//        result.data.searchBioCollectProject == null
//        result.errors[0].message == "Exception while fetching data (/searchBioCollectProject) : Invalid organisationFacet : suggested values are : []"
//    }

    def "Get activity output dashboard data"() {
        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
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
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.activityOutput.outputData != null
    }

    def "Get activity output dashboard data of a specific activity type"() {
        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
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
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data != null
    }

    def "Get output targets"() {
        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
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
        }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.outputTargetsByProgram.targets != null
    }

    def "Get output targets of a specific program"() {
        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
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
        }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.outputTargetsByProgram.targets != null
    }

    def "Get management unit details"() {
        setup:
        ManagementUnit mu = new ManagementUnit(managementUnitId: "mu1", name: "mu1").save(failOnError: true, flush: true)

        when:
        graphqlController.request.contentType = 'application/graphql'
        graphqlController.request.method = 'POST'
        def bodyContent = """
            query{
                searchManagementUnits{
                    managementUnitId
                    }
            }"""
        graphqlController.request.content = bodyContent.toString().getBytes('UTF-8')
        def result = graphqlController.index()

        then:
        result.data.searchManagementUnits != null
        result.data.searchManagementUnits.size() == 1
        result.data.searchManagementUnits[0].size() == 1
        result.data.searchManagementUnits[0].managementUnitId == mu.managementUnitId
    }


}
