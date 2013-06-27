package au.org.ala.ecodata

import grails.converters.JSON

class MetadataController {

    def metadataService

    def activitiesModel() {
        render metadataService.activitiesModel()
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
