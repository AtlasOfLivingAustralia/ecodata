package au.org.ala.ecodata
import au.org.ala.ecodata.converter.RecordConverter
import au.org.ala.ecodata.metadata.OutputMetadata

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class OutputService {

    static transactional = false

    def grailsApplication
    MetadataService metadataService
    RecordService recordService
    UserService userService
    DocumentService documentService
    CommentService commentService
    ActivityService activityService

    static final ACTIVE = "active"
    static final SCORES = 'scores'

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def get(id, levelOfDetail = ['all']) {
        def o = Output.findByOutputId(id)
        return o ? toMap(o, levelOfDetail) : null
    }

    def getAll(listOfIds, levelOfDetail = ['all']) {
        Output.findAllByOutputIdInListAndStatus(listOfIds, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForActivityId(id, levelOfDetail = [], version = null) {
        if (version) {
            def sourceOutputs = Output.findAllByActivityId(id).collect { it.outputId }
            def all = AuditMessage.findAllByEntityIdInListAndEntityTypeAndDateLessThanEquals(sourceOutputs, Output.class.name,
                    new Date(version as Long), [sort:'date', order:'desc'])
            def outputs = []
            def found = []
            all?.each {
                if (!found.contains(it.entityId)) {
                    found << it.entityId
                    if (it.entity.activityId == id && it.entity.status == ACTIVE &&
                        (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        outputs << toMap(it.entity, levelOfDetail)
                    }
                }
            }

            outputs
        } else {
            Output.findAllByActivityIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
        }
    }

    def findAllForActivityIdAndName(id, name, levelOfDetail = []) {
        Output.findAllByActivityIdAndNameAndStatus(id, name, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    Map delete(String id, boolean destroy = false) {
        Output output = Output.findByOutputId(id)

        if (output) {

            commentService.deleteAllForEntity(Output.class.name, id, destroy)

            if (destroy) {
                Record.findAllByOutputId(id)?.each {
                    it.delete()
                }

                Document.findAllByOutputId(id)?.each {
                    it.delete(flush: true)
                }

                output.delete(flush: true)
            } else {
                output.status = DELETED
                output.save(flush: true)

                Document.findAllByOutputId(id)?.each {
                    documentService.deleteDocument(it.documentId, destroy)
                }

                Record.findAllByOutputId(id)?.each {
                    it.status = DELETED
                    it.save(flush: true)
                }
            }

            [status: 'ok']
        } else {
            [status: 'error', error: 'No such id']
        }
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param output an Output instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    def toMap(output, levelOfDetail = []) {
        def mapOfProperties = output instanceof Output ?  output.getProperty("dbo").toMap() : output
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        if (SCORES in levelOfDetail) {
            def scores = extractScores mapOfProperties.data, output.name
            mapOfProperties.scores = scores
            mapOfProperties.remove 'data'
        }
        mapOfProperties.findAll { k, v -> v != null }
    }

    /**
     * Returns a map of scores based on the output model.
     * @param map
     * @param name
     * @return
     */
    def extractScores(map, name) {
        //log.debug "extracting scores for ${name}"
        def model = metadataService.getOutputModel(name)
        //log.debug "model is " + model
        def scoreDefinitions = model?.scores ?: []
        //log.debug "scoreNames = ${scoreNames}"
        def scores = scoreDefinitions.collectEntries { [(it.name): map[it.name]] }
        //log.debug "scores = ${scores}"
        return scores
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(Map props) {
        assert getCommonService()
        Activity activity = Activity.findByActivityId(props.activityId)
        if (activity) {
            Output output = new Output(activityId: activity.activityId, outputId: Identifiers.getNew(true, ''))
            try {
                output.save(failOnError: true) // Getting dynamic properties not saving without this.

                // save images to ecodata
                props.data = saveImages(props.data, props.name, output.outputId, props.activityId);
                props.data = saveAudio(props.data, props.name, output.outputId, props.activityId);

                getCommonService().updateProperties(output, props)

                createOrUpdateRecordsForOutput(activity, output, props)

                return [status: 'ok', outputId: output.outputId]
            } catch (Exception e) {
                def error = "Error creating output for activity ${props.activityId} - ${e.message}"
                log.error error, e
                // clear session to avoid exception when GORM tries to autoflush the changes
                Output.withSession { session -> session.clear() }
                return [status: 'error', error: error]
            }
        } else {
            def error = "Error creating output - no activity with id = ${props.activityId}"
            log.error error
            return [status: 'error', error: error]
        }
    }

    void createOrUpdateRecordsForOutput(Activity activity, Output output, Map props) {
        Map outputMetadata = metadataService.getOutputDataModelByName(props.name) as Map

        boolean createRecord = outputMetadata && outputMetadata["record"]?.toBoolean()

        if (createRecord) {
            Project project = Project.findByProjectId(activity.projectId)
            Site site = activity.siteId ? Site.findBySiteId(activity.siteId) : null
            ProjectActivity projectActivity = ProjectActivity.findByProjectActivityId(activity.projectActivityId)

            List<Map> records = RecordConverter.convertRecords(project, site, projectActivity, activity, output, props.data, outputMetadata)

            records.each { record ->
                //Create or update record?
                Record existingRecord = Record.findByOutputSpeciesId(record.outputSpeciesId)
                if (existingRecord) {
                    existingRecord.status = Status.ACTIVE
                    try {
                        recordService.updateRecord(existingRecord, record)
                    } catch (e) { // Never hide an exception, chain it instead
                        //No need to log here if it is chained, the catcher should do the right thing
                        throw new IllegalArgumentException("Failed to update record: ${record},\n Original Error: ${e.message}", e)
                    }
                } else {
                    try {
                        recordService.createRecord(record)
                    } catch (e) {
                        throw new IllegalArgumentException("Failed to create record: ${record},\n Original Error: ${e.message}", e)
                    }
                }
            }
        }
    }

    def update(Map props, String outputId) {
        Output output = Output.findByOutputId(outputId)
        Map result
        if (output) {
            Activity activity = Activity.findByActivityId(output.activityId)
            try {
                // save image properties to db
                props.data = saveImages(props.data, props.name, output.outputId, activity.activityId)
                props.data = saveAudio(props.data, props.name, output.outputId, activity.activityId)

                getCommonService().updateProperties(output, props)

                List statusUpdate = recordService.updateRecordStatusByOutput(outputId, Status.DELETED)
                if (!statusUpdate) {
                    createOrUpdateRecordsForOutput(activity, output, props)
                    result = [status: 'ok']
                } else {
                    result = [status: 'error', error: "Error updating the record status"]
                }

            } catch (Exception e) {
                String error = "Error updating output ${outputId} - ${e.message}"
                log.error error, e
                Output.withSession { session -> session.clear() }
                result = [status: 'error', error: error]
            }
        } else {
            String error = "Error updating output - no such id ${outputId}"
            log.error error
            result = [status: 'error', error: error]
        }

        result
    }

    def getAllOutputIdsForActivity(String activityId) {
        def c = Output.createCriteria()
        def list = c {
            eq("activityId", activityId)
            projections {
                property("outputId")
            }
        }
        return list*.toString()
    }

    /**
     * list all output for an activity id
     * @param activityId
     * @return
     */
    List listAllForActivityId(String activityId){
       Output.findAllByActivityIdAndStatus(activityId, ACTIVE)?.collect{
           toMap(it)
       }
    }

    /**
     * find images and save or delete it.
     * @param activityId
     * @param outputs
     * @return the output data, with any image objects updated to include the new document id
     */
    Map saveImages(Map output, String metadataName, String outputId, String activityId, Map context = null) {
        saveMultimedia(output, metadataName, outputId, activityId, "image", "surveyImage", "image", context)
    }

    /**
     * find images and save or delete it.
     * @param activityId
     * @param outputs
     * @return the output data, with any image objects updated to include the new document id
     */
    Map saveAudio(Map output, String metadataName, String outputId, String activityId, Map context = null) {
        saveMultimedia(output, metadataName, outputId, activityId, "audio", "surveyAudio", "audio", context)
    }

    Map saveMultimedia(Map output, String metadataName, String outputId, String activityId, String dataTypeName, String role, String type, Map context = null) {
        URL biocollect
        InputStream stream
        Map outputMetadata, names
        OutputMetadata dataModel
        List remove

        if(!context){
            outputMetadata = metadataService.getOutputDataModelByName(metadataName) as Map
            dataModel = new OutputMetadata(outputMetadata);
            names = dataModel.getNamesForDataType(dataTypeName, null);
        } else {
            names = context
        }

        if (activityId && output?.size() > 0) {
            names?.each { name, node ->
                if(node instanceof Boolean){
                    remove = []
                    output[name]?.each {
                        // save image if document id not found
                        if (!it.documentId) {
                            it.activityId = activityId
                            it.outputId = outputId
                            it.remove('staged')
                            it.role = role
                            it.type = type
                            // record creation requires images to have an 'identifier' attribute containing the url for the image
                            it.identifier = it.url

                            biocollect = new URL(it.url)
                            stream = biocollect.openStream()
                            Map document = documentService.create(it, stream)
                            it.documentId = document.documentId
                        } else {
                            documentService.update(it, it.documentId);
                            // if deleted remove the document
                            if (it.status == DELETED) {
                                remove.push(it);
                            }
                        }
                    }
                    // remove all deleted images
                    output[name]?.removeAll(remove)
                }

                // recursive check for image data
                if(node instanceof Map){
                    if(output[name] instanceof Map){
                        output[name] = saveMultimedia(output[name], metadataName, outputId, activityId, dataTypeName, role, type, node)
                    }

                    if(output[name] instanceof  List){
                        output[name].eachWithIndex{ column, index ->
                            output[name][index] = saveMultimedia(column, metadataName, outputId, activityId, dataTypeName, role, type,  node)
                        }
                    }
                }
            }
        }

        output
    }
}
