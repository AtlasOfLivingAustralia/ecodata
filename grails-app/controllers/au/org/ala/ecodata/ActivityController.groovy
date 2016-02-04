package au.org.ala.ecodata

import grails.converters.JSON

import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX

class ActivityController {

    ActivityService activityService
    SiteService siteService
    CommonService commonService
    UserService userService
    ProjectActivityService projectActivityService
    ElasticSearchService elasticSearchService

    static final SCORES = 'scores'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

    def index() {
        log.debug "Total activities = ${Activity.count()}"
        render "${Activity.count()} activities"
    }

    def get(String id) {
        def detail = params.view == SCORES ? [SCORES] : []
        if (!id) {

            def list = activityService.getAll(params.includeDeleted as boolean, params.view)
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def act = activityService.get(id, detail)
            if (act) {
                asJson act
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    @RequireApiKey
    def delete(String id) {
        boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()
        if (activityService.delete(id, destroy).status == 'ok') {
            render (status: 200, text: 'deleted')
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    /**
     * Deletes all activities associated with project activityId.
     *
     * @param id project activity id
     */
    @RequireApiKey
    def deleteByProjectActivity(String id) {
        boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()
        Map result = activityService.deleteByProjectActivity(id, destroy)
        if (result.status == 'ok') {
            render (status: 200, text: 'deleted')
        } else if(result.status == 'error') {
            response.status = 500
            render (status: 500, text: result.status.error)
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }


    @RequireApiKey
    def update(String id) {
        def props = request.JSON
        //log.debug props
        def result
        def message
        if (id) {
            result = activityService.update(props,id)
            message = [message: 'updated']
        }
        else {
            result = activityService.create(props)
            message = [message: 'created', activityId: result.activityId]
        }
        if (result.status != 'ok') {
            //Activity.withSession { session -> session.clear() }
            def errors = result.errorList ?: []
            if (result.error) {
                errors << [error: result.error]
            }
            errors.each {
                log.error it
            }
            message = [message: 'error', errors: errors]
        }
        asJson(message)
    }

    /**
     * The request should look like:
     * /activities/?id=id1&id=id2&id=id3
     * Request body should contain the properties to update, as per the update method.
     * All activities identified by the supplied ids will have the supplied properties updated.
     *
     */
    @RequireApiKey
    def bulkUpdate() {
        def ids = params.list("id")
        def props = request.JSON

        if (!ids) {
            def message = [message:'The id parameter is mandatory']
            render status:400, message as JSON
        }
        if (!props) {
            def message = [message:'The properties to be updated must be supplied in the request body']
            render status:400, message as JSON
        }

        def result = activityService.bulkUpdate(props,ids)
        def message = [message: 'updated']

        if (result.status != 'ok') {
            def errors = result.errorList ?: []
            if (result.error) {
                errors << [error: result.error]
            }
            errors.each {
                log.error it
            }
            message = [message: 'error', errors: errors]
        }
        asJson(message)

    }

    /**
     * Returns a detailed list of all activities associated with a project.
     *
     * Activities can be directly linked to a project, or more commonly, linked
     * via a site that is associated with the project.
     *
     * *** Changing this to match the assumption that every activity will be associated with a
     * project (with or without a site). So there is no need to search via a project's sites.
     *
     * Main output scores are also included.
     *
     * @param id of the project
     */
    def activitiesForProject(String id) {
        if (id) {
            def activityList = []

            activityList.addAll activityService.findAllForProjectId(id, [SCORES])

            asJson([list: activityList])
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    def listForUser(String id){

        def sort = params.sort ?: "lastUpdated"
        def order = params.order ?:  "desc"
        def offset = params.offset ?: 0
        def max = params.pageSize ?: 10

        if(!id){
            response.status = 404
            render status:404, text: 'No such id'
        }
        else{
            def list = activityService.findAllForUserId(id, [max: max,offset:offset,order:order,sort:sort])
            asJson([activities: list?.list, total: list?.total])
        }
    }

    def listByProject(String id){
        def sort = params.sort ?: "lastUpdated"
        def order = params.order ?:  "desc"
        def offset = params.offset ?: 0
        def max = params.pageSize ?: 10
        String userId = params.userId ?: userService.getCurrentUserDetails()?.userId

        if(!id){
            response.status = 404
            render status:404, text: 'No such id'
        }
        else{
            List<String> restrictedProjectActivities = projectActivityService.listRestrictedProjectActivityIds(userId)

            def list = activityService.listByProjectId(id, [max: max,offset:offset,order:order,sort:sort], restrictedProjectActivities)
            asJson([activities: list?.list, total: list?.total])
        }
    }

    /**
     * Is user {@link UserDetails#userId userId} owner of an  {@link Activity#userId userId}
     * @param id activity identifier
     * @return
     */
    def isUserOwnerForActivity(String id) {
        String userId = params.userId

        if (!id) {
            response.status = 404
            render status: 404, text: 'No such id'
        } else if (!userId) {
            response.status = 404
            render status: 400, text: 'Invalid userId'
        } else {
            boolean isOwner = activityService.isUserOwner(userId, id)
            asJson([userIsOwner: isOwner])
        }
    }

    /**
     * Count activity by project activity
     * @param id Project Activity identifier
     * @return activity count.
     */
    def countByProjectActivity(String id){
        if(!id){
            response.status = 404
            render status:404, text: 'No such id'
        }
        else{
            def total = activityService.countByProjectActivityId(id)
            asJson([total: total])
        }
    }

    /**
     * Request body should be JSON formatted of the form:
     * {
     *     "property1":value1,
     *     "property2":value2,
     *     etc
     * }
     * where valueN may be a primitive type or array.
     * The criteria are ANDed together.
     *
     * Dates are treated specially by this method - the properties "plannedStartDate" and "startDate" will be
     * searched using greater than or equals, "plannedEndDate" and "endDate" will be searched using less than or
     * equals.  This is to support range / stage based searching of activities.  Dates should be formatted as UTC.
     *
     * If a property is supplied that isn't a property of the project, it will not cause
     * an error, but no results will be returned.  (this is an effect of mongo allowing
     * a dynamic schema)
     *
     * @return a list of the activities that match the supplied criteria
     */
    @RequireApiKey
    def search() {
        def searchCriteria = request.JSON

        def startDate = null, endDate = null, planned = null
        def dateProperty = searchCriteria.remove('dateProperty')
        if (dateProperty && searchCriteria.startDate) {
            def startDateStr = searchCriteria.remove('startDate')
            startDate = commonService.parse(startDateStr)
        }
        if (dateProperty && searchCriteria.endDate) {
            def endDateStr = searchCriteria.remove('endDate')
            endDate = commonService.parse(endDateStr)
        }

        def activityList = activityService.search(searchCriteria, startDate, endDate, dateProperty, 'all')

        asJson activities:activityList
    }

    @PreAuthorise (idType="projectActivityId")
    def list(String id){

        params.userId = request.userId
        params.projectId = request.projectId ?: projectActivityService.get(id)?.projectId
        elasticSearchService.buildProjectActivityQuery(params)

        response.setContentType("application/json; charset=\"UTF-8\"")
        render elasticSearchService.search(params.query, params, PROJECT_ACTIVITY_INDEX)
    }

    @PreAuthorise(idType = "activityId")
    def getActivity(String id) {
        String error = ''
        if (params.view) {
            List options = ['all', 'flat', 'site']
            String found = options.find { it == params.view }
            error = !found ? 'Invalid view parameter. (Accepted value: all, flat, site)' : ''
        }

        if (!error) {
            def activity = activityService.get(id, params.view ?: 'flat')
            response.setContentType("application/json; charset=\"UTF-8\"")
            render activity as JSON
        } else {
            render status: 403, text: error
        }
    }
}
