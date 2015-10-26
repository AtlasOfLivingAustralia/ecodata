package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.*

import au.org.ala.ecodata.converter.RecordConverter
import au.org.ala.ecodata.converter.RecordConverterFactory

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

                getCommonService().updateProperties(output, props)

                createRecordsForOutput(output, activity, props)

                return [status: 'ok', outputId: output.outputId]
            } catch (Exception e) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Output.withSession { session -> session.clear() }
                def error = "Error creating output for activity ${props.activityId} - ${e.message}"
                log.error error, e
                return [status: 'error', error: error]
            }
        } else {
            def error = "Error creating output - no activity with id = ${props.activityId}"
            log.error error
            return [status: 'error', error: error]
        }
    }

    void createRecordsForOutput(Output output, Activity activity, Map props) {
        Map outputMetadata = metadataService.getOutputDataModelByName(props.name)

        Date embargoUntilDate = EmbargoUtil.calculateEmbargoUntilDate(ProjectActivity.findByProjectActivityId(activity.projectActivityId))

        outputMetadata?.dataModel?.each { dataModel ->
            if (dataModel.containsKey("record") && dataModel.record.toBoolean()) {
                RecordConverter converter = RecordConverterFactory.getConverter(dataModel.dataType)
                List<Map> records = converter.convert(props, dataModel)

                records.each { record ->
                    record.outputId = output.outputId
                    record.projectId = activity.projectId
                    record.projectActivityId = activity.projectActivityId
                    record.activityId = activity.activityId
                    record.userId = activity.userId
                    record.embargoUntil = embargoUntilDate

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
    }

    def update(Map props, String outputId) {
        Output output = Output.findByOutputId(outputId)
        if (output) {
            Activity activity = Activity.findByActivityId(output.activityId)
            try {
                getCommonService().updateProperties(output, props)

                List<Record> records = Record.findAllByOutputId(outputId)

                if (records) {
                    records.each {
                        it.delete()
                    }
                }

                createRecordsForOutput(output, activity, props)

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
}
