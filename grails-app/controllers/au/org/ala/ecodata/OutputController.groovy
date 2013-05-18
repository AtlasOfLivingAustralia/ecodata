package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class OutputController {

    def outputService, commonService

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    def index() {
        log.debug "Total outputs = ${Output.count()}"
        render "${Output.count()} outputs"
    }

    def get(String id) {
        if (!id) {
            def list = []
            Output.list().each { act ->
                list << outputService.toMap(act)
            }
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def a = Output.findByOutputId(id)
            if (a) {
                asJson outputService.toMap(a)
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    def delete(String id) {
        def a = Output.findByOutputId(id)
        if (a) {
            a.delete()
            asJson([message: 'deleted'])
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    def update(String id) {
        def props = request.JSON
        log.debug props
        if (id) {
            def a = Output.findByOutputId(id)
            if (a) {
                try {
                    commonService.updateProperties(a, props)
                    asJson([message: 'updated'])
                } catch (Exception e) {
                    Output.withSession { session -> session.clear() }
                    log.error "Error updating output ${id} - ${e.message}"
                    render status:400, text: e.message
                }
            } else {
                log.error "Error updating output - no such id ${id}"
                render status:404, text: 'No such id'
            }
        }
        else {
            // no id - create the resource
            def activity = Activity.findByActivityId(props.activityId)
            if (activity) {
                def o = new Output(activityId: activity.activityId, outputId: Identifiers.getNew(true,''))
                try {
                    commonService.updateProperties(o, props)
                    activity.addToOutputs(o)
                    //activity.outputs << o.outputId
                    activity.save()
                    asJson([message: 'created', outputId: o.outputId])
                } catch (Exception e) {
                    // clear session to avoid exception when GORM tries to autoflush the changes
                    Output.withSession { session -> session.clear() }
                    log.error "Error creating output for ${props.activityId} - ${e.message}"
                    render status:400, text: e.message
                }
            } else {
                log.error "Error creating output - no activity with id = ${props.activityId}"
                render status:400, text: 'No such activity'
            }
        }
    }
}
