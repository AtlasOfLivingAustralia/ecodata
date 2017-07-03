package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputUploadTemplateBuilder
import au.org.ala.web.AlaSecured
import grails.converters.JSON
import org.springframework.web.multipart.MultipartFile

import static au.org.ala.ecodata.Status.DELETED

class MetadataController {

    def metadataService, activityService, commonService, projectService, webService

    def activitiesModel() {
        render metadataService.activitiesModel()
    }

    def activitiesList() {
        render metadataService.activitiesList(params.program, params.subprogram) as JSON
    }

    @RequireApiKey
    @AlaSecured("ROLE_ADMIN")
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
    @AlaSecured("ROLE_ADMIN")
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
    @AlaSecured("ROLE_ADMIN")
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

        def outputName, listName, data, expandList
        boolean editMode, allowExtraRows, autosizeColumns
        def json = request.getJSON()
        if (json) {
            outputName = json.type
            listName = json.listName
            editMode = Boolean.valueOf(json.editMode)
            allowExtraRows = Boolean.valueOf(json.allowExtraRows)
            autosizeColumns = json.autosizeColumns != null ? Boolean.valueOf(json.autosizeColumns) : true
            data = JSON.parse(json.data)


        }
        else {
            outputName = params.type
            listName = params.listName
            expandList = params.expandList
            editMode = params.getBoolean('editMode', false)
            allowExtraRows = params.getBoolean('allowExtraRows', false)
            autosizeColumns = params.getBoolean('autosizeColumns', true)
        }


        if (!outputName) {
            def result = [status:400, error:'type is a required parameter']
            render result as JSON
            return null
        }

        def annotatedModel = null
        if (expandList && expandList == 'true') {
            annotatedModel = metadataService.annotatedOutputDataModel(outputName, true)
        } else {
            annotatedModel = metadataService.annotatedOutputDataModel(outputName)
        }
        if (!annotatedModel) {
            def result = [status:404, error:"No output of type ${outputName} exists"]
            render result as JSON
            return null
        }

        def fileName = outputName

        if (listName) {
            def listModel = annotatedModel.find { it.name == listName }
            if (listModel.description) {
                fileName += " - ${listModel.description}"
            }
            annotatedModel = listModel?.columns
        }


        OutputUploadTemplateBuilder builder
        if (expandList && expandList == 'true') {

            def listModel = annotatedModel.grep{it.dataType == 'list'}

            listModel.each {
                def nestedListName = it.name
                def listColumns = it.columns
                listColumns.each {
                    it.header = nestedListName
                    annotatedModel.add(it)
                }
            }
            annotatedModel = annotatedModel.grep{it.dataType != 'list'}
            builder = new OutputUploadTemplateBuilder(fileName, outputName, annotatedModel, data ?: [], editMode, allowExtraRows, autosizeColumns);
            builder.buildGroupHeaderList()
        } else {
            builder = new OutputUploadTemplateBuilder(fileName, outputName, annotatedModel, data ?: [], editMode, allowExtraRows, autosizeColumns);
            builder.build()
        }

        builder.setResponseHeaders(response)

        builder.save(response.outputStream)

     //   response.getOutputStream().flush();
     //   response.getOutputStream().close();

    }

    /**
     * Accepts an Excel workbook formatted as per the return value from the excelOutputTemplate method above
     * and json formatted data as it would be stored in the Output entity.
     *
     * Note that no type conversion or validation is performed.
     *
     * @param data (required) the workbook as a multipart file.
     * @param type (required) the name of the output definition that describes the data in the workbook.
     * @param listName (optional) the name of a list typed attribute in the output model.  If specified, the
     * data in the workbook will be expected to contain the contents of that list.
     *
     * @return the output data contained in the workbook formatted as JSON
     */
    def extractOutputDataFromExcelOutputTemplate() {

        MultipartFile file = null
        if (request.respondsTo('getFile')) {
            file = request.getFile('data')
        }
        String outputName = params.type
        String listName = params.listName

        if (file && outputName) {

            def data = metadataService.excelWorkbookToMap(file.inputStream, outputName, listName)

            def result
            if (!data) {
                response.status = 400
                result = [status:400, error:'No data was found that matched the output description identified by the type parameter, please check the template you used to upload the data. ']
            }
            else {
                result = [status: 200, data:data]
            }
            render result as JSON

        }
        else {
            response.status = 400
            def result = [status: 400, error:'Missing mandatory parameter(s).  Please ensure you supply the "type" and "data" parameters']

            render result as JSON
        }

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
        OutputUploadTemplateBuilder builder = new OutputUploadTemplateBuilder(outputName, outputName, model, outputData)
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

    /**
     * Returns all Scores.
     * @param view supports a "configuration" view which will return the score and it's associated configuration.
     * @return a List of all Scores
     */
    def scores() {
        List views = params.getList("view")

        List<Score> scores = Score.findAllWhereStatusNotEqual(DELETED)
        List<Map> scoreMaps = scores.collect{metadataService.toMap(it, views)}

        render scoreMaps as JSON
    }


}
