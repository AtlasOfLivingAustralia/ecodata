package au.org.ala.ecodata

import grails.converters.JSON

import static au.org.ala.ecodata.Status.DELETED

class OutputController {

    def outputService, commonService
    static final SCORES = 'scores'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        //response.setContentType("application/json; charset=\"UTF-8\"")
        render model as JSON
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
        log.debug "${props}"
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
            log.error result.error.toString()
            render status:400, text: result.error
        }
    }

    /**
     * list all output for an activity id
     * @param activityId
     */
    def list(){
        String activityId = params.activityId
        List outputs;
        if(activityId){
            outputs = outputService.listAllForActivityId(activityId)
            render text: outputs as JSON, contentType: 'application/json'
        } else {
            render( status: 404, text: 'Not found');
        }
    }

    /**
     * Get unique id associated with ecodata server seed.
     *
     * @return uuid
     */
    def getOutputSpeciesUUID(){
        Map results = [outputSpeciesId: UUID.randomUUID().toString()]
        render text: results as JSON, contentType: 'application/json'
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
     * If a property is supplied that isn't a property of the project, it will not cause
     * an error, but no results will be returned.  (this is an effect of mongo allowing
     * a dynamic schema)
     *
     * @return a list of the outputs that match the supplied criteria
     */
    @RequireApiKey
    def search() {
        def searchCriteria = request.JSON

        def outputList = outputService.search(searchCriteria)
        asJson outputs:outputList
    }

}
