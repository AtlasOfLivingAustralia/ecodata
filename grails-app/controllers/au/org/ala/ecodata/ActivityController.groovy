package au.org.ala.ecodata

import grails.converters.JSON

import java.text.SimpleDateFormat

class ActivityController {

    def activityService, siteService
    static final SCORES = 'scores'
    static final BRIEF = 'brief'

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json; charset=\"UTF-8\"")
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
        if (activityService.delete(id, params.destroy).status == 'ok') {
            render (status: 200, text: 'deleted')
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
            // activities directly linked to project
            activityList.addAll activityService.findAllForProjectId(id, [SCORES])
            // activities via sites
            /*siteService.findAllForProjectId(id, BRIEF).each {
                activityList.addAll activityService.findAllForSiteId(it.siteId, [SCORES])
            }*/
            //log.debug activityList
            asJson([list: activityList])
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }
}
