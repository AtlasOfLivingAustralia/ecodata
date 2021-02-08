package au.org.ala.ecodata

import grails.testing.mixin.integration.Integration
import org.grails.gorm.graphql.plugin.testing.GraphQLSpec

import spock.lang.Specification

@Integration
class GraphqlIntegrationSpec extends Specification implements GraphQLSpec {

    def cleanup() {
        Project.findAll().each { it.delete(flush:true) }
    }

    def "Get project"() {
        setup:
        Project project = new Project(projectId: "graphqlProject1", name: "graphqlProject1").save(failOnError: true, flush: true)

        when:
        def resp = graphQL.graphql("""
            query{
                project(projectId:"graphqlProject1"){
                    name
                }
            }""")
        def result = resp.json

        then:
        result.data.project.name == "graphqlProject1"
    }
}
