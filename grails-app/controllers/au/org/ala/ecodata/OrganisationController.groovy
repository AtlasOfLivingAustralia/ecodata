package au.org.ala.ecodata

import grails.converters.JSON

import static au.org.ala.ecodata.ElasticIndex.DEFAULT_INDEX
/**
 * Exposes web services to perform CRUD operations on an organisation.
 */
class OrganisationController {
    static responseFormats = ['json', 'xml']
    OrganisationService organisationService
    ElasticSearchService elasticSearchService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        //response.setContentType("application/json;charset=UTF-8")
        render model as JSON
    }

    def get(String id) {
        def includeDeleted = params.boolean('includeDeleted', false)

        if (!id) {
            if(params.name) {
                def p = organisationService.findByName(params.name)
                if (p) {
                    asJson p
                } else {
                    render (status: 404, text: 'No such organisation name')
                }
            } else {
                // When listing all organisations don't add any level of details as it could slow down the system
                // It is ok for retrieving individual organisations though.
                def list = organisationService.list([])
                list.sort {it.name}
                asJson([list: list])
            }
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
            log.error result.errors.toString()
            render status:400, text: result.errors
        }
    }


    /**
     * Deprecated since these APIs are not used by mobile apps.
     */
    @Deprecated
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

    def organisationMetrics(String id) {

        def organisation = Organisation.findByOrganisationId(id)

        boolean approvedOnly = true
        boolean targetsOnly = false
        boolean includeTargets = true
        List scoreIds
        Map aggregationConfig = null

        Map paramData = request.JSON
        if (!paramData) {
            approvedOnly = params.getBoolean('approvedOnly')
            scoreIds = params.getList('scoreIds')
            targetsOnly = params.getBoolean('targetsOnly')
            includeTargets = params.getBoolean('includeTargets', true)
        }
        else {

            if (paramData.approvedOnly != null) {
                approvedOnly = paramData.approvedOnly
            }
            if (paramData.targetsOnly != null) {
                approvedOnly = paramData.targetsOnly
            }
            if (paramData.includeTargets != null) {
                includeTargets = paramData.includeTargets
            }
            scoreIds = paramData.scoreIds
            aggregationConfig = paramData.aggregationConfig
        }

        if (organisation) {
            render organisationService.organisationMetrics(id, targetsOnly, approvedOnly, scoreIds, aggregationConfig, includeTargets) as JSON

        } else {
            render (status: 404, text: 'No such id')
        }
    }

}
