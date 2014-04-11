package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.ProjectXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit

class SearchController {
    def searchService
    def elasticSearchService
    def reportService
    def projectService
    def metadataService


    def index(String query) {
        def list = searchService.findForQuery(query, params)
        render list as JSON
    }

    def elastic() {
        def res = elasticSearchService.search(params.query, params, "")
        //log.debug "res = ${res}"
        response.setContentType("application/json; charset=\"UTF-8\"")
        //response.setCharacterEncoding("UTF-8")
        render res
    }

    def elasticHome() {
        def res = elasticSearchService.search(params.query, params, "homepage")
        response.setContentType("application/json; charset=\"UTF-8\"")
        render res
    }

    def elasticGeo() {
        def res = elasticSearchService.search(params.query, params, "homepage")
        def geoRes = []
        res.hits.hits.each { hit ->
            if(hit.source?.geo){
                def proj = [:]
                proj.projectId =hit.source.projectId
                proj.name = hit.source.name
                proj.org = hit.source.organisationName
                proj.geo = hit.source.geo
                geoRes << proj
            }
        }
        response.setContentType("application/json; charset=\"UTF-8\"")
        def projectsAndTotal = ['total':res.hits.getTotalHits(),'projects':geoRes]
        render projectsAndTotal as JSON
    }

    def elasticPost() {
        def paramsObj = request.JSON
        def paramMap = new GrailsParameterMap(paramsObj, request)
        log.debug "paramMap = ${paramMap}"

        if (paramMap) {
            def res = elasticSearchService.search(paramMap.query, paramMap, "")
            response.setContentType("application/json; charset=\"UTF-8\"")
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

    def dashboardReport() {
        def filters = params.getList("fq")
        def results = reportService.aggregate(filters)
        render results as JSON
    }

    def downloadSearchResults() {
        params.max = 1000
        params.offset = 0

        SearchResponse res = elasticSearchService.search(params.query, params, "")

        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.source.projectId) {
                ids << hit.source.projectId
            }
        }

        XlsExporter exporter = new XlsExporter("results")
        exporter.setResponseHeaders(response)
        ProjectXlsExporter projectExporter = new ProjectXlsExporter(exporter, metadataService)


        ids.each{
            def project = projectService.get(it,ProjectService.ALL)
            projectExporter.export(project)
        }
        exporter.sizeColumns()

        exporter.save(response.outputStream)
    }

}
