package au.org.ala.ecodata

import static au.org.ala.ecodata.ElasticIndex.DEFAULT_INDEX

/**
 * Exposes web services to perform CRUD operations on an organisation.
 */
class OrganisationController {

    OrganisationService organisationService
    ElasticSearchService elasticSearchService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

    def get(String id) {
        def includeDeleted = params.boolean('includeDeleted', false)

        // When listing all organisations don't add any level of details as it could slow down the system
        // It is ok for retrieving individual organisations though.
        if (!id) {
            def list = organisationService.list([])
            list.sort {it.name}
            asJson([list: list])
        } else {
            def levelOfDetail = ['documents']
            if (params.view == 'all') {
                levelOfDetail << 'projects'
            }
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
            log.error result.errors
            render status:400, text: result.errors
        }
    }

    @PreAuthorise
    def search() {

        String error = ""

        if (params.max && !params.max.isNumber()) {
            error = "Invalid max parameter."
        } else if (params.offset && !params.offset.isNumber()) {
            error = "Invalid offset parameter."
        } else if (params.sort) {
            List options = ['nameSort', '_score', 'organisationSort']
            String found = options.find { it == params.sort }
            error = !found ? 'Invalid sort parameter (Accepted values: nameSort, _score, organisationSort ).' : ''
        }

        if (!error) {
            Map args = buildParams(params)
            response.setContentType('application/json; charset="UTF-8"')
            render elasticSearchService.search(args.query, args, DEFAULT_INDEX)
        } else {
            render status: 400, text: error
        }
    }


    private Map buildParams(Map params) {

        Map values = [:]
        values.offset = params.offset ?: 0
        values.max = params.max ?: 10
        values.query = params.query ?: "*:*"
        values.highlight = params.highlight ?: true
        values.flimit = 999
        values.sort = params.sort ?: 'nameSort'
        values.fq = "className:au.org.ala.ecodata.Organisation"

        values
    }

}
