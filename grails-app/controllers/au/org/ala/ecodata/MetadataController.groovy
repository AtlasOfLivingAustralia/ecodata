package au.org.ala.ecodata

import grails.converters.JSON

class MetadataController {

    def metadataService, cacheService

    def activitiesModel() {
        render metadataService.activitiesModel()
    }

    def updateActivitiesModel() {
        def model = request.JSON
        //log.debug "Model=${model.getClass()}"
        metadataService.updateActivitiesModel(model.model.toString(4))
        def result = [model: metadataService.activitiesModel()]
        render result as JSON
    }

    def programsModel() {
        render metadataService.programsModel()
    }

    def updateProgramsModel() {
        def model = request.JSON
        metadataService.updateProgramsModel(model.model.toString(4))
        def result = [model: metadataService.programsModel()]
        render result as JSON
    }

    /** Returns the json data/view model for a specified output.
     *
     * @param id an output template name
     * @return json model
     */
    def dataModel(String id) {
        def result = metadataService.getOutputDataModel(id)
        if (result && result.error) {
            render result as JSON
        } else {
            render result
        }
    }

    def updateOutputDataModel(String id) {
        //log.debug "id=${id}"
        def model = request.JSON
        def modelStr = model.model.toString(4);
        //log.debug "modelStr = ${modelStr}"
        metadataService.updateOutputDataModel(modelStr, id)
        def result = [model: metadataService.getOutputDataModel(id)]
        render result as JSON
    }
}
