package au.org.ala.ecodata

class ActivityService {

    static transactional = false
    static final ACTIVE = "active"
    static final FLAT = 'flat'

    def grailsApplication, outputService

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def get(id, levelOfDetail = []) {
        def o = Activity.findByActivityIdAndStatus(id, ACTIVE)
        return o ? toMap(o, levelOfDetail) : null
    }

    def getAll(boolean includeDeleted = false, levelOfDetail = []) {
        includeDeleted ?
            Activity.list().collect { toMap(it, levelOfDetail) } :
            Activity.findAllByStatus(ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def getAll(List listOfIds, levelOfDetail = []) {
        Activity.findAllByActivityIdInListAndStatus(listOfIds, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForSiteId(id, levelOfDetail = []) {
        Activity.findAllBySiteIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForProjectId(id, levelOfDetail = []) {
        Activity.findAllByProjectIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param act an Activity instance
     * @return map of properties
     */
    def toMap(act, levelOfDetail = ['all']) {
        def dbo = act.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")

        if (levelOfDetail != FLAT) {
            mapOfProperties.remove("outputs")
            mapOfProperties.outputs = outputService.findAllForActivityId(act.activityId, levelOfDetail)
        }

        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props) {
        assert getCommonService()
        def o = new Activity(siteId: props.siteId, activityId: Identifiers.getNew(true,''))
        try {
            props.remove('id')
            //println "outputs = " + props.outputs
            def os = props.outputs?.collect { it instanceof String ? it :  it.outputId }
            props.remove('outputs')
            //println os
            props.outputs = os
            //println "outputs = " + props.outputs
            getCommonService().updateProperties(o, props)
            return [status:'ok',activityId:o.activityId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Activity.withSession { session -> session.clear() }
            def error = "Error creating activity for site ${props.siteId} - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def a = Activity.findByActivityId(id)
        if (a) {
            try {
                getCommonService().updateProperties(a, props)
                return [status:'ok']
            } catch (Exception e) {
                Activity.withSession { session -> session.clear() }
                def error = "Error updating Activity ${id} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error updating Activity - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

    /**
     * Converts the domain object into a restricted map of properties.
     * @param act an Activity instance
     * @return map of properties
     */
    def toLiteMap(act) {
        def dbo = act.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        [activityId: mapOfProperties.activityId,
                siteId: mapOfProperties.siteId,
                type: mapOfProperties.type,
                startDate: mapOfProperties.startDate,
                endDate: mapOfProperties.endDate,
                collector: mapOfProperties.collector]
    }

}
