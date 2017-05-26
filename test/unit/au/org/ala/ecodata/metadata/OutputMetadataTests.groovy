package au.org.ala.ecodata.metadata

import grails.converters.JSON
import spock.lang.Specification

/**
 * Created by Temi Varghese on 12/01/16.
 *
 * tests for OutputMetadata.groovy.
 */

class OutputMetadataTests extends Specification {

    void "test null"() {
        when:
        OutputMetadata om = new OutputMetadata();
        Map result = om.getNamesForDataType('image', null)

        then:
        result.size() == 0
    }

    void "testSimpleDataModel"() {
        when:
        Map dataModel = [dataModel: [[dataType: 'image', name: 'imageList']]]
        OutputMetadata om = new OutputMetadata(dataModel);
        Map result = om.getNamesForDataType('image', null);

        then:
        result.imageList == true
        result.size() == 1
    }

    void "testNestedDataModel"() {

        when:
        Map dataModel = [dataModel: [[dataType: 'image', name: 'imageList'],
                                     [
                                             "columns" : [
                                                     [
                                                             "dataType"   : "image",
                                                             "name"       : "speciesPhoto",
                                                             "description": "Upload photos."
                                                     ]
                                             ],
                                             "dataType": "list",
                                             "name"    : "multiSightingTable"
                                     ]]]
        OutputMetadata om = new OutputMetadata(dataModel);
        Map result = om.getNamesForDataType('image', null);

        then:
        result.imageList == true
        result.multiSightingTable.speciesPhoto == true
        result.size() == 2
    }

    void "The annotated output model should use table column headers as label when the data is in a list"() {

        when:
        List annotated = annotatedRevegetationModel()

        then:
        Map plantings = annotated.find{it.name == 'planting'}
        Map seedsSown = plantings.columns.find{it.name == 'seedSownKg'}
        seedsSown.label == 'Seed Sown (Kg):'
    }

    void "The annotated output model should use preLabels as label if it exists"() {
        when:
        List annotated = annotatedRevegetationModel()

        then:
        Map areaRevegetated = annotated.find{it.name == 'areaRevegHa'}
        areaRevegetated.label == 'Area of revegetation works (Ha):'
    }

    void "The annotated output model should use postLabels as label as a fallback for no preLabel"() {
        when:
        List annotated = annotatedRevegetationModel()

        then:
        Map postLabelTest = annotated.find{it.name == 'postLabelTest'}
        postLabelTest.label == 'Post label'
    }


    private List annotatedRevegetationModel() {
        def model = getJsonResource('revegetationDetailsMetadata')
        println model.toString(2)
        OutputMetadata outputMetadata = new OutputMetadata(model)

        List annotated = outputMetadata.annotateDataModel()
        annotated
    }

    private Map getJsonResource(name) {
        JSON.parse(getClass().getResourceAsStream("/resources/${name}.json"), 'UTF-8')
    }
}
