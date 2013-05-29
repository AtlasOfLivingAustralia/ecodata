package au.org.ala.ecodata

class OutputService {

    static transactional = false

    def grailsApplication

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def get(id, rich = false) {
        def o = Output.findByOutputId(id)
        return o ? (/*rich ? toRichMap(o):*/ toMap(o)) : null
    }

    def getAll(listOfIds, rich = false) {
        Output.findAllByOutputIdInList(listOfIds).collect { toMap(it) }
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param output an Output instance
     * @return map of properties
     */
    def toMap(output) {
        def dbo = output.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.findAll {k,v -> v != null}
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
            def o = new Output(/*activityId: activity.activityId, */outputId: Identifiers.getNew(true,''))
            try {
                getCommonService().updateProperties(o, props)
                activity.outputs << o.outputId
                activity.save()
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

}
