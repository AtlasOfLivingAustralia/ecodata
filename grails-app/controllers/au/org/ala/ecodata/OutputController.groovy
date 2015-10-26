package au.org.ala.ecodata

class OutputController {

    def outputService, commonService
    static final SCORES = 'scores'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json; charset=\"UTF-8\"")
        model
    }

    def index() {
        log.debug "Total outputs = ${Output.count()}"
        render "${Output.count()} outputs"
    }

    def get(String id) {
        def detail = params.view == SCORES ? [SCORES] : []
        if (!id) {
            def list = []
            def outputs = params.includeDeleted ? Output.list() :
                Output.findAllByStatus('active')
            outputs.each { o ->
                list << outputService.toMap(o, detail)
            }
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def a = Output.findByOutputId(id)
            if (a) {
                asJson outputService.toMap(a, detail)
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    @RequireApiKey
    def delete(String id) {
        Output output = Output.findByOutputId(id)
        if (output) {
            boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()
            Map result = outputService.delete(id, destroy)
            if (!result.error) {
                render(status: 200, text: 'deleted')
            }
            else {
                response.status = 500
                render status:500, text:result.error
            }
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    @RequireApiKey
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
