package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputUploadTemplateBuilder
import grails.converters.JSON

class MetadataController {

    def metadataService, activityService, commonService, projectService

    def activitiesModel() {
        render metadataService.activitiesModel()
    }

    def activitiesList() {
        render metadataService.activitiesList(params.program) as JSON
    }

    @RequireApiKey
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

    @RequireApiKey
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

    @RequireApiKey
    def updateOutputDataModel(String id) {
        //log.debug "id=${id}"
        def model = request.JSON
        def modelStr = model.model.toString(4);
        //log.debug "modelStr = ${modelStr}"
        metadataService.updateOutputDataModel(modelStr, id)
        def result = [model: metadataService.getOutputDataModel(id)]
        render result as JSON
    }

    // Return the Nvis classes for the supplied location. This is an interim solution until the spatial portal can be fixed to handle
    // large grid files such as the NVIS grids.
    def getNvisClassesForPoint(Double lat, Double lon) {
        def result = metadataService.getNvisClassesForPoint(lat, lon)
        render result as JSON
    }


    /**
     * Attaches a label matching the form to each entry of the dataModel in the output model template for the
     * specified output.
     *
     */
    def annotatedOutputDataModel() {
        def outputName = params.type

        if (!outputName) {
            def result = [status:400, error:'type is a required parameter']
            render result as JSON
            return null
        }
        def annotatedModel = metadataService.annotatedOutputDataModel(outputName)

        if (!annotatedModel) {
            def result = [status:404, error:"No output of type ${outputName} exists"]
            render result as JSON
            return null
        }

        render annotatedModel as JSON
    }

    /**
     * Returns an Excel template that can be populated with output data and uploaded.
     */
    def excelOutputTemplate() {

        def outputName = params.type
        def listName = params.listName

        if (!outputName) {
            def result = [status:400, error:'type is a required parameter']
            render result as JSON
            return null
        }

        def annotatedModel = metadataService.annotatedOutputDataModel(outputName)

        if (!annotatedModel) {
            def result = [status:404, error:"No output of type ${outputName} exists"]
            render result as JSON
            return null
        }

        if (listName) {
            annotatedModel = annotatedModel.find { it.name == listName }?.columns
        }

        OutputUploadTemplateBuilder builder = new OutputUploadTemplateBuilder(outputName, annotatedModel);
        builder.build()
        builder.setResponseHeaders(response)

        builder.save(response.outputStream)

    }

    /**
     * Returns an Excel template with data that can be populated  and uploaded.
     */
    def excelBulkActivityTemplate() {
        def props = request.JSON
        def activityType = props?.type
        List activityIds = props?.ids?.split(',')

        if (!activityType || !activityIds)  {
            def result = [status:400, error:'type and ids are required']
            render result as JSON
            return null
        }

        def activityModel = metadataService.activitiesModel().activities.find { it.name == activityType }
        def outputModels = activityModel.outputs?.collect {
            [name:it, annotatedModel:metadataService.annotatedOutputDataModel(it), dataModel:metadataService.getDataModelFromOutputName(it)]
        }

        def activities = activityIds?.collect{act-> activityService.getAll(activityIds).find{it.activityId == act}}
        def outputData = []
        activities?.each { val ->
            def project = projectService.get(val.projectId)
            def data = [projectName: project.name,grantId:project.grantId]
            if(val.outputs?.size() > 0) {
                val.outputs?.each{ content ->
                    outputData.add( data << content.data)
                }
            }
            else {
                outputData.add(data)
            }
        }

        def model = [[name:"projectName", label:"Project Name",dataType:"text",description:"Project Description",rowHeader:true],
                     [name:"grantId", label:"Project",dataType:"text",description:"Grant Id",rowHeader:true]]
        outputModels?.first().annotatedModel.collect { model.add(it) }

        def outputName = "${outputModels?.first().name}"
        OutputUploadTemplateBuilder builder = new OutputUploadTemplateBuilder(outputName, model, outputData)
        builder.build()
        builder.setResponseHeaders(response)
        builder.save(response.outputStream)
        return null
    }

    def getLocationMetadataForPoint(double lat, double lng) {
        render metadataService.getLocationMetadataForPoint(lat, lng) as JSON
    }

    def getGeographicFacetConfig() {
        render grailsApplication.config.app.facets.geographic as JSON
    }
}
