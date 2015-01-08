package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.ProjectXlsExporter
import au.org.ala.ecodata.reporting.SummaryXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit

class SearchController {

    static final String PUBLISHED_ACTIVITIES_FILTER = 'publicationStatus:published'

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
        response.setContentType("application/json; charset=\"UTF-8\"")
        render res
    }

    def elasticHome() {
        def res = elasticSearchService.search(params.query, params, "homepage")
        response.setContentType("application/json; charset=\"UTF-8\"")
        render res
    }

    private def populateGeoInfo(markBy, hit, selectedFacetTerms){

        def geo = hit.source.geo
        if(!markBy)
            return geo

        def legendName, index
        def name =  hit.source[markBy.replaceAll("Facet", "")] ?: hit.source[markBy.replaceAll("Facet", "Name")] ?:""

        if(name){
            for(int i = 0; i < selectedFacetTerms.size(); i++){
                if(selectedFacetTerms[i].legendName.equals(name)){
                    legendName = selectedFacetTerms[i].legendName
                    index = selectedFacetTerms[i].index
                    selectedFacetTerms[i].count++
                    break;
                }
            }

            geo.each{ data ->
                data.legendName = legendName
                data.index = index
            }
        }
        else {
            hit.source.sites.each { site ->
                if(site.extent?.geometry) {
                    name =  site.extent?.geometry[markBy.replaceAll("Facet", "")] ?:
                            site.extent?.geometry[markBy.replaceAll("Facet", "Name")] ?: ""

                    if(name) {
                        for(int i = 0; i < selectedFacetTerms.size(); i++){
                            if(selectedFacetTerms[i].legendName.equals(name)){
                                legendName = selectedFacetTerms[i].legendName
                                index = selectedFacetTerms[i].index
                                selectedFacetTerms[i].count++
                                break;
                            }
                        }

                        geo.each{ data ->
                            if(data.siteId.equals(site.siteId)) {
                                data.legendName = legendName
                                data.index = index
                            }
                        }
                    }
                }
            }
        }

        geo
    }

    def elasticGeo() {
        def res = elasticSearchService.search(params.query, params, "homepage")
        def selectedFacetTerms = []
        def markBy = params.markBy

        if(markBy){
            res.facets.facets.each{ facet ->
                if(facet.key.equals(markBy)){
                    facet.value.eachWithIndex{ val, index ->
                        def data = [:]
                        data.legendName = val.term.toString()
                        data.index = index
                        data.count = 0
                        selectedFacetTerms << data
                    }
                }
            }
        }

        def geoRes = []

        res.hits.hits.each { hit ->
            if(hit.source?.geo) {
                def proj = [:]
                proj.projectId = hit.source.projectId
                proj.name = hit.source.name
                proj.org = hit.source.organisationName
                proj.geo = populateGeoInfo(markBy, hit, selectedFacetTerms)

                geoRes << proj
            }
        }
        response.setContentType("application/json; charset=\"UTF-8\"")
        def projectsAndTotal = ['total':res.hits.getTotalHits(),'projects':geoRes,'selectedFacetTerms':selectedFacetTerms]

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
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER]
        additionalFilters.addAll(filters)
        def results = reportService.aggregate(additionalFilters)
        render results as JSON
    }

    def report() {

        def filters = params.getList("fq")

        def results = reportService.runReport(filters, 'Green Army Monthly Summary', params)
        render results as JSON
    }

    @RequireApiKey
    def downloadAllData() {

        if (!params.max) {
            params.max = 5000
            params.offset = 0
        }

        SearchResponse res = elasticSearchService.search(params.query, params, "homepage")
        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.source.projectId) {
                ids << hit.source.projectId
            }
        }

        withFormat {
            json {
                List projects = ids.collect{projectService.get(it,ProjectService.ALL)}
                render projects as JSON
            }
            xlsx {
                XlsExporter exporter = new XlsExporter("results")
                exporter.setResponseHeaders(response)
                ProjectXlsExporter projectExporter = new ProjectXlsExporter(exporter, metadataService)

                List projects = ids.collect{projectService.get(it,ProjectService.ALL)}
                projectExporter.exportAll(projects)
                exporter.sizeColumns()

                exporter.save(response.outputStream)
            }
        }
    }

    @RequireApiKey
    def downloadSummaryData() {

        def defaultCategory = "Not categorized"
        def filters = params.getList("fq")
        def results = reportService.aggregate(filters)
        def scores = results.outputData
        def scoresByCategory = scores.groupBy{
            (it.score.category?:defaultCategory)
        }

        withFormat {
            json {
                render scoresByCategory as JSON
            }
            xlsx {
                XlsExporter exporter = new XlsExporter("results")
                exporter.setResponseHeaders(response)

                SummaryXlsExporter summaryXlsExporter = new SummaryXlsExporter(exporter)
                summaryXlsExporter.exportAll(scoresByCategory)
                exporter.sizeColumns()

                exporter.save(response.outputStream)
            }
        }
    }

    /** Temporary method to assist generating the user report.  Needs work */
    def userReport() {

        def users = reportService.userSummary()

        File out = new File('/Users/god08d/Documents/MERIT/users/userReport.csv')
        out.withWriter { writer ->
            writer.println("User Id, Name, Email, Role, Project ID, Grant ID, External ID, Project Name, Project Access Role")

            users.values().each { user->

                writer.print(user.userId+","+user.name+","+user.email+","+user.role+",")
                if (user.projects) {
                    boolean first = true
                    user.projects.each { project ->
                        if (!first) {
                            writer.print(",,,,")
                        }
                        writer.println(project.projectId+","+project.grantId+","+project.externalId+","+project.name+","+project.access)
                        first = false
                    }
                }


            }


        }
    }

    def exportProjectSitesAsShapefile() {

        if (!params.max) {
            params.max = 100
            params.offset = 0
        }
        def query = params.query
        if (!query) {
            query = '*'
        }

        SearchResponse res = elasticSearchService.search(query, params, "")

        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.source.projectId) {
                ids << hit.source.projectId
            }
        }

        def zipOut = reportService.exportShapeFile(ids)

        response.setContentType("application/zip")
        response.setHeader("Content-disposition", "filename=meritSites.zip")
        response.outputStream << zipOut.toByteArray()
        response.outputStream.flush()
    }

}
