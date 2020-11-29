package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.ActivityService
import au.org.ala.ecodata.ElasticSearchService
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Output
import au.org.ala.ecodata.PermissionService
import au.org.ala.ecodata.SchemaBuilder
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.Summary
import grails.core.GrailsApplication
import graphql.schema.DataFetchingEnvironment
import org.springframework.context.MessageSource

class OutputFetcher implements graphql.schema.DataFetcher<List<Output>> {

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
}
