package au.org.ala.ecodata

import grails.converters.JSON

class SearchController {
    def searchService

    def index(String query) {
        def list = searchService.findForQuery(query, params)
        render list as JSON
    }
}
