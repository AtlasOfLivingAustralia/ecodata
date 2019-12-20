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

    void "Nested properties can be identified in the data model"() {
        setup:
        def model = getJsonResource('revegetationDetailsMetadata')
        OutputMetadata outputMetadata = new OutputMetadata(model)

        when:
        List names = outputMetadata.getNestedPropertyNames()

        then:
        names == ['planting']
    }

    void "More than one level of nesting can be identified in the data model"() {
        setup:
        def model = getJsonResource('nestedDataModel')
        OutputMetadata outputMetadata = new OutputMetadata(model)

        when:
        List names = outputMetadata.getNestedPropertyNames()

        then:
        names == ['list', 'list.nestedList']
    }

    void "The property names from the data model can be returned as a list"() {
        setup:
        def model = getJsonResource('nestedDataModel')
        OutputMetadata outputMetadata = new OutputMetadata(model)

        when:
        List names = outputMetadata.propertyNamesAsList()

        then:
        names == ['number1', 'list', 'list.value1', 'list.nestedList', 'list.nestedList.value2', 'list.afterNestedList', 'notes']
    }

    void "test model iterator"() {
        setup:
        def model = getJsonResource('nestedDataModel')
        OutputMetadata outputMetadata = new OutputMetadata(model)

        when:
        List names = []
        outputMetadata.modelIterator { path, view, data ->
            names << [path:path, view:view.type, data:data.name]
        }

        then:
        names == [[path:'number1', view:'number', data:'number1'],
                  [path:'list', view:'repeat', data:'list'],
                  [path:'list.value1', view:'text', data:'value1'],
                  [path:'list.nestedList', view:'table', data:'nestedList'],
                  [path:'list.nestedList.value2', view:'text', data:'value2'],
                  [path:'list.afterNestedList', view:'text', data:'afterNestedList'],
                  [path:'notes', view:'textarea', data:'notes']
        ]
    }

    private List annotatedRevegetationModel() {
        def model = getJsonResource('revegetationDetailsMetadata')
        OutputMetadata outputMetadata = new OutputMetadata(model)

        List annotated = outputMetadata.annotateDataModel()
        annotated
    }

    private Map getJsonResource(name) {
        JSON.parse(getClass().getResourceAsStream("/resources/${name}.json"), 'UTF-8')
    }
}
