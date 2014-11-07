package au.org.ala.ecodata

/**
 * Exposes web services to perform CRUD operations on an organisation.
 */
class OrganisationController {

    def organisationService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

    def get(String id) {
        def includeDeleted = params.boolean('includeDeleted', false)
        def levelOfDetail = ['documents']
        if (params.view == 'all') {
            levelOfDetail << 'projects'
        }

        if (!id) {
            def list = organisationService.list(levelOfDetail)
            list.sort {it.name}
            asJson([list: list])
        } else {
            def p = organisationService.get(id, levelOfDetail, includeDeleted)
            if (p) {
                asJson p
            } else {
                render (status: 404, text: 'No such id')
            }
        }
    }

    @RequireApiKey
    def update(String id) {
        def props = request.JSON
        def result, message
        if (id) {
            result = organisationService.update(id, props)
            message = [message: 'updated']
        }
        else {
            result = organisationService.create(props)
            message = [message: 'created', organisationId: result.organisationId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            log.error result.error
            render status:400, text: result.errors
        }
    }


}
