package au.org.ala.ecodata

import grails.converters.JSON

@PreAuthorise(basicAuth = false)
class DocumentationController {

   // def grailsApplication
    def metadataService

    def index() {

        def activitiesModel = metadataService.activitiesModel()

        // Exclude deprecated / soft deleted activities
        def activeActivities = activitiesModel.activities.findAll{!it.status || it.status == 'active'}
        def activeOutputs = []
        activitiesModel.outputs.each { output ->
            if (activeActivities.find{output.name in it.outputs}) {
                activeOutputs << output
            }
        }

        def displayModel = [activities:activeActivities, outputs:activeOutputs]

        def schemaGenerator = new SchemaBuilder(grailsApplication.config, activitiesModel)
        def outputs = [:]
        def scoresByOutput = [:]
        displayModel.outputs.each { output ->

            def outputDataModel = metadataService.getOutputDataModel(output.template)

            try {
                def schema = schemaGenerator.schemaForOutput(output.name, outputDataModel)
                outputs << [(output.name): schema]
                scoresByOutput << [(output.name):output.scores]
            }
            catch (Exception e) {
                log.error("Unable to generate documentation for output: ${output.name}", e)
            }

        }

        [activitiesModel:displayModel, outputs:outputs, scores:scoresByOutput]
    }

    def project() {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config, metadataService.activitiesModel())
        def schema = schemaGenerator.projectActivitiesSchema(metadataService.programsModel())
        withFormat {
            json {render schema as JSON}
        }
    }

    def activity(String id) {
        def activitiesModel = metadataService.activitiesModel()

        def schemaGenerator = new SchemaBuilder(grailsApplication.config, activitiesModel)
        if (!id) {
            forward index()
            return
        }

        def activityModel = activitiesModel.activities.find{it.name == id}
        def schema = schemaGenerator.schemaForActivity(activityModel)
        def simplifiedSchema = schemaOverview(schema)
        simplifiedSchema.type = id
        def exampleActivity = exampleActivity(id)

        withFormat {
            json {render schema as JSON}
            html {
                def activityForm = null

                [name: activityModel.name, activity:schema, overview:simplifiedSchema, example:exampleActivity, formUrl:activityForm]
            }
        }
    }


    def output(String id) {
        def activitiesModel = metadataService.activitiesModel()

        def schemaGenerator = new SchemaBuilder(grailsApplication.config, activitiesModel)
        if (!id) {
            forward index()
            return
        }
        def outputTemplate = metadataService.getOutputModel(id)?.template
        def outputName = metadataService.getOutputModel(id)?.name



        def outputDataModel = metadataService.getOutputDataModel(outputTemplate)

        def schema = schemaGenerator.schemaForOutput(id, outputDataModel)
        def simplifiedSchema = schemaOverview(schema)
        simplifiedSchema.name = outputName

        withFormat {
            json {render schema as JSON}
            html {
                def example = null
                if (outputName) {
                    example = exampleOutput(outputName)
                }
                [name:outputName, outputSchema: schema, overview:simplifiedSchema, example:example]
            }
        }
    }

    def postProjectActivities() {
        def schemaGenerator = new SchemaBuilder(grailsApplication.config, metadataService.activitiesModel())
        def schema = schemaGenerator.projectActivitiesSchema(metadataService.programsModel())
        [schema:schema, overview:schemaOverview(schema)]
    }

    def getProjectSites() {
        []
    }

    def schemaOverview(schema) {

        def overview = [:]
        def required = schema.required?:[]
        schema.properties.each{key, value ->
            if (value.enum || (value.type != 'object' && value.type != 'array' )) {
                def name = required.contains(key)?key+'*':key
                overview << [(name):value.type?:'string']
            }
            else if (value.type == 'object' ) {
                overview << [(key):schemaOverview(value)]
            }
            else if (value.type == 'array') {
                if (value.items.enum) {
                    overview << [(key):["string", "string..."]]
                }
                else if (value.items.anyOf || value.items.oneOf) {
                    overview << [(key):[schemaOverview(value.items)]]
                }
                else {
                    overview << [(key):[schemaOverview(value.items)]]
                }
            }
        }

        overview

    }

    def exampleActivity(activityType) {

        // Get demo data from the test server....
        def url = grailsApplication.config.ecodata.documentation.exampleProjectUrl

        def activities = doGet(url)
        if (activities && !activities.error) {
            def activity = activities.list.find{it.type == activityType}
            if (activity) {
                activity.remove('documents')
                activity.remove('assessment')
                activity.remove('dateCreated')
                activity.remove('lastUpdated')
                activity.remove('projectStage')
                activity.remove('status')
            }
            return activity
        }
        return null
    }

    def exampleOutput(outputType) {
        // Get demo data from the dev server....
        def url = grailsApplication.config.ecodata.documentation.exampleProjectUrl
        def output

        def activities = doGet(url)
        if (!activities.error) {
            activities.list.find { activity ->
                output = activity.outputs.find {it.name == outputType}
                output
            }
            if (output) {
                url = 'http://ecodata-dev.ala.org.au/ws/output/'+output.outputId
                output = doGet(url)
                if (!output.error) {
                    output.remove('id')
                    output.remove('lastUpdated')
                    output.remove('dateCreated')
                    output.remove('status')
                }
            }
        }
        output
    }

    private def doGet(String url) {
        def conn = new URL(url).openConnection()
        try {
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(50000)

            return JSON.parse(conn.content.text)
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= ${url}."]
            log.error error.toString()
            return error
        } catch (Exception e) {
            log.error e.toString()
            def error = [error:e.message]
            return error
        }
    }


}
