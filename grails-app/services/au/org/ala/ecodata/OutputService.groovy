package au.org.ala.ecodata

class OutputService {

    static transactional = false

    def grailsApplication, metadataService

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

    def delete(String id, destroy) {
        def a = Output.findByOutputId(id)
        if (a) {
            if (destroy) {
                a.delete()
            } else {
                a.status = 'deleted'
                a.save(flush: true)
            }
            return [status:'ok']
        } else {
            return [status:'error', error:'No such id']
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
        mapOfProperties.findAll {k,v -> v != null}
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
        def scores = scoreDefinitions.collectEntries { [(it.name):map[it.name]] }
        //log.debug "scores = ${scores}"
        return scores
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props) {
        assert getCommonService()
        def activity = Activity.findByActivityId(props.activityId)
        if (activity) {
            def o = new Output(activityId: activity.activityId, outputId: Identifiers.getNew(true,''))
            try {
                o.save(failOnError: true) // Getting dynamic properties not saving without this.

                getCommonService().updateProperties(o, props)
                return [status:'ok',outputId:o.outputId]
            } catch (Exception e) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Output.withSession { session -> session.clear() }
                def error = "Error creating output for activity ${props.activityId} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error creating output - no activity with id = ${props.activityId}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def a = Output.findByOutputId(id)
        if (a) {
            try {
                getCommonService().updateProperties(a, props)
                return [status:'ok']
            } catch (Exception e) {
                Output.withSession { session -> session.clear() }
                def error = "Error updating output ${id} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error updating output - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def testGrailsApplication() {
        assert grailsApplication
        assert grailsApplication.mainContext.commonService
        return "ok"
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
