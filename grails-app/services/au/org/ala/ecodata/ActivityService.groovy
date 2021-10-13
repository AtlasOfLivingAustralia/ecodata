package au.org.ala.ecodata
import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import com.mongodb.DBObject
import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.grails.datastore.mapping.query.api.BuildableCriteria
import au.org.ala.ecodata.metadata.*

import javax.persistence.PessimisticLockException

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class ActivityService {

    static transactional = false
    static final FLAT = 'flat'
    static final SITE = 'site'

    def grailsApplication
    OutputService outputService
    CommonService commonService
    DocumentService documentService
    SiteService siteService
    CommentService commentService
    UserService userService
    LockService lockService
    MetadataService metadataService
    PermissionService permissionService

    def get(id, levelOfDetail = [], version = null, userId = null, hideMemberOnlyFlds = false) {
        def activity = null

        if (version) {
            def all = AuditMessage.findAllByEntityIdAndEntityTypeAndDateLessThanEquals(id, Activity.class.name,
                    new Date(version as Long), [sort:'date', order:'desc', max: 1])

            all?.each {
                if (it.entity.status == ACTIVE &&
                        (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                    activity = toMap(it.entity, levelOfDetail, version)
                }
            }
        } else {
            def o = Activity.findByActivityIdAndStatus(id, ACTIVE)
            activity = o ? toMap(o, levelOfDetail) : null
        }

        if (activity == null ){
            return [status:404 , error: 'Activity cannot be found']
        }

        // If field is flagged as visible to project members only, and the caller requested to hide its value
        if (hideMemberOnlyFlds){
            boolean userIsAlaAdmin = userId && permissionService.isUserAlaAdmin(userId) ? true : false

            boolean userIsProjectMember = false
            if (userId) {
                def members = permissionService.getMembersForProject(activity.projectId)
                userIsProjectMember = members.find{it.userId == userId} || userIsAlaAdmin
            }

            OutputModelProcessor processor = new OutputModelProcessor()
            activity.outputs?.each { output ->
                OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(output.name))
                processor.hideMemberOnlyAttributes(output, outputModel, userIsProjectMember)
            }
        }

        activity
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
        def collection = Activity.getCollection()

       // collection.setDBDecoderFactory
        BasicDBObject query = new BasicDBObject('status', ACTIVE)
     //   query.append("activityId", "19b0b4db-5b74-4907-b14b-dccc3bac0f07")
        //query.append('activityId', 'd6d2f4b6-1479-4647-ac94-e48d91651b6b')
        //Activity.setMapping()
        def results = collection.find(query).batchSize(100)

        results.each { dbObject ->
            action.call(dbObject)
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

    /**
     * Get activities of given activities
     * @param listOfIds  a list of activityId
     * @param startDate
     * @param endDate
     * @param levelOfDetail
     * @return
     */
    def getAll(List listOfIds, Date startDate, Date endDate, levelOfDetail = []) {
        Activity.findAllByActivityIdInListAndStartDateGreaterThanEqualsAndEndDateLessThanEqualsAndStatus(listOfIds,startDate,endDate, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    /**
     * Get the period of activities
     * @param listOfIds IDs of activities
     * @return
     */
    def getPeriod(List listOfIds){
      def period = Activity.createCriteria().list {
            projections {
                min "plannedStartDate"
                max "plannedEndDate"
            }
          inList('activityId', listOfIds)
        }
        return period
    }


    def findAllForSiteId(id, levelOfDetail = [], version = null) {
        if (version) {
            def activityIds = Activity.findAllBySiteId(id).collect { it.activityId }
            def all = AuditMessage.findAllByEntityIdInListAndEntityTypeAndDateLessThanEquals(activityIds, Activity.class.name,
                    new Date(version as Long), [sort:'date', order:'desc'])
            def activities = []
            def found = []
            all?.each {
                if (!found.contains(it.entityId)) {
                    found << it.entityId
                    if (it.entity.status == ACTIVE &&
                        (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        activities << toMap(it.entity, levelOfDetail, version)
                    }
                }
            }

            activities
        } else {
            Activity.findAllBySiteIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
        }
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

    List<Map> findAllForProjectActivityId(String projectActivityId, levelOfDetail = []) {
        Activity.findAllByProjectActivityIdAndStatus(projectActivityId, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    List<Map> findAllForActivityIdsInProjectActivity(List activityIdList, String projectActivityId, levelOfDetail = []) {
        Activity.findAllByActivityIdInListAndProjectActivityIdAndStatus(activityIdList, projectActivityId, ACTIVE).collect { toMap(it, levelOfDetail) }
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

    Map listByProjectId(String projectId, Map query, List<String> restrictedProjectActivityIds,levelOfDetail = []) {
        String userId = userService.getCurrentUserDetails()?.userId

        def list = Activity.createCriteria().list(query) {
            eq ("projectId", projectId)
            eq ("status", ACTIVE)

            if (restrictedProjectActivityIds) {
                or {
                    eq "userId", userId
                    not { 'in' "projectActivityId", restrictedProjectActivityIds }
                }
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
     * Count activity by project activity
     * @param pActivityId Project Activity identifier
     * @return activity count.
     */
    List getDistinctSitesForProjectActivity(pActivityId, status = ACTIVE) {

        BuildableCriteria c = Activity.createCriteria()
        def results = c.listDistinct {
            projections {
                property 'siteId'
            }
            eq("projectActivityId", pActivityId)
            eq("status", status)
            and{
                ne("siteId", null)
                ne("siteId", "")
            }

        }

        results
    }

    /**
     * Get distinct sites associated with activities in a project
     * @param projectId Project identifier
     * @return activity count.
     */
    List getDistinctSitesForProject(projectId, status = ACTIVE) {

        BuildableCriteria c = Activity.createCriteria()
        def results = c.listDistinct {
            projections {
                property 'siteId'
            }
            eq("projectId", projectId)
            eq("status", status)
            and{
                ne("siteId", null)
                ne("siteId", "")
            }

        }

        new HashSet(results).toArray().toList()
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param act an Activity instance
     * @return map of properties
     */
    def toMap(act, levelOfDetail = ['all'], version = null) {
       // def mapOfProperties = act instanceof Activity ? act.getProperty("dbo").toMap() : act
        def mapOfProperties = act instanceof Activity ? GormMongoUtil.extractDboProperties(act.getProperty("dbo")) : act //[*:GormMongoUtil.extractDboProperties(act.getProperty("dbo"))] : act
        mapOfProperties.complete = act.complete // This is not a persistent property so is not in the dbo.
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")

        if (levelOfDetail == SITE) {
            if (mapOfProperties.siteId) {
                mapOfProperties.site = siteService.get(mapOfProperties.siteId, SiteService.FLAT, version)
            }
        }
        else if (levelOfDetail != FLAT && levelOfDetail != LevelOfDetail.NO_OUTPUTS.name()) {
            mapOfProperties.remove("outputs")
            mapOfProperties.outputs = outputService.findAllForActivityId(act.activityId, levelOfDetail, version)
            mapOfProperties.documents = documentService.findAllForActivityId(act.activityId, version)
            Lock lock = lockService.get(act.activityId)
            if (lock) {
                mapOfProperties.lock = lock
            }
        }

        mapOfProperties.findAll {k,v -> v != null}
       // GormMongoUtil.deepPrune(mapOfProperties)
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
            //activity.save(failOnError: true, flush:true)

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
     * Deletes all activities associated with project activityId.
     *
     * @param pActivityId project activity id
     * @param destroy if true will really delete the object
     * @return
     */
    Map deleteByProjectActivity(String pActivityId, boolean destroy = false) {
        Map result

        ProjectActivity pActivity = ProjectActivity.findByProjectActivityId(pActivityId)
        if (pActivity) {
            getAllActivityIdsForProjectActivity(pActivityId).each { delete(it, destroy) }
            boolean exists = Activity.countByProjectActivityIdAndStatusNotEqual(pActivity.projectActivityId, DELETED) > 0
            if (exists) {
                result = [status: 'error', error: "Error deleting activities"]
            } else {
                result = [status: 'ok']
            }
        } else {
            result = [status: 'not found']
        }

        result
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
            deleteActivityOutputs(activityId, destroy)

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
     * Deletes an activity by marking it as not 'active'.
     *
     * @param id
     * @param destroy if true will really delete the object
     * @return
     */
    Map bulkDelete(List activityIds, boolean destroy = false) {
        Map result = [ success : true]

        activityIds?.each { activityId ->
            result[activityId] = delete(activityId, destroy)

            if (result[activityId]?.status != 'ok') {
                result['success'] = false
            }
        }

        result
    }

    /**
     * Deletes each of the outputs associated with this activity.
     * @param activityId the ID of the activity to delete.
     * @param destroy whether to perform a soft delete or hard delete.
     */
    void deleteActivityOutputs(String activityId, destroy = false) {
        outputService.getAllOutputIdsForActivity(activityId).each { outputService.delete(it, destroy) }
    }

    /**
     * Updates an activity and also updates/creates any outputs that are passed in the 'outputs' property
     * of the activity. The activity itself is optional and will only be updated if the activityId
     * property is present (in the props).
     *
     * @param props the activity properties and the list of outputs
     * @param id the activity id
     * @param lock if true, a lock will be checked / obtained for the duration of the update.  If the lock is already
     * held by the user, it will not be released after the update.  If it is held by another user the update will
     * not occur and an error will be returned
     * @return Map containing either status:'ok' for a successful result or status:'error' for a failure.
     */
    Map update(props, String id, boolean lock = false) {

        Map result
        if (lock) {
            try {
                result = lockService.executeWithLock(id) {
                    doUpdate(props, id)
                }
            }
            catch (PessimisticLockException e) {
                result = [status:'error', error:"The activity is being updated by another user"]
            }
            return result

        }
        else {
            result = doUpdate(props, id)
        }

        result
    }

    private Map doUpdate(props, String id) {
        def activity = Activity.findByActivityId(id)
        def errors = []
        if (activity) {
            def outputs = props.remove('outputs') // get rid of the hitchhiking outputs before updating the activity
            // see if the activity itself has updates
            if (props.activityId) {
                try {
                    props.remove('userId')
                    props.remove('activityId')
                    props.remove('projectId')
                    props.remove('projectActivityId')
                    // If the activity type has changed, we need to delete any outputs associated with
                    // the previous type or they will hang around and be counted in dashboards etc.
                    // A more sophisticated routine may keep output data that is common to both the
                    // old type and new type.
                    if (props.type && activity.type != props.type) {
                        deleteActivityOutputs(id)
                    }
                    commonService.updateProperties(activity, props)

                    // If the activity has been updated to a state where Outputs are not supported, delete any
                    // existing outputs.  This is to handle the case where an activity with output data is
                    // cancelled or deferred.
                    if (!activity.supportsOutputs()) {
                        deleteActivityOutputs(id)
                    }

                } catch (Exception e) {
                    Activity.withSession { session -> session.clear() }
                    def error = "Error updating Activity ${id} - ${e.message}"
                    log.error(error, e) //You have to hate exeption hiding
                    errors << [error: error, name: 'activity']
                }
            }

            // do updates for each attached output
            //outputs and/or records depend on activity info so activity needs to be updated first
            outputs?.each { output ->
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
            // aggregate errors
            if (errors) {
                return [status: 'error', errorList: errors]
            } else {
                return [status: 'ok']
            }
        } else {
            def error = "Error updating Activity - no such id ${id}"
            log.error error
            return [status: 'error', error: error]
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
        def mapOfProperties = act.getProperty("dbo")
       // def mapOfProperties = dbo.toMap()
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

    def getAllActivityIdsForProjectActivity(String pActivityId) {
        Activity.withCriteria {
            eq "projectActivityId", pActivityId
            projections {
                property("activityId")
            }
        }
    }

}
