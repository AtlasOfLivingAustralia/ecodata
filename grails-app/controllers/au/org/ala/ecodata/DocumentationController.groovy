package au.org.ala.ecodata

import grails.converters.JSON

class DocumentationController {

    def grailsApplication
    def metadataService

    /**
     * Just looked up in the configuration for now, but we may need a better approach.
     */
    def apiVersion() {
        return grailsApplication.config.app.external.api.version
    }

    def index() {

        def schemaGenerator = new SchemaBuilder(grailsApplication.config.grails.serverURL, apiVersion())
        def activitiesModel = metadataService.activitiesModel()
        def outputs = [:]
        activitiesModel.outputs.each {
            def outputDataModel = metadataService.getOutputDataModel(it.template)

            outputs << [(it.name): schemaGenerator.schemaForOutput(outputDataModel)]
        }

        [activitiesModel:activitiesModel, outputs:outputs]
    }

    def activity(String id) {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config.grails.serverURL,  apiVersion())
        if (!id) {
            forward index()
            return
        }

        def activityModel = metadataService.activitiesModel().activities.find{it.name == id}
        def schema = schemaGenerator.schemaForActivity(activityModel)
        withFormat {
            json {render schema as JSON}
            html {[name: activityModel.name, activity:schema]}
        }
    }


    def output(String id) {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config.grails.serverURL,  apiVersion())
        if (!id) {
            forward index()
            return
        }
        def outputTemplate = metadataService.getOutputModel(id)?.template

        def outputDataModel = metadataService.getOutputDataModel(outputTemplate)

        def schema = schemaGenerator.schemaForOutput(outputDataModel)
        withFormat {
            json {render schema as JSON}
            html {[name: outputDataModel.modelName, output: schema]}
        }
    }

}
