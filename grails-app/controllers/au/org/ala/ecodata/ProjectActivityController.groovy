package au.org.ala.ecodata

import grails.converters.JSON

class ProjectActivityController {

    /* Testing merge to grails 3 */
    def projectActivityService

    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

    /**
     * Get all ProjectActivity by project id.
     *
     * @param id - project id
     * @return json
     */
    def getAllByProject(String id) {
        if (id) {
            def list = []
            list.addAll(projectActivityService.getAllByProject(id, params.view, params?.version))
            if( params.stats?.asBoolean() ){
                list.each { pActivity ->
                    projectActivityService.addProjectActivityStats(pActivity)
                }
            }

            asJson([list: list])
        } else {
            response.status = 404
            render status: 404, text: 'No such id'
        }
    }

    /**
     * Get projectActivity by id
     *
     * @param id - projectActivity id
     * @return json
     */
    def get(String id) {
        Map result = [:]
        if (!id) {
            result = [status: 404, text: 'No such id'];
        } else {
            result = projectActivityService.get(id, params.view, params?.version)
            if( params.stats?.asBoolean() ){
                projectActivityService.addProjectActivityStats(result)
            }

            if (!result) {
                result = [status: 404, text: 'Invalid id'];
            }
        }

        asJson(result)
    }

    /**
     * Delete project activity and all child records
     * @param id - project activity id
     * @param destroy = true to permanently delete the records, false to soft delete
     *
     * @return json map of
     */
    @RequireApiKey
    def delete(String id) {
        ProjectActivity pActivity = ProjectActivity.findByProjectActivityId(id)
        if (pActivity) {
            boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()
            Map result = projectActivityService.delete(id, destroy)
            if (!result.error) {
                response.setStatus(200)
                response.setContentType("application/json")
                render(status: 200, text: 'deleted')
            } else {
                response.status = 400
                render(status: 400, text: result.error)
            }
        } else {
            response.status = 400
            render(status: 404, text: 'No such id')
        }
    }

    /**
     * Update a project activity.
     *
     * @param id - identifies the resource
     * @return
     */
    @RequireApiKey
    def update(String id) {
        def props = request.JSON
        log.debug props
        def result
        def message
        if (id) {
            result = projectActivityService.update(props, id)
            message = [message: 'updated']
        } else {
            result = projectActivityService.create(props)
            message = [message: 'created', projectActivityId: result.projectActivityId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            log.error result.error
            render status: 400, text: result.error
        }
    }

    /**
     * List projectActivities for the given projectId
     *
     * @param id project identifier.
     */
    @PreAuthorise (idType="projectId")
    def list(String id){
        response.setContentType('application/json; charset="UTF-8"')
        render projectActivityService.getAllByProject(id) as JSON
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
     * If a property is supplied that isn't a property of the project activity, it will not cause
     * an error, but no results will be returned.  (this is an effect of mongo allowing
     * a dynamic schema)
     *
     * @return a list of the project activity that match the supplied criteria
     */
    @RequireApiKey
    def search() {
        def searchCriteria = request.JSON

        def view = searchCriteria.remove('view') ?: ""
        def projectActivityList = projectActivityService.search(searchCriteria, view)
        asJson projectActivities: projectActivityList
    }
}