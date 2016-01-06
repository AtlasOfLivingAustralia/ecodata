package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.DataModel

import static au.org.ala.ecodata.Status.*

import au.org.ala.ecodata.converter.RecordConverter

class OutputService {

    static transactional = false

    def grailsApplication
    MetadataService metadataService
    RecordService recordService
    UserService userService
    DocumentService documentService
    CommentService commentService

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

    def findAllForActivityId(id, levelOfDetail = []) {
        Output.findAllByActivityIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
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
        def dbo = output.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
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
                props = saveImages(props, output.outputId, props.activityId);

                getCommonService().updateProperties(output, props)

                createRecordsForOutput(activity, output, props)

                return [status: 'ok', outputId: output.outputId]
            } catch (Exception e) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Output.withSession { session -> session.clear() }
                def error = "Error creating output for activity ${props.activityId} - ${e.message}"
                e.printStackTrace()
                log.error error, e
                return [status: 'error', error: error]
            }
        } else {
            def error = "Error creating output - no activity with id = ${props.activityId}"
            log.error error
            return [status: 'error', error: error]
        }
    }

    void createRecordsForOutput(Activity activity, Output output, Map props) {
        Map outputMetadata = metadataService.getOutputDataModelByName(props.name) as Map

        boolean createRecord = outputMetadata && outputMetadata["record"]?.toBoolean()

        if (createRecord) {
            Project project = Project.findByProjectId(activity.projectId)
            Site site = activity.siteId ? Site.findBySiteId(activity.siteId) : null
            ProjectActivity projectActivity = ProjectActivity.findByProjectActivityId(activity.projectActivityId)

            List<Map> records = RecordConverter.convertRecords(project, site, projectActivity, activity, output, props.data, outputMetadata)

            records.each { record ->
                // createRecord returns a 2-element list:
                // [0] = Record (always there even if the save failed);
                // [1] = Error object if the save failed, empty map if the save succeeded.
                List result = recordService.createRecord(record)
                if (result[1]) {
                    throw new IllegalArgumentException("Failed to create record: ${record}")
                }
            }
        }
    }

    def update(Map props, String outputId) {
        Output output = Output.findByOutputId(outputId)
        if (output) {
            Activity activity = Activity.findByActivityId(output.activityId)
            try {
                // save image properties to db
                props = saveImages(props, output.outputId, activity.activityId)

                getCommonService().updateProperties(output, props)

                List<Record> records = Record.findAllByOutputId(outputId)
                if (records) {
                    Record.deleteAll(records)
                }

                createRecordsForOutput(activity, output, props)

                return [status: 'ok']
            } catch (Exception e) {
                Output.withSession { session -> session.clear() }
                String error = "Error updating output ${outputId} - ${e.message}"
                log.error error, e
                return [status: 'error', error: error]
            }
        } else {
            String error = "Error updating output - no such id ${outputId}"
            log.error error
            return [status: 'error', error: error]
        }
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
     * @return
     */
    Map saveImages(Map output, String outputId, String activityId){
        List result, activityOutputs;
        Map document, savedOutput
        String documentId
        URL biocollect
        InputStream stream

        if(activityId && output?.size() > 0){
            Map outputMetadata = metadataService.getOutputDataModelByName(output.name) as Map
            DataModel model = new DataModel(outputMetadata);
            List names = model.getNamesforDataType('image');

            result = []
            names.each { name ->
                output?.data[name]?.each {
                    document = null;
                    if(!it.documentId){
                        it.activityId = activityId
                        it.outputId = outputId
                        it.remove('staged')
                        it.role = 'surveyImage'
                        it.type = 'image'

                        biocollect = new URL(it.url)
                        stream = biocollect.openStream()
                        document = documentService.create(it, stream)
                        documentId = document?.documentId
                        if(documentId){
                            document = documentService.toMap(Document.findByDocumentId(documentId))
                        }
                    } else {
                        documentService.update(it, it.documentId);
                        // if deleted ignore the document
                        if(it.status != Status.DELETED){
                            document = documentService.toMap(Document.findByDocumentId(it.documentId))
                        }
                    }

                    if(document){
                        document.remove('url')
                        document.remove('thumbnailUrl')
                        result.push(document)
                    }
                }

                output.data[name] = result
            }
        }

        output
    }
}
