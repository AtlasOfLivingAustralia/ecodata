package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputUploadTemplateBuilder
import grails.converters.JSON
import org.springframework.web.multipart.MultipartFile

import static au.org.ala.ecodata.Status.DELETED

@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class MetadataController {
    static responseFormats = ['json']

    def metadataService, activityService, commonService, projectService, webService
    ActivityFormService activityFormService
    def activitiesModel() {
        render metadataService.activitiesModel()
    }

    def activitiesList() {
        render metadataService.activitiesList(params.program, params.subprogram) as JSON
    }

    def activitiesListByProgram() {
        render metadataService.activitiesListByProgramId(params.programId) as JSON
    }

    def programsModel() {
        render metadataService.programsModel()
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
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
        String activityForm = params.activityForm
        Integer formVersion = params.getInt('formVersion', null)
        def annotatedModel
        if (activityForm) {
            ActivityForm form = activityFormService.findActivityForm(activityForm, formVersion)
            if (!form) {
                def result = [status:400, error:'No form with name '+activityForm+' and version '+formVersion+' was found']
                render result as JSON
                return null
            }
            annotatedModel = form.getFormSection(outputName).annotatedTemplate()
        }
        else {
            annotatedModel = metadataService.annotatedOutputDataModel(outputName)
        }

        if (!annotatedModel) {
            def result = [status:404, error:"No output of type ${outputName} exists"]
            render result as JSON
            return null
        }

        render annotatedModel as JSON
    }

    private Map getModelAndAnnotatedModel(String outputName, String activityFormName, Integer activityFormVersion, def expandList) {
        List annotatedModel
        def model
        if (activityFormName) {
            ActivityForm form = activityFormService.findActivityForm(activityFormName, activityFormVersion)
            model = form?.sections?.find{it.name == outputName}
            OutputMetadata metadata = new OutputMetadata(model?.template)
            annotatedModel = metadata.annotateDataModel()
        }
        else {
            // Legacy support
            model = metadataService.getOutputDataModelByName(outputName)
            if (expandList && expandList == 'true') {
                annotatedModel = metadataService.annotatedOutputDataModel(outputName, true)
            } else {
                annotatedModel = metadataService.annotatedOutputDataModel(outputName)
            }
        }
        return [model:model, annotatedModel:annotatedModel]
    }

    /**
     * Returns an Excel template that can be populated with output data and uploaded.
     */
    def excelOutputTemplate() {

        def outputName, listName, data, expandList
        boolean editMode, allowExtraRows, autosizeColumns, includeDataPathHeader
        String activityForm
        Integer formVersion
        def json = request.getJSON()
        if (json) {
            activityForm = json.activityForm
            formVersion = json.formVersion
            outputName = json.type
            listName = json.listName
            editMode = Boolean.valueOf(json.editMode)
            allowExtraRows = Boolean.valueOf(json.allowExtraRows)
            autosizeColumns = json.autosizeColumns != null ? Boolean.valueOf(json.autosizeColumns) : true
            includeDataPathHeader = json.includeDataPathHeader != null ? Boolean.valueOf(json.includeDataPathHeader) : false
            data = json.data ? JSON.parse(json.data) : null
            expandList = json.expandList

        }
        else {
            outputName = params.type
            listName = params.listName
            expandList = params.expandList
            editMode = params.getBoolean('editMode', false)
            allowExtraRows = params.getBoolean('allowExtraRows', false)
            autosizeColumns = params.getBoolean('autosizeColumns', true)
            includeDataPathHeader = params.getBoolean('includeDataPathHeader', false)
            activityForm = params.activityForm
            formVersion = params.getInt('formVersion', null)
        }


        if (!outputName) {
            def result = [status:400, error:'type is a required parameter']
            render result as JSON
            return null
        }

        Map modelAndAnnotatedModel = getModelAndAnnotatedModel(outputName, activityForm, formVersion, expandList)
        def model = modelAndAnnotatedModel.model
        List annotatedModel = modelAndAnnotatedModel.annotatedModel
        if (!annotatedModel) {
            def result = [status:404, error:"No output of type ${outputName} exists"]
            render result as JSON
            return null
        }

        String fileName = model.title ?: outputName

        if (listName) {
            OutputMetadata outputMetadata = new OutputMetadata([dataModel:annotatedModel])
            Map listModel = outputMetadata.findDataModelItemByName(listName)
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
                    it.path = nestedListName + '.' + it.name
                    annotatedModel.add(it)
                }
            }
            annotatedModel = annotatedModel.grep{it.dataType != 'list'}
            builder = new OutputUploadTemplateBuilder(fileName, outputName, annotatedModel, data ?: [], editMode, allowExtraRows, autosizeColumns);
            builder.additionalFieldsForDataTypes = grailsApplication.config.getProperty('additionalFieldsForDataTypes', Map)
            if(includeDataPathHeader) {
                builder.buildDataPathHeaderList()
            }
            else {
                builder.buildGroupHeaderList()
            }
        } else {
            builder = new OutputUploadTemplateBuilder(fileName, outputName, annotatedModel, data ?: [], editMode, allowExtraRows, autosizeColumns);
            builder.build()
        }

        builder.setResponseHeaders(response)

        builder.save(response.outputStream)

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

    def extractOutputDataFromActivityExcelTemplate() {

        MultipartFile file = null
        if (request.respondsTo('getFile')) {
            file = request.getFile('data')
        }
        String outputName = params.type

        if (file && outputName) {

            def data = metadataService.excelWorkbookToMap(file.inputStream, outputName, true)
            def status = 200

            def result
            if (data.error) {
                status = 500
                data.status = 500
                result = data
            }
            else if (!data.success) {
                status = 400
                result = [status:400, error:'No data was found that matched the output description identified by the type parameter, please check the template you used to upload the data. ']
            }
            else {
                result = [status: 200, data:data.success]
            }
            respond(result, status: status)
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
        render metadataService.getGeographicConfig() as JSON
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

    def getUniqueDataTypes(){
        List datatypes = metadataService.getUniqueDataTypes()
        render( text: datatypes as JSON, contentType: 'application/json')
    }

    def getIndicesForDataModels(){
        Map indices = metadataService.getIndicesForDataModels()
        render( text: indices as JSON, contentType: 'application/json')
    }

    /** Returns all Services, including associated Scores based on the forms associated with each service */
    def services() {
        render metadataService.getServiceList() as JSON
    }

    def terms(String category, String hubId) {
        respond metadataService.findTermsByCategory(category, hubId)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def deleteTerm(String termId) {
        Term term = metadataService.deleteTerm(termId)
        if (term.hasErrors()) {
            respond term.errors
        }
        else {
            Map result = [status:Status.DELETED]
            respond result
        }
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def updateTerm() {
        Map termProperties = request.JSON
        Term term = metadataService.updateTerm(termProperties)
        respond term
    }

}
