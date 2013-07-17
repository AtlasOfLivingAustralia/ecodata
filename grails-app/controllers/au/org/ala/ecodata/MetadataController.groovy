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

    def dataModel(String id) {
        def result = metadataService.getDataModel(id)
        if (result && result.error) {
            render result as JSON
        } else {
            render result
        }
    }
}
