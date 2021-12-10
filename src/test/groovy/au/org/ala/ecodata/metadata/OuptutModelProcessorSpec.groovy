package au.org.ala.ecodata.metadata

import grails.converters.JSON
import spock.lang.Specification

/**
 * Tests the OutputModelProcessor
 */
class OuptutModelProcessorSpec extends Specification {

    private OutputModelProcessor outputModelProcessor = new OutputModelProcessor()

    /** The regevetation details output has a single table */
    void "test the revegetation details output"() {
        when:
            def regevetationDetailsMetadata = getJsonResource('revegetationDetailsMetadata')
            def flat =  outputModelProcessor.flatten(
                getJsonResource('sampleRevegetationDetails1'), new OutputMetadata(regevetationDetailsMetadata))
            def expectedNumberPlanted = [1,2]


        then:
            flat.size() == 2

            flat.eachWithIndex { flatOutput, i ->
                flatOutput.outputId == 'output1'
                flatOutput.activityId == 'activity1'
                flatOutput.numberPlanted == expectedNumberPlanted[i]
            }
    }

    void "Data models with a single nested list can be flattened"() {
        setup:
        def modelMetadata = new OutputMetadata(getJsonResource('singleNestedDataModel'))
        def modelData = getJsonResource('singleSampleNestedDataModel')

        when:
        List flat = outputModelProcessor.flatten2(modelData, modelMetadata, OutputModelProcessor.FlattenOptions.REPEAT_ALL)

        then:
        flat.size() == 2

        flat[0] == [name:"Single Nested lists", number1:"33", notes:"single notes", "list.value1":"single.0.value1"]
        flat[1] == [name:"Single Nested lists", number1:"33", notes:"single notes", "list.value1":"single.1.value1"]
    }

    void "Data models containing nested lists can be flattened"() {
        setup:
        def modelMetadata = new OutputMetadata(getJsonResource('nestedDataModel'))
        def modelData = getJsonResource('sampleNestedDataModel')

        when:
        List flat = outputModelProcessor.flatten2(modelData, modelMetadata, OutputModelProcessor.FlattenOptions.REPEAT_ALL)

        then:
        flat.size() == 5

        flat[0] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.0.value2"]
        flat[1] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.1.value2"]
        flat[2] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"1.value1", "list.nestedList.value2":"1.0.value2"]
        flat[3] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"1.value1", "list.nestedList.value2":"1.1.value2"]
        flat[4] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"1.value1", "list.nestedList.value2":"1.2.value2"]



        // list.value1, list.nestedList.value2
        // 0.value1,    0.0.value2
        // 0.value1,    0.1.value2
        // 1.value1,    1.0.value2
        // 1.value1,    1.1.value2
        // 1.value1,    1.2.value2

    }

    void "Data models containing 3 levels of nested lists can be flattened"() {
        setup:
        def modelMetadata = new OutputMetadata(getJsonResource('deeplyNestedDataModel'))
        def modelData = getJsonResource('sampleDeeplyNestedDataModel')

        when:
        List flat = outputModelProcessor.flatten2(modelData, modelMetadata, OutputModelProcessor.FlattenOptions.REPEAT_ALL)

        then:
        flat.size() == 6

        flat[0] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.0.value2", "list.nestedList.nestedNestedList.value3": "3"]
        flat[1] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.0.value2", "list.nestedList.nestedNestedList.value3": "4"]
        flat[2] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.1.value2"]
        flat[3] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"1.value1", "list.nestedList.value2":"1.0.value2"]
        flat[4] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"1.value1", "list.nestedList.value2":"1.1.value2"]
        flat[5] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"1.value1", "list.nestedList.value2":"1.2.value2"]
    }
    void "Data can be namespaced to avoid clashes with outputs containing the same names in the same activity"() {
        setup:
        def modelMetadata = new OutputMetadata(getJsonResource('deeplyNestedDataModel'))
        def modelData = getJsonResource('sampleDeeplyNestedDataModel')

        when:
        List flat = outputModelProcessor.flatten2(modelData, modelMetadata, OutputModelProcessor.FlattenOptions.REPEAT_ALL, "Test")

        then:
        flat.size() == 6

        flat[0] == ["Test.name":"Nested lists", "Test.number1":"3", "Test.notes":"notes", "Test.list.value1":"0.value1", "Test.list.nestedList.value2":"0.0.value2", "Test.list.nestedList.nestedNestedList.value3": "3"]
        flat[1] == ["Test.name":"Nested lists", "Test.number1":"3", "Test.notes":"notes", "Test.list.value1":"0.value1", "Test.list.nestedList.value2":"0.0.value2", "Test.list.nestedList.nestedNestedList.value3": "4"]
        flat[2] == ["Test.name":"Nested lists", "Test.number1":"3", "Test.notes":"notes", "Test.list.value1":"0.value1", "Test.list.nestedList.value2":"0.1.value2"]
        flat[3] == ["Test.name":"Nested lists", "Test.number1":"3", "Test.notes":"notes", "Test.list.value1":"1.value1", "Test.list.nestedList.value2":"1.0.value2"]
        flat[4] == ["Test.name":"Nested lists", "Test.number1":"3", "Test.notes":"notes", "Test.list.value1":"1.value1", "Test.list.nestedList.value2":"1.1.value2"]
        flat[5] == ["Test.name":"Nested lists", "Test.number1":"3", "Test.notes":"notes", "Test.list.value1":"1.value1", "Test.list.nestedList.value2":"1.2.value2"]
    }

    void "Data can be flattened differently depending on how data gets repeated when unrolling nested structures"() {
        setup:
        def modelMetadata = new OutputMetadata(getJsonResource('deeplyNestedDataModel'))
        def modelData = getJsonResource('sampleDeeplyNestedDataModel')

        when:
        List flat = outputModelProcessor.flatten2(modelData, modelMetadata, OutputModelProcessor.FlattenOptions.REPEAT_NONE)

        then:
        flat.size() == 6

        flat[0] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.0.value2", "list.nestedList.nestedNestedList.value3": "3"]
        flat[1] == ["list.nestedList.nestedNestedList.value3": "4"]
        flat[2] == ["list.nestedList.value2":"0.1.value2"]
        flat[3] == ["list.value1":"1.value1", "list.nestedList.value2":"1.0.value2"]
        flat[4] == ["list.nestedList.value2":"1.1.value2"]
        flat[5] == ["list.nestedList.value2":"1.2.value2"]

        when:
        flat = outputModelProcessor.flatten2(modelData, modelMetadata, OutputModelProcessor.FlattenOptions.REPEAT_SELECTIONS)

        then:
        flat.size() == 6

        flat[0] == [name:"Nested lists", number1:"3", notes:"notes", "list.value1":"0.value1", "list.nestedList.value2":"0.0.value2", "list.nestedList.nestedNestedList.value3": "3"]
        flat[1] == ["list.value1":"0.value1", "list.nestedList.value2":"0.0.value2", "list.nestedList.nestedNestedList.value3": "4"]
        flat[2] == ["list.value1":"0.value1", "list.nestedList.value2":"0.1.value2"]
        flat[3] == ["list.value1":"1.value1", "list.nestedList.value2":"1.0.value2"]
        flat[4] == ["list.value1":"1.value1", "list.nestedList.value2":"1.1.value2"]
        flat[5] == ["list.value1":"1.value1", "list.nestedList.value2":"1.2.value2"]

    }

    def "The lookupTable data type is supported"() {
        when:
        outputModelProcessor.processNode(null, ["dataType":"lookupTable"], null)

        then:
        noExceptionThrown()
    }

   private Map getJsonResource(name) {
        JSON.parse(new File("src/test/resources/${name}.json").newInputStream(),'UTF-8')
    }
}
