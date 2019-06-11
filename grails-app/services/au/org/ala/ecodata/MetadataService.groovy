package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.ProgramsModel
import au.org.ala.ecodata.reporting.XlsExporter
import grails.converters.JSON
import grails.validation.ValidationException
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellReference
import org.codehaus.groovy.grails.web.json.JSONArray
import org.grails.plugins.csv.CSVMapReader

import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class MetadataService {

    // The spatial portal returns n/a when the point does not intersect a layer.
    private static final String SPATIAL_PORTAL_NO_MATCH_VALUE = 'n/a'

    private static final int BATCH_LIMIT = 200

    private static final List IGNORE_DATA_TYPES = ['lookupByDiscreteValues', 'lookupRange']

    def grailsApplication, webService, cacheService, messageSource, excelImportService, emailService, userService, commonService

    def activitiesModel() {
        return cacheService.get('activities-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + 'activities-model.json'
            JSON.parse(new File(filename).text)
        })
    }

    /**
     * Returns a Map of activity types grouped by category.
     * @param programName If supplied, restricts the returned activities to those configured for use by the specified program
     * @param subprogramName If supplied, restricts the returned activities to those configured for use by the specified sub-program
     * @return a Map, key: String, value: List of name, description for each activity in the category
     */
    Map activitiesList(String programName = null, String subprogramName = null) {

        List activities = activitiesModel().activities

        if (programName) {
            ProgramsModel model = new ProgramsModel(programsModel())
            List supportedActivities = model.getSupportedActivityTypes(programName, subprogramName)
            if (supportedActivities) {
                activities = activities.findAll{it.name in supportedActivities}
            }
        }

        // Remove deprecated activities
        activities = activities.findAll {!it.status || it.status == ACTIVE}

        Map byCategory = [:]

        // Group by the activity category field, falling back on a default grouping of activity or assessment.
        activities.each {
            def category = it.category?: it.type == 'Activity' ? 'Activities' : 'Assessment'
            if (!byCategory[category]) {
                byCategory[category] = []
            }
            def description = messageSource.getMessage("api.${it.name}.description", null, "", Locale.default)
            byCategory[category] << [name:it.name, type:it.type, description:description]
        }
        byCategory
    }

    def programsModel() {
        return cacheService.get('programs-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + 'programs-model.json'
            JSON.parse(new File(filename).text)
        })
    }

    def programModel(program) {
        return programsModel().programs.find {it.name == program}
    }

    def getOutputModel(name) {
        return activitiesModel().outputs.find { it.name == name }
    }

    def getDataModelFromOutputName(outputName) {
        def activityName = getActivityModelName(outputName)
        return activityName ?: getOutputDataModel(activityName)
    }

    def getActivityModelName(outputName) {
        return activitiesModel().outputs.find({it.name == outputName})?.template
    }

    def getOutputDataModel(templateName) {
        return cacheService.get(templateName + '-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + templateName + '/dataModel.json'
            JSON.parse(new File(filename).text)
        })
    }

    def getOutputDataModelByName(name) {
        def outputModel = activitiesModel().outputs.find{it.name == name}
        return getOutputDataModel(outputModel?.template)
    }

    def getOutputDataByActivityName(name) {
        def outputList = activitiesModel().activities.find{it.name == name}?.outputs
        if (outputList && outputList.size() > 0) {
            return activitiesModel().outputs.grep{it.name in outputList}?.collect{getOutputDataModel(it.template)}
        }
        return null
    }

    Map getOutputNameAndDataModelForAnActivityName(name) {
        def outputList = activitiesModel().activities.find { it.name == name }?.outputs
        if (outputList && outputList.size() > 0) {
            return activitiesModel().outputs.grep { it.name in outputList }?.collectEntries { [(it.name): getOutputDataModel(it.template)] }
        }
        return null
    }
    def updateOutputDataModel(model, templateName) {
        Map isDataModelValid = isDataModelValid(model)
        if(isDataModelValid.valid){
            log.debug "updating template name = ${templateName}"
            writeWithBackup(model, grailsApplication.config.app.external.model.dir, templateName, 'dataModel', 'json')
            // make sure it gets reloaded
            clearCacheForTemplate(templateName)
            String bodyText = "The output data model ${model} has been edited by ${userService.currentUserDisplayName?: 'an unknown user'} on the ${grailsApplication.config.grails.serverURL} server"
            emailService.emailSupport("Output model updated in ${grailsApplication.config.grails.serverURL}", bodyText)
            [error: false]
        } else {
            [
                    message: "Error! Model is not valid. The following indexes are at fault - ${isDataModelValid?.errorInIndex?.join(',')}"
                    , error: true
            ]
        }
    }

    def clearCacheForTemplate(String templateName){
        cacheService.clear(templateName + '-model')
        cacheService.clear("indices-for-data-models")
    }

    def getModelName(output, type) {
        return output.template ?: getModelNameFromType(type)
    }

    def getModelNameFromType(type) {
        //log.debug "Getting model name for ${type}"
        return activitiesModel().find({it.name == type})?.template
    }

    def getInstitutionName(uid) {
        return uid ? institutionList().find({ it.uid == uid })?.name : ''
    }

    def institutionList() {
        return cacheService.get('institutions',{
            webService.getJson(grailsApplication.config.collectory.baseURL + 'ws/institution')
        })
    }

    /**
     * Returns the institution from the institutionList() that matches the supplied
     * name (using a case-insensitive match).
     * @param name the name of the institution to find.
     * @return the institution with the supplied name, or null if it cannot be found.
     */
    def findInstitutionByName(String name) {
        def lowerCaseName = name.toLowerCase()
        return institutionList().find{ it.name.toLowerCase() == lowerCaseName }
    }

    def writeWithBackup(content, modelPathRoot, path, filename, extension) {
        /* build path creating directories as required */
        // assume root ends with a path separator
        String filePath = modelPathRoot + path + (path.endsWith('/') ? '' : '/')
        // get new or existing file
        def f = new File(filePath + filename + '.' + extension)

        // create dirs as required
        new File(f.getParentFile().getAbsolutePath()).mkdirs()

        if (f.exists()) {
            // create a backup of the file appending the current timestamp to the name
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            def backupFilename = filePath + filename + "-" + sdf.format(date) + '.' + extension
            f.renameTo(new File(backupFilename))
        }

        // write the new content
        f.withWriter { out ->
            use(OrderedJSONAttributePrinter) {
                out.write content as String
            }
        }
    }

    def updateActivitiesModel(model) {
        writeWithBackup(model, grailsApplication.config.app.external.model.dir, '', 'activities-model', 'json')
        // make sure it gets reloaded
        cacheService.clear('activities-model')
        String bodyText = "The activities-model has been edited by ${userService.currentUserDisplayName?: 'an unknown user'} on the ${grailsApplication.config.grails.serverURL} server"
        emailService.emailSupport("Activities model updated in ${grailsApplication.config.grails.serverURL}", bodyText)
    }

    def updateProgramsModel(model) {
        writeWithBackup(model, grailsApplication.config.app.external.model.dir, '', 'programs-model', 'json')
        // make sure it gets reloaded
        cacheService.clear('programs-model')
        String bodyText = "The programs-model has been edited by ${userService.currentUserDisplayName?: 'an unknown user'} on the ${grailsApplication.config.grails.serverURL} server"
        emailService.emailSupport("Program model updated in ${grailsApplication.config.grails.serverURL}", bodyText)
    }

    // Return the Nvis classes for the supplied location. This is an interim solution until the spatial portal can be fixed to handle
    // large grid files such as the NVIS grids.
    def getNvisClassesForPoint(Double lat, Double lon) {
        def retMap = [:]

        def nvisLayers = grailsApplication.config.app.facets.geographic.special

        nvisLayers.each { name, path ->
            def classesJsonFile = new File(path + '.json')
            if (classesJsonFile.exists()) { // The files are too big for the development system
                def classesJson = classesJsonFile.text
                def classesMap = JSON.parse(classesJson)

                BasicGridIntersector intersector = null
                try {
                    intersector = new BasicGridIntersector(path)
                    def classNumber = intersector.readCell(lon, lat)
                    retMap.put(name, classesMap[classNumber.toInteger().toString()])
                } catch (IllegalArgumentException ex) {
                    // Lat long was outside extent of grid
                    retMap.put(name, null)
                }
                finally {
                    if (intersector != null) {
                        intersector.close()
                    }
                }
            }
            else {
                retMap << [(name) : null]
            }

        }

        return retMap
    }

    /**
     * Attaches a label matching the form to each entry of the dataModel in the output model template for the
     * specified output.
     * @param outputName identifies the output to annotate.
     */
    def annotatedOutputDataModel(outputName, boolean expandList = false) {
        def outputMetadata = null
        def annotatedModel = []
        if (expandList) {
            outputMetadata = getOutputDataByActivityName(outputName)
            outputMetadata?.each { it ->
                OutputMetadata metadata = new OutputMetadata(it)
                metadata.annotateDataModel().each {annotatedModel.add(it)}
            }

        } else {
            outputMetadata = getOutputDataModelByName(outputName)
            if (outputMetadata) {
                OutputMetadata metadata = new OutputMetadata(outputMetadata)
                annotatedModel = metadata.annotateDataModel()
            }
        }
        annotatedModel
    }


    /**
     * This method produces the location metadata for a point, used in particular to provide the geographic facet terms
     * for a site.  This is done by intersecting the site centroid against a configured set of spatial portal layers
     * and storing the results against attributes that are indexed specifically for faceting.
     * @param lat the latitude of the point.
     * @param lng the longitude of the point.
     * @return metadata for the point.
     */
    def getLocationMetadataForPoint(lat, lng) {

        def features = performLayerIntersect(lat, lng)

        def localityUrl = grailsApplication.config.google.geocode.url + "${lat},${lng}"
        def result = webService.getJson(localityUrl)
        def localityValue = (result?.results && result.results)?result.results[0].formatted_address:''
        features << [locality: localityValue]

        // Return the Nvis classes for the supplied location. This is an interim solution until the spatial portal can be fixed to handle
        // large grid files such as the NVIS grids.
        features << getNvisClassesForPoint(lat as Double, lng as Double)

        features
    }

    /**
     * Reads the facet configuration and intersects the supplied point against the defined facets.
     * @param lat the latitude of the point.
     * @param lng the longitude of the point.
     * @return metadata for the point obtained from the spatial portal.
     */
    def performLayerIntersect(lat,lng) {


        def contextualLayers = grailsApplication.config.app.facets.geographic.contextual
        def groupedFacets = grailsApplication.config.app.facets.geographic.grouped

        // Extract all of the layer field ids from the facet configuration so we can make a single web service call to the spatial portal.
        def fieldIds = contextualLayers.collect { k, v -> v }
        groupedFacets.each { k, v ->
            fieldIds.addAll(v.collect { k1, v1 -> v1 })
        }

        // Do the intersect
        def featuresUrl = grailsApplication.config.spatial.intersectUrl + "${fieldIds.join(',')}/${lat}/${lng}"
        def features = webService.getJson(featuresUrl)

        def facetTerms = [:]

        if (features instanceof List) {
            contextualLayers.each { name, fid ->
                def match = features.find { it.field == fid }
                if (match && match.value && !SPATIAL_PORTAL_NO_MATCH_VALUE.equalsIgnoreCase(match.value)) {
                    facetTerms << [(name): match.value]
                }
                else {
                    facetTerms << [(name): null]
                }
            }

            groupedFacets.each { group, layers ->
                def groupTerms = []
                layers.each { name, fid ->
                    def match = features.find { it.field == fid }
                    if (match && match.value && !SPATIAL_PORTAL_NO_MATCH_VALUE.equalsIgnoreCase(match.value)) {
                        groupTerms << match.layername
                    }
                }
                facetTerms << [(group): groupTerms]
            }
        }
        else {
            log.warn("Error performing intersect for lat=${lat} lng=${lng}, result=${features}")
        }

        facetTerms
    }

     private def buildPointsArray(sites) {

        def points = ""
        def pointsArray = []

        sites?.eachWithIndex { site, i ->
            if(points){
                points = "${points},${site.extent.geometry.centre[1]},${site.extent.geometry.centre[0]}"
            }
            else{
                points = "${site.extent.geometry.centre[1]},${site.extent.geometry.centre[0]}"
            }
            if(((i+1) % BATCH_LIMIT) == 0){
                pointsArray.add(points)
                points = ""
            }
        }
        if(points){
            pointsArray.add(points)
        }

        pointsArray
    }

    /** Returns a list of spatial portal layer/field ids that ecodata will intersect every site against to support facetted geographic searches */
    List<String> getSpatialLayerIdsToIntersect() {
        def contextualLayers = grailsApplication.config.app.facets.geographic.contextual
        def groupedFacets = grailsApplication.config.app.facets.geographic.grouped
        def fieldIds = contextualLayers.collect { k, v -> v }
        groupedFacets.each { k, v ->
            fieldIds.addAll(v.collect { k1, v1 -> v1 })
        }
        fieldIds
    }

    /**
     * Returns a map of the form [grouped:Boolean, name:String] describing the configuration for the facet that
     * uses the supplied field id.
     * @param fid the field id.
     */
    Map getGeographicFacetConfig(String fid) {
        Map config = grailsApplication.config.app.facets.geographic
        Map facetConfig = null
        config.contextual.each { String groupName, String groupFid ->
            if (fid == groupFid) {
                facetConfig = [grouped:false, name:groupName]
                return false
            }
        }
        if (!facetConfig) {
            config.grouped.each { String groupName, Map<String, String> layers ->
                if (layers.values().contains(fid)) {
                    facetConfig = [grouped:true, name:groupName]
                    return false
                }
            }
        }
        if (!facetConfig) {
            throw new IllegalArgumentException("No configuration for field id: ${fid}")
        }
        facetConfig

    }

    /**
     * Download spatial layers.
     * Spatial service api: http://spatial.ala.org.au/ws/
     * Example format: http://spatial-dev.ala.org.au/ws/intersect/batch?fids=cl958,cl927&points=-29.911,132.769,-20.911,122.769
     * @param list of available sites.
     * @return raw spatial data
     */
    private def downloadSpatialLayers(sites){

        def pointsArray = buildPointsArray(sites)

        def fieldIds = getSpatialLayerIdsToIntersect()

        def results = []

        for(int i = 0; i < pointsArray?.size(); i++) {
            log.info("${(i+1)}/${pointsArray.size()} batch process started..")

            def featuresUrl = grailsApplication.config.spatial.intersectBatchUrl + "?fids=${fieldIds.join(',')}&points=${pointsArray[i]}"
            def status = webService.getJsonRepeat(featuresUrl)
            if(status?.error){
                throw new Exception("Webservice error, failed to get JSON after 12 tries.. - ${status}")
            }

            def download, timeout = 0
            while ( !(download = webService.getJson(status?.statusUrl))?.status?.equals("finished") && timeout < 12){ // break out after 1 min
                sleep(5000)
                timeout++
                log.info("${(i+1)}/${pointsArray.size()} - In the waiting queue, trying again..")
            }
            if(download?.error || timeout >= 12){
                log.info("${(i+1)}/${pointsArray.size()} - failed after 12 tries..")
                throw new Exception("Webservice error, failed to get JSON - ${download}")
            }

            URL downloadURL = new URL(download?.downloadUrl)
            ZipInputStream zipIn = new ZipInputStream(downloadURL?.openStream());
            ZipEntry entry;

            StringBuilder s = new StringBuilder();
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((entry = zipIn?.getNextEntry()) !=  null) {
                while ((read = zipIn.read(buffer, 0, 1024)) >= 0) {
                    s.append(new String(buffer, 0, read));
                }
                results += new CSVMapReader(new StringReader(s.toString())).readAll()
            }

            log.info("${(i+1)}/${pointsArray.size()} batch process completed..")
        }

        results
    }

    private def getValidSites(allSites){

        def sites = []

        log.info("Total sites = ${allSites?.size()}")

        allSites.each{ site ->
            if (!site.projects) {
                log.info("Ignoring site ${site.siteId} due to no associated projects")
                return
            }
            def centroid = site.extent?.geometry?.centre
            if (centroid && centroid.size() == 2) {
                sites.add(site)
            }
            else {
                log.error("Unable to update metadata for site: ${site.siteId}, no centroid exists.")
            }
        }

        log.info("Total sites with valid points = ${sites.size()}")

        sites
    }

    private def getGridAndFacetLayers(layers,lat,lng){

        def siteResult = layers.find{it['latitude'] == (lat as String) && it['longitude'] == (lng as String)} ?: [:]
        if (!siteResult) {
            log.error("Missing result for ${lat}, ${lng}")
        }

        def contextualLayers = grailsApplication.config.app.facets.geographic.contextual
        def groupedFacets = grailsApplication.config.app.facets.geographic.grouped
        def facetTerms = [:]

        contextualLayers.each { name, fid ->
            def match = siteResult[fid]
            if (match && !SPATIAL_PORTAL_NO_MATCH_VALUE.equalsIgnoreCase(match)) {
                facetTerms << [(name): match]
            }
            else {
                facetTerms << [(name): null]
            }
        }

        groupedFacets.each { group, entry ->
            def groupTerms = []
            entry.each { name, fid ->
                def match = siteResult[fid]
                if (match && !SPATIAL_PORTAL_NO_MATCH_VALUE.equalsIgnoreCase(match)) {
                    groupTerms << match
                }
            }
            facetTerms << [(group): groupTerms]
        }

        facetTerms
    }

    /**
     * Updates sites extent properties from the values obtained from
     * 1. Spatial server for layer information.
     * 2. Google server for location and
     * 3. NVIS classes info.
     * These data's are used for geographic facet terms for a site.
     *
     * @param list of available sites.
     * @return sites with the updated extent values.
     */
    def getLocationMetadataForSites(List allSites, boolean includeLocality = true) {

        def sites = getValidSites(allSites)

        def layers = downloadSpatialLayers(sites)

        log.info("Initiating extent mapping")

        sites.eachWithIndex { site, index ->

            def lat = site.extent.geometry.centre[1]
            def lng = site.extent.geometry.centre[0]

            def features = [:]
            if (includeLocality) {
                def localityUrl = grailsApplication.config.google.geocode.url + "${lat},${lng}"
                def result = webService.getJson(localityUrl)
                def localityValue = (result?.results && result.results) ? result.results[0].formatted_address : ''
                features << [locality: localityValue]
            }
            features << getNvisClassesForPoint(lat as Double, lng as Double)
            features << getGridAndFacetLayers(layers,lat,lng)

            site.extent.geometry.putAll(features)
            if(index > 0 && (index % BATCH_LIMIT) == 0){
                log.info("Completed (${index+1}) extent mapping")
            }
        }

        log.info("Completed batch processing and site extent mapping..")

        sites
    }

    /**
     * Accepts an Excel workbook containing output data and returns a Map containing output data formatted
     * as it would be stored in the Output entity.
     *
     * Note that no type conversion or validation is performed.
     *
     * @param excelWorkbookIn an InputStream containing the contents of the Excel workbook.
     * @param outputName the name of the output definition that describes the data in the workbook.
     * @param listName (optional) the name of a list typed attribute in the output model.  If specified, the
     * data in the workbook will be expected to contain the contents of that list.
     *
     * @return the output data contained in the workbook in a Map.
     */
    List<Map> excelWorkbookToMap(InputStream excelWorkbookIn, String outputName, String listName = null) {
        List model = annotatedOutputDataModel(outputName)
        if (listName) {
            model = model.find { it.name == listName }?.columns
        }
        int index = 0;
        def columnMap = model.collectEntries {
            def colString = CellReference.convertNumToColString(index++)
            [(colString):it.name]
        }
        def config = [
                sheet:XlsExporter.sheetName(outputName),
                startRow:1,
                columnMap:columnMap
        ]
        Workbook workbook = WorkbookFactory.create(excelWorkbookIn)

        excelImportService.convertColumnMapConfigManyRows(workbook, config)

    }

    /**
     * Converts a Score domain object to a Map.
     * @param score the Score to convert.
     * @param views specifies the data to include in the Map.  Only current supported value is "configuration",
     * which will return the score and it's associated configuration.
     *
     */
    Map toMap(Score score, List views) {
        Map scoreMap = [
                scoreId:score.scoreId,
                category:score.category,
                outputType:score.outputType,
                isOutputTarget:score.isOutputTarget,
                label:score.label,
                description:score.description,
                displayType:score.displayType,
                entity:score.entity,
                externalId:score.externalId,
                entityTypes:score.entityTypes]
        if (views?.contains("config")) {
            scoreMap.configuration = score.configuration
        }
        scoreMap
    }

    Score createScore(Map properties) {

        properties.scoreId = Identifiers.getNew(true, '')
        Score score = new Score(scoreId:properties.scoreId)
        commonService.updateProperties(score, properties)
        score.save(flush:true)
        return score
    }

    Score updateScore(String id, Map properties) {
        Score score = Score.findByScoreId(id)
        commonService.updateProperties(score, properties)
        score.save(flush:true)
        return score
    }

    def deleteScore(String id, boolean destroy) {
        Score score = Score.findByScoreId(id)
        if (score) {
            try {
                if (destroy) {
                    score.delete()
                } else {
                    score.status = DELETED
                    score.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                Score.withSession { session -> session.clear() }
                def error = "Error deleting score ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    /**
     * Get a unique list of data types used by all models.
     * @return
     */
    List getUniqueDataTypes(){
        Set dataTypes = new HashSet()
        activitiesModel().outputs.each({
            Map dataModel = getOutputDataModel(it.template)
            dataModel.dataModel.each({
                dataTypes.add(it.dataType)
            })
        })

        dataTypes.asList()
    }

    /**
     * Find custom indices used by data models.
     * @return
     */
    Map getIndicesForDataModels(){
        cacheService.get('indices-for-data-models', {
            Map indices = [:].withDefault { [] }
            activitiesModel()?.outputs.each({
                Map dataModel = getOutputDataModel(it.template)
                Map tempIndices = getIndicesForDataModel(dataModel)
                tempIndices.each { key, value->
                    indices[key].addAll(value)
                }
            })

            indices
        })
    }

    /**
     * Remove indices added from template name.
     * @return
     */
    Map getIndicesForDataModelsMinusIndicesForTemplate(String templateName, Map indices){
        indices?.each { String indexName, List dataTypeDetails ->
            List removeDataTypeDetails = dataTypeDetails?.grep {
                it.modelName == templateName
            }

            dataTypeDetails.removeAll(removeDataTypeDetails)
        }

        indices
    }

    /**
     * Find custom indices used in a data model.
     * @return
     */
    Map getIndicesForDataModel(Map model){
        Map indices = [:].withDefault { [] }
        model?.dataModel?.each { metadata ->
            if(IGNORE_DATA_TYPES.contains(metadata.dataType))
                return

            switch (metadata.dataType){
                case 'list':
                    metadata?.columns?.each { column ->
                        if(column.indexName){
                            indices[column.indexName].add([
                                    modelName: model.modelName, indexName: column.indexName, dataType: column.dataType,
                                    path: ["data", metadata.name, column.name]
                            ])
                        }
                    }
                    break;
                case 'matrix':
                    metadata?.rows?.each { row ->
                        if(row.indexName){
                            indices[row.indexName].add([
                                    modelName: model.modelName, indexName: row.indexName, dataType: row.dataType,
                                    path: ["data", metadata.name, row.name]
                            ])
                        }
                    }
                    break
                default:
                    if(metadata.indexName){
                        indices[metadata.indexName].add([
                            modelName: model.modelName, indexName: metadata.indexName, dataType: metadata.dataType,
                            path: ["data",metadata.name]
                        ])
                    }
            }
        }

        indices
    }

    /**
     * An index is valid if
     * 1. all fields using this index has the same data type
     * @param fields
     * @return
     */
    boolean isIndexValid(List fields){
        List dataTypes = fields?.collect { it.dataType }
        dataTypes = dataTypes?.unique()
        if(dataTypes?.size() > 1){
            return false
        }

        true
    }

    /**
     * Checks if user added indices to the passed data model is valid.
     * Conditions to be met by a valid data model
     * 1. Data type of an index must be the same in all data models using that index i.e. if an index 'individualCount'
     * of data type 'number' is added to passed data model, then ensure 'individualCount' used in other models also is
     * of type 'number'. Otherwise, the document is invalid.
     */
    Map isDataModelValid(Map model){
        Map modelIndices = getIndicesForDataModel(model)
        Map allIndices = getIndicesForDataModels()
        allIndices = getIndicesForDataModelsMinusIndicesForTemplate(model.modelName, allIndices)
        boolean valid = true;
        List errorInIndex = []
        if(modelIndices){
            modelIndices.each { String indexName,  List details ->
                List dataType = details?.collect { it.dataType }
                List existingDataTypes = allIndices?.get(indexName)?.collect { it.dataType }
                List defaultDataTypes = grailsApplication.config.facets.data?.grep { it.name == indexName }?.collect { it.dataType }
                List allDataTypes = []
                if(dataType){
                    allDataTypes.addAll(dataType)
                }

                if(existingDataTypes){
                    allDataTypes.addAll(existingDataTypes)
                }

                if(defaultDataTypes){
                    allDataTypes.addAll(defaultDataTypes)
                }

                if(allDataTypes.unique().size() > 1){
                    valid = false
                    errorInIndex.add(indexName)
                }
            }
        }

        [valid : valid, errorInIndex: errorInIndex]
    }

    /**
     * Get services of project from configuration file
     * services.json should be idential with fieldcapture
     * @return
     */
    List<Map> getProjectServices() {
        List services = JSON.parse(getClass().getResourceAsStream('/data/services.json'), 'UTF-8')

        List<Score> scores = Score.findAllWhereStatusNotEqual(DELETED)
        services.each { service ->
            service.scores = new JSONArray(scores.findAll{it.outputType == service.output})
        }
        services
    }

    /**
     * Returns a the List of services being delivered by this project with target information for each score.
     * @param  project
     * @return a data structure similar to:
     * [
     *    name:<service name>,
     *    id:<service id>,
     *    scores:[
     *         [
     *             scoreId:<id>,
     *             label:<score description>,
     *             isOutputTarget: <true/false>,
     *             target:<target defined for this score in the MERI plan, may be null>
     *             periodTargets: [
     *                 [
     *                     period:<string of form year1/year2, eg. 2017/2018, as it appears in the MERI plan>,
     *                     target:<minimum target for this score during the period>
     *                 ]
     *             ]
     *         ]
     *    ]
     * ]
     *
     */

    List<Map> getProjectServicesWithTargets(project){
        def services =  getProjectServices()
        List projectServices = services?.findAll {it.id in project.custom?.details?.serviceIds }
        List targets = project.outputTargets

        // Make a copy of the services as we are going to augment them with target information.
        List results = projectServices.collect { service ->
            [
                    name:service.name,
                    id: service.id,
                    scores: service.scores?.collect { score ->
                        [scoreId: score.scoreId, label: score.label, isOutputTarget:score.isOutputTarget]
                    }
            ]
        }
        results.each { service ->
            service.scores?.each  { score ->
                Map target = targets.find {it.scoreId == score.scoreId}
                if (target){
                    score.valid = true // Ideally remove it, but to set it to invalid is easier
                    score.target = target?.target
                    score.periodTargets = target?.periodTargets
                }

            }
        }

        return results
    }




}
