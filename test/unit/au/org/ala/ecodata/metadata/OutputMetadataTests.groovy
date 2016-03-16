package au.org.ala.ecodata.metadata
/**
 * Created by Temi Varghese on 12/01/16.
 *
 * tests for OutputMetadata.groovy.
 */

class OutputMetadataTests extends GroovyTestCase {

    void testNull() {
        OutputMetadata om = new OutputMetadata();
        Map result = om.getNamesForDataType('image', null)
        assert result.size() == 0
    }

    void testSimpleDataModel() {
        Map dataModel = [dataModel: [[dataType: 'image', name: 'imageList']]]
        OutputMetadata om = new OutputMetadata(dataModel);
        Map result = om.getNamesForDataType('image', null);
        assert result.imageList == true
        assert result.size() == 1
    }

    void testNestedDataModel() {
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
        assert result.imageList == true
        assert result.multiSightingTable.speciesPhoto == true
        assert result.size() == 2
    }
}
