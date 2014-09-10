package au.org.ala.ecodata.metadata

import grails.converters.JSON
import grails.test.GrailsUnitTestCase

/**
 * Tests the OutputModelProcessor
 */
class OuptutModelProcessorTests extends GrailsUnitTestCase {

    private OutputModelProcessor outputModelProcessor = new OutputModelProcessor()

    /** The regevetation details output has a single table */
    def testRevegetationDetails() {
        def regevetationDetailsMetadata = getJsonResource('revegetationDetailsMetadata')
        def flat =  outputModelProcessor.flatten(
                getJsonResource('sampleRevegetationDetails1'), new OutputMetadata(regevetationDetailsMetadata))

        assertEquals(2, flat.size())

        def expectedNumberPlanted = [1,2]
        flat.eachWithIndex { flatOutput, i ->
            assertEquals('output1', flatOutput.outputId)
            assertEquals('activity1', flatOutput.activityId)
            assertEquals(expectedNumberPlanted[i], flatOutput.numberPlanted)
        }
    }
    /** The management practice change output has two tables */
    def testManagementPracticeChange() {

    }

    private Map getJsonResource(name) {
        JSON.parse(getClass().getResourceAsStream("/resources/${name}.json"), 'UTF-8')
    }
}
