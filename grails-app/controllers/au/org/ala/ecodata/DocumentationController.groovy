package au.org.ala.ecodata

import grails.converters.JSON

class DocumentationController {

    def grailsApplication
    def metadataService, activityService

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

        def exampleActivity = exampleActivity(id)
        def activityForm = null

        if (exampleActivity) {
            activityForm = "http://fieldcapture-dev.ala.org.au/activity/enterData/${exampleActivity.activityId}?returnTo=http://fieldcapture-dev.ala.org.au/project/index/746cb3f2-1f76-3824-9e80-fa735ae5ff35"
        }
        withFormat {
            json {render schema as JSON}
            html {[name: activityModel.name, activity:schema, overview:schemaOverview(schema), example:exampleActivity, formUrl:activityForm]}
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

    def exampleActivity(activityType) {

        // Get demo data from the dev server....
        def url = 'http://ecodata-dev.ala.org.au/ws/activitiesForProject/746cb3f2-1f76-3824-9e80-fa735ae5ff35'

        def activities = get(url)
        if (!activities.error) {
            def activity = activities.list.find{it.type == activityType}
            if (activity) {
                activity.remove('documents')
                activity.remove('assessment')
                activity.remove('dateCreated')
                activity.remove('lastUpdated')
                activity.remove('projectStage')
            }
            return activity
        }
        return null
    }

    def get(String url) {
        def conn = new URL(url).openConnection()
        try {
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(50000)

            return JSON.parse(conn.content.text)
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= ${url}."]
            log.error error
            return error
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getClass()} ${e.getMessage()} URL= ${url}.",
                    statusCode: conn.responseCode?:"",
                    detail: conn.errorStream?.text]
            log.error error
            return error
        }
    }

}
