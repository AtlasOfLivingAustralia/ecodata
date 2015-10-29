package au.org.ala.ecodata
import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import com.mongodb.DBObject

class ActivityService {

    static transactional = false
    static final ACTIVE = "active"
    static final FLAT = 'flat'
    static final SITE = 'site'

    def grailsApplication
    OutputService outputService
    CommonService commonService
    DocumentService documentService
    SiteService siteService
    CommentService commentService

    def get(id, levelOfDetail = []) {
        def o = Activity.findByActivityIdAndStatus(id, ACTIVE)
        return o ? toMap(o, levelOfDetail) : null
    }

    def getAll(boolean includeDeleted = false, levelOfDetail = []) {
        includeDeleted ?
            Activity.list().collect { toMap(it, levelOfDetail) } :
            Activity.findAllByStatus(ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    /**
     * Accepts a closure that will be called once for each (not deleted) Activity in the system,
     * passing the details Activity (as a Map) as the single parameter.
     * Implementation note, this uses the Mongo API directly as using GORM incurs a
     * significant memory and performance overhead when dealing with so many entities
     * at once.
     * @param action the action to be performed on each Activity.
     * @return
     */
    def doWithAllActivities(Closure action) {
        // Due to various memory & performance issues with GORM mongo plugin 1.3, this method uses the native API.
        com.mongodb.DBCollection collection = Activity.getCollection()
        DBObject query = new BasicDBObject('status', ACTIVE)
        DBCursor results = collection.find(query).batchSize(100)

        results.each { dbObject ->
            action.call(dbObject.toMap())
        }
    }

    /**
     * Check user is an owner of an activity.
     *
     * @param userId user identifier.
     * @param activityId activity identifier.
     * @return
     */
    boolean isUserOwner(userId, activityId) {
        Activity.countByUserIdAndActivityId(userId, activityId) > 0
    }

    def getAll(List listOfIds, levelOfDetail = []) {
        Activity.findAllByActivityIdInListAndStatus(listOfIds, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForSiteId(id, levelOfDetail = []) {
        Activity.findAllBySiteIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    List findAllForProjectId(id, levelOfDetail = [], includeDeleted = false) {
        List activities
        if (includeDeleted) {
            activities = Activity.findAllByProjectId(id).collect {toMap(it, levelOfDetail)}
        }
        else {
            activities = Activity.findAllByProjectIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
        }
        activities
    }

    def findAllForUserId(userId, query, levelOfDetail = []){
         def list = Activity.createCriteria().list(query) {
            and{
                eq ("userId", userId)
                eq ("status", ACTIVE)
            }
           order('lastUpdated','desc')
        }

        [total: list.totalCount, list:list.collect{ toMap(it, levelOfDetail) }]
    }

    def listByProjectId(projectId, query, levelOfDetail = []){
        def list = Activity.createCriteria().list(query) {
            and{
                eq ("projectId", projectId)
                eq ("status", ACTIVE)
            }
            order('lastUpdated','desc')
        }

        [total: list.totalCount, list:list.collect{ toMap(it, levelOfDetail) }]
    }

    /**
     * Count activity by project activity
     * @param pActivityId Project Activity identifier
     * @return activity count.
     */
    def countByProjectActivityId(pActivityId){
        Activity.countByProjectActivityIdAndStatus(pActivityId, ACTIVE)
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
        mapOfProperties.complete = act.complete // This is not a persistent property so is not in the dbo.
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        if (levelOfDetail == SITE) {
            if (mapOfProperties.siteId) {
                mapOfProperties.site = siteService.get(mapOfProperties.siteId, SiteService.FLAT)
            }
        }
        else if (levelOfDetail != FLAT && levelOfDetail != LevelOfDetail.NO_OUTPUTS.name()) {
            mapOfProperties.remove("outputs")
            mapOfProperties.outputs = outputService.findAllForActivityId(act.activityId, levelOfDetail)
            mapOfProperties.documents = documentService.findAllForActivityId(act.activityId)
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
    def create(Map props) {
        Activity activity = new Activity(siteId: props.siteId, activityId: Identifiers.getNew(true, ''))
        try {
            activity.save(failOnError: true)

            props.remove('id')
            props.remove('activityId')
            def outputs = props.remove('outputs')
            commonService.updateProperties(activity, props)
            // If outputs were supplied, update those separately.
            if (outputs) {
                update(outputs: outputs, activity.activityId)
            }
            
            return [status: 'ok', activityId: activity.activityId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Activity.withSession { session -> session.clear() }
            def error = "Error creating activity for site ${props.siteId} - ${e.message}"
            log.error error, e
            
            return [status: 'error', error: error]
        }
    }

    /**
     * Deletes an activity by marking it as not 'active'.
     *
     * @param id
     * @param destroy if true will really delete the object
     * @return
     */
    Map delete(String activityId, boolean destroy = false) {
        Map result

        Activity activity = Activity.findByActivityIdAndStatus(activityId, ACTIVE)
        if (activity) {
            // Delete the outputs associated with this activity.
            outputService.getAllOutputIdsForActivity(activityId).each { outputService.delete(it, destroy) }

            documentService.findAllForActivityId(activityId).each {
                documentService.deleteDocument(it.documentId, destroy)
            }

            commentService.deleteAllForEntity(Activity.class.name, activityId, destroy)

            if (destroy) {
                activity.delete(flush: true)
            } else {
                commonService.updateProperties(activity, [status: 'deleted'])
            }

            if (activity.hasErrors()) {
                result = [status: 'error', error: activity.getErrors()]
            } else {
                result = [status: 'ok']
            }
        } else {
            result = [status: 'not found']
        }

        result
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
        //log.debug "props = ${props}"
        def activity = Activity.findByActivityId(id)
        def errors = []
        if (activity) {
            // do updates for each attached output
            props.outputs?.each { output ->
                if (output.outputId && output.outputId != "null") {
                    // update
                    log.debug "Updating output ${output.name}"
                    def result = outputService.update(output, output.outputId)
                    if (result.error) {
                        errors << [error: result.error, name: output.name]
                    }
                } else {
                    // create
                    log.debug "Creating output ${output.name}"
                    output.remove('outputId')   // in case a blank one is supplied
                    output.activityId = id
                    def result = outputService.create(output)
                    if (result.error) {
                        errors << [error: result.error, name: output.name]
                    }
                }
            }
            // see if the activity itself has updates
            if (props.activityId) {
                try {
                    props.remove('outputs') // get rid of the hitchhiking outputs before updating the activity
                    commonService.updateProperties(activity, props)
                } catch (Exception e) {
                    Activity.withSession { session -> session.clear() }
                    def error = "Error updating Activity ${id} - ${e.message}"
                    log.error error
                    errors << [error: error, name: 'activity']
                }
            }
            // aggregate errors
            if (errors) {
                return [status:'error', errorList: errors]
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
     * A simple implementation of a bulk update - simply delegates to the update method for each
     * supplied id.  The batch size is assumed to be small and hence issues like the grails validation cache are not considered.
     * @param props
     * @param ids a list of activity ids identifying the activities to update.
     * @param props the properties of each activity to update.
     */
    def bulkUpdate(props, ids) {

        def errors = []
        ids.each {
            def newProps = [activityId:it]
            newProps.putAll(props)
            def result = update(newProps, it)
            if (result.status == 'error') {
                if (result.errorList) {
                    errors.addAll(result.errorList)
                }
                if (result.error) {
                    errors << result.error
                }
            }
        }
        if (errors) {
            return [status:'error', errorList: errors]
        } else {
            return [status:'ok']
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

    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a list of the activities that match the supplied criteria
     */
    public search(Map searchCriteria, levelOfDetail = []) {
        return search(searchCriteria, null, null, null, levelOfDetail)
    }


    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     * @param startDate if supplied will constrain the returned activities to those with 'dateProperty' on or after this date.
     * @param endDate if supplied will constrain the returned activities to those with 'dateProperty' before this date.
     * @param dateProperty the property to use for the date range. (plannedStartDate, plannedEndDate, startDate, endDate)
     * @return a listbuilof the activities that match the supplied criteria
     */
    public search(Map searchCriteria, Date startDate, Date endDate, String dateProperty, levelOfDetail = []) {

        def criteria = Activity.createCriteria()
        def activities = criteria.list {
            ne("status", "deleted")
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }

            if (dateProperty && startDate) {
                ge(dateProperty, startDate)
            }
            if (dateProperty && endDate) {
                lt(dateProperty, endDate)
            }


        }
        activities.collect{toMap(it, levelOfDetail)}
    }

}
