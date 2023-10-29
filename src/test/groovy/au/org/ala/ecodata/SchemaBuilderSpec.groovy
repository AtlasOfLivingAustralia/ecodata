package au.org.ala.ecodata

import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

/**
 * Tests for the SchemaBuilder class.
 */

class SchemaBuilderSpec extends Specification implements GrailsUnitTest {

    def schemaGenerator = new SchemaBuilder(grailsApplication.config, [])

    void "The schema for a text property can be generated correctly"() {
        setup:
        def input = [dataType:'text', name:'TextProperty']

        when:
        def schema = schemaGenerator.textProperty(input)

        then:
        schema == [type:'string']
    }

    void "The schema for a constrained text property can be generated correctly"() {
        setup:
        def input = [dataType:'text', name:'TextProperty', constraints:['1','2','3']]

        when:
        def schema = schemaGenerator.constrainedTextProperty(input)

        then:
        schema == [enum:['1','2','3']]
    }

    void "The schema for a stringList property can be generated correctly"() {
        setup:
        def input = [dataType:'stringList', name:'StringListProperty', constraints:['1','2','3']]

        when:
        def schema = schemaGenerator.stringListProperty(input)

        then:
        schema == [type:'array', items:[enum:['1','2','3']]]
    }

    void "The schema for a species property can be generated correctly"() {
        setup:
        def input = [dataType:'species', name:'SpeciesProperty']

        when:
        def schema = schemaGenerator.speciesProperty(input)

        then:
        schema == [type:'object', properties:[name:[type:'string'], guid:[type:'string'], listId:[type:'string']]]
    }

    void "The schema for a number property can be generated correctly"() {
        setup:
        def input = [dataType:'number', name:'NumberProperty']

        when:
        def schema = schemaGenerator.numberProperty(input)

        then:
        schema == [type:'number']
    }

    void "The schema for a date property can be generated correctly"() {
        setup:
        def input = [dataType:'date', name:'DateProperty']

        when:
        def schema = schemaGenerator.dateProperty(input)

        then:
        schema == [type:'string', format:'date-time']
    }

    def "An output schema can be generated correctly"() {
        setup:
        def input = [modelName:'Test Output', dataModel:[
                [dataType:'text', name:'TextProperty']
        ]]

        when:
        def schema = schemaGenerator.schemaForOutput(input.modelName, input)

        then:
        schema == [
                id:"http://localhost:8080/ecodata/ws/documentation/draft/output/Test%20Output",
                $schema:"http://json-schema.org/draft-04/schema#",
                type:"object",
                properties:[
                        name:[enum:["Test Output"]],
                        data:[type:"object", properties:[TextProperty:[type:"string"]]]]]

    }

    def "An output schema can be generated correctly for outputs with nested properties"() {
        setup:
        def input = [modelName:'Test Output', dataModel:[
                [dataType:'text', name:'TextProperty'],
                [dataType:'list', name:"list", columns:[
                        [dataType:"text", name:'nestedText']
                ]]
        ]]

        when:
        def schema = schemaGenerator.schemaForOutput(input.modelName, input)

        then:
        schema == [
                id:"http://localhost:8080/ecodata/ws/documentation/draft/output/Test%20Output",
                $schema:"http://json-schema.org/draft-04/schema#",
                type:"object",
                properties:[
                        name:[enum:["Test Output"]],
                        data:[type:"object", properties:[
                                TextProperty:[type:"string"],
                                list:[type:"array", items:[type:"object", oneOf:[[$ref:"#/definitions/list"]]]]]]], definitions:[list:[type:"object", properties:[nestedText:[type:"string"]]]]]


    }

    def "An output schema can be generated correctly for outputs with deeply nested properties"() {
        setup:
        def input = [modelName:'Test Output', dataModel:[
                [dataType:'text', name:'TextProperty'],
                [dataType:'list', name:"list", columns:[
                        [dataType:"text", name:'nestedText'],
                        [dataType:"list", name:"nestedList", columns:[
                                [dataType:'text', name:"nestedNestedText"]
                        ]]
                ]]
        ]]

        when:
        def schema = schemaGenerator.schemaForOutput(input.modelName, input)

        then:
        schema == [
                id:"http://localhost:8080/ecodata/ws/documentation/draft/output/Test%20Output",
                $schema:"http://json-schema.org/draft-04/schema#",
                type:"object",
                properties:[
                        name:[enum:["Test Output"]],
                        data:[type:"object", properties:[
                                TextProperty:[type:"string"],
                                list:[type:"array", items:[type:"object", oneOf:[[$ref:"#/definitions/list"]]]]]]],
                definitions:[list:[type:"object", properties:[
                        nestedText:[type:"string"],
                        nestedList:[type:"array", items:[type:"object", oneOf:[[$ref:"#/definitions/nestedList"]]]]]],
                             nestedList:[type:"object", properties:[nestedNestedText:[type:"string"]]]]]


    }
}
