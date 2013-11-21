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

    def project() {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config.grails.serverURL,  apiVersion())
        def schema = schemaGenerator.projectSchema(metadataService.activitiesModel(), metadataService.programsModel())
        withFormat {
            json {render schema as JSON}
        }
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
            html {[name: activityModel.name, activity:schema, overview:schemaOverview(schema)]}
        }
    }


    def output(String id) {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config.grails.serverURL,  apiVersion())
        if (!id) {
            forward index()
            return
        }
        def outputTemplate = metadataService.getOutputModel(id)?.template
        def outputName = metadataService.getOutputModel(id)?.name


        def outputDataModel = metadataService.getOutputDataModel(outputTemplate)

        def schema = schemaGenerator.schemaForOutput(outputDataModel)
        withFormat {
            json {render schema as JSON}
            html {[name:outputName, outputSchema: schema, overview:schemaOverview(schema)]}
        }
    }

    def postProjectActivities() {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config.grails.serverURL,  apiVersion())
        def schema = schemaGenerator.projectSchema(metadataService.activitiesModel(), metadataService.programsModel())
        [schema:schema]
    }

    def getProjectSites() {

    }

    def schemaOverview(schema) {

        def overview = [:]
        schema.properties.each{key, value ->
            if (value.enum || (value.type != 'object' && value.type != 'array' )) {
                overview << [(key):value.type?:'string']
            }
            else if (value.type == 'object' ) {
                overview << [(key):schemaOverview(value)]
            }
            else if (value.type == 'array') {
                overview << [(key):[schemaOverview(value.items)]]
            }
        }

        overview

    }

}
