package au.org.ala.ecodata.metadata

import grails.converters.JSON
import grails.test.GrailsUnitTestCase
import spock.lang.Specification

/**
 * Tests the OutputModelProcessor
 */
class OuptutModelProcessorTests extends Specification {

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

    private Map getJsonResource(name) {
        JSON.parse(getClass().getResourceAsStream("/resources/${name}.json"), 'UTF-8')
    }
}
