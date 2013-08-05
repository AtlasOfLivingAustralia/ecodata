package au.org.ala.ecodata

import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

class SearchController {
    def searchService
    def elasticSearchService

    def index(String query) {
        def list = searchService.findForQuery(query, params)
        render list as JSON
    }

    def elastic() {
        def res = elasticSearchService.search(params.query, params, "")
        response.setContentType("application/json")
        render res
    }

    def elasticHome() {
        def res = elasticSearchService.search(params.query, params, "homepage")
        response.setContentType("application/json")
        render res
    }

    def elasticPost() {
        def paramsObj = request.JSON
        def paramMap = new GrailsParameterMap(paramsObj, request)
        log.debug "paramMap = ${paramMap}"

        if (paramMap) {
            def res = elasticSearchService.search(paramMap.query, paramMap, "")
            response.setContentType("application/json")
            render res
        } else {
            def msg = [error: "Required JSON body not found"]
            render msg as JSON
        }
    }

    def clearIndex() {
        log.debug "Clearing index"
        render elasticSearchService.deleteIndex()
    }

    def indexAll() {
        render elasticSearchService.indexAll() as JSON
    }
}
