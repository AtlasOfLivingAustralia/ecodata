package au.org.ala.ecodata

import grails.converters.JSON

class SearchController {
    def projectService
    def siteService
    def activityService
    def searchService
    def elasticSearchService

    def index(String query) {
        def list = searchService.findForQuery(query, params)
        render list as JSON
    }

    def elastic() {
        def res = elasticSearchService.search(params.query, params)
        response.setContentType("application/json")
        render res
    }

    def clearIndex() {
        log.debug "Clearing index"
        render elasticSearchService.deleteIndex()
    }

    def indexAll() {
        elasticSearchService.indexAll()


    }
}
