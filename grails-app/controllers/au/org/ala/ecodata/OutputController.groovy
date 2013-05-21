package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class OutputController {

    def outputService, commonService

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
        def result
        def message
        if (id) {
            result = outputService.update(props,id)
            message = [message: 'updated']
        }
        else {
            result = outputService.create(props)
            message = [message: 'created', outputId: result.outputId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            //Output.withSession { session -> session.clear() }
            log.error result.error
            render status:400, text: result.error
        }
    }
}
