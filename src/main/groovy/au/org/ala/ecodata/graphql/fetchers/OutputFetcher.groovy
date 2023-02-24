package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Output
import au.org.ala.ecodata.SchemaBuilder
import au.org.ala.ecodata.Status
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.Summary
import grails.core.GrailsApplication
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.springframework.context.MessageSource

class OutputFetcher implements DataFetcher<List<Output>> {

    public OutputFetcher(MetadataService metadataService, MessageSource messageSource, GrailsApplication grailsApplication) {

        this.metadataService = metadataService
        this.messageSource = messageSource
        this.grailsApplication = grailsApplication
    }

    MetadataService metadataService
    MessageSource messageSource
    GrailsApplication grailsApplication

    List<Summary> getOutputSummaryList(DataFetchingEnvironment environment) {

        List<Summary> activityList = new ArrayList<Summary>()

        def activitiesModel = metadataService.activitiesModel()

        def activeActivities = activitiesModel.activities.findAll{!it.status || it.status == 'active'}
        activitiesModel.outputs.each { output ->
            if (activeActivities.find{output.name in it.outputs}) {
                Summary sa = new Summary(name: output.name, definition: messageSource.getMessage("api.${output.name}.description", null, "", Locale.default))
                activityList << sa
            }
        }
        return activityList
    }

    Schema getOutputByName(String name) {

        def activitiesModel = metadataService.activitiesModel()

        def schemaGenerator = new SchemaBuilder(grailsApplication.config, activitiesModel)
        if (!name) {
            return null
        }
        def outputTemplate = metadataService.getOutputModel(name)?.template

        def outputDataModel = metadataService.getOutputDataModel(outputTemplate)

        def schema = schemaGenerator.schemaForOutput(name, outputDataModel)

        Schema schemaEntity = new Schema()
        schemaEntity.type = schema.type
        schemaEntity.id = schema.id
        schemaEntity.propertyList = schema.properties
        return schemaEntity
    }

    @Override
    List<Output> get(DataFetchingEnvironment environment) {
        Output.findAll([max:10])
    }

    List<Output> getFilteredOutput(List args, String activityType = null, String activityId = null)
    {
        //validate inputs
        if(args){
            List arguments = []
            arguments.add(["outputs":args])
            new Helper(Holders.applicationContext.metadataService).validateActivityData(arguments)
        }

        List requestedOutput = []
        def outputArgs = []

        args.each {
            if(activityType && it["activityType"] == activityType && it["output"]){
                requestedOutput = it["output"]["outputType"].flatten()
            }
            else if(!activityType && it["outputType"]) {
                requestedOutput.add(it["outputType"])
            }
        }

        if(args) {
            if (activityType) {
                outputArgs = args["output"]
            } else {
                outputArgs.add(args)
            }
        }

        def outputList = Output.where {
            //get requested output types
            if(requestedOutput.size() > 0) {
                name in requestedOutput
            }
            if(activityId) {
                activityId == activityId
            }
            status != Status.DELETED
        }.each {
            if (outputArgs) {
                it.tempArgs.add(["output": outputArgs])
            }
        }.sort{it.name}.findAll()

        //remove empty outputs
        if(outputArgs) {
            outputArgs.each { y ->
                y.each {
                    if (it["fields"] && !it["fields"].contains(null)) {
                        outputList.removeAll { x ->
                            (!x.data || x.data.size() == 0) && x.name == it["outputType"]
                        }
                    }
                }
            }
        }
        return outputList
    }
}
