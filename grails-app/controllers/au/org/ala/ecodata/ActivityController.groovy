package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class ActivityController {

    def activityService, commonService

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    def index() {
        log.debug "Total activities = ${Activity.count()}"
        render "${Activity.count()} activities"
    }

    def get(String id) {
        if (!id) {
            def list = []
            def activities
            if (params.type == 'assessment') {
                activities = params.includeDeleted ? Activity.findAllByAssessment(true) :
                    Activity.findAllByStatusAndAssessment('active',true)
            } else {
                activities = params.includeDeleted ? Activity.findAllByAssessment(false) :
                    Activity.findAllByStatusAndAssessment('active',false)
            }
            activities.each { act ->
                list << activityService.toMap(act)
            }
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def act = activityService.get(id, true)
            if (act) {
                asJson act
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    def delete(String id) {
        def a = Activity.findByActivityId(id)
        if (a) {
            if (params.destroy) {
                a.delete()
            } else {
                a.status = 'deleted'
                a.save(flush: true)
            }
            render (status: 200, text: 'deleted')
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    def update(String id) {
        def props = request.JSON
        log.debug props
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
        if (result.status == 'ok') {
            asJson(message)
        } else {
            //Activity.withSession { session -> session.clear() }
            log.error result.error
            render status:400, text: result.error
        }
    }
}
