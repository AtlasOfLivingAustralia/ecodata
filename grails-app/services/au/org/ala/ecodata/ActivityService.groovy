package au.org.ala.ecodata

class ActivityService {

    static transactional = false
    static final ACTIVE = "active"
    static final FLAT = 'flat'

    def grailsApplication, outputService, commonService

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

        if (levelOfDetail != FLAT && levelOfDetail != LevelOfDetail.NO_OUTPUTS.name()) {
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

    /**
     * Creates an activity.
     *
     * NOTE embedded output data is not expected here and will be discarded.
     *
     * @param props the activity properties
     * @return json status
     */
    def create(props) {
        def o = new Activity(siteId: props.siteId, activityId: Identifiers.getNew(true,''))
        try {
            props.remove('id')
            props.remove('outputs')
            commonService.updateProperties(o, props)
            return [status:'ok',activityId:o.activityId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Activity.withSession { session -> session.clear() }
            def error = "Error creating activity for site ${props.siteId} - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    /**
     * Updates an activity and also updates/creates any outputs that are passed in the 'outputs' property
     * of the activity. The activity itself is optional and will only be updated if the activityId
     * property is present (in the props).
     *
     * @param props the activity properties and the list of outputs
     * @param id the activity id
     * @return json status
     */
    def update(props, id) {
        log.debug "props = ${props}"
        def a = Activity.findByActivityId(id)
        def errors = []
        if (a) {
            // do updates for each attached output
            props.outputs.each { output ->
                if (output.outputId) {
                    // update
                    log.debug "Updating output ${output.name}"
                    def result = outputService.update(output, output.outputId)
                    if (result.error) {
                        errors << result.error
                    }
                } else {
                    // create
                    log.debug "Creating output ${output.name}"
                    output.remove('outputId')   // in case a blank one is supplied
                    output.activityId = id
                    def result = outputService.create(output)
                    if (result.error) {
                        errors << result.error
                    }
                }
            }
            // see if the activity itself has updates
            if (props.activityId) {
                try {
                    props.remove('outputs') // get rid of the hitchhiking outputs before updating the activity
                    commonService.updateProperties(a, props)
                } catch (Exception e) {
                    Activity.withSession { session -> session.clear() }
                    def error = "Error updating Activity ${id} - ${e.message}"
                    log.error error
                    errors << error
                }
            }
            // aggregate errors
            if (errors) {
                return [status:'error', error: errors]
            } else {
                return [status:'ok']
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
