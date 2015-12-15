package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.SummaryXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.converters.JSON
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit

import static au.org.ala.ecodata.ElasticIndex.*
import java.text.SimpleDateFormat

class SearchController {

    static final String PUBLISHED_ACTIVITIES_FILTER = 'publicationStatus:published'

    SearchService searchService
    ElasticSearchService elasticSearchService
    ReportService reportService
    ProjectService projectService
    MetadataService metadataService
    DocumentService documentService
    ActivityService activityService
    SiteService siteService
    DownloadService downloadService

    def index(String query) {
        def list = searchService.findForQuery(query, params)
        render list as JSON
    }

    def elastic() {
        def res = elasticSearchService.search(params.query, params, DEFAULT_INDEX)
        response.setContentType("application/json; charset=\"UTF-8\"")
        render res
    }

    def elasticHome() {
        Map geoSearch = null
        if (params.geoSearchJSON) {
            geoSearch = new JsonSlurper().parseText(params.geoSearchJSON)
        }
        def res = elasticSearchService.search(params.query, params, HOMEPAGE_INDEX, geoSearch)
        response.setContentType("application/json; charset=\"UTF-8\"")
        render res
    }

    /*
    * Searches the given query in project activity context.
    * Requires API key to prevent unauthorized access to embargoed records.
    */
    @RequireApiKey
    def elasticProjectActivity(){
        elasticSearchService.buildProjectActivityQuery(params)
        def res = elasticSearchService.search(params.query, params, PROJECT_ACTIVITY_INDEX)
        response.setContentType("application/json; charset=\"UTF-8\"")
        render res
    }

    private def populateGeoInfo(markBy, hit, selectedFacetTerms){

        def geo = hit.source.geo
        if(!markBy) {
            geo[0].geometry = hit.source.sites[0].extent.geometry
            return geo
        }

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

    def scoresByLabel() {
        def scores = params.getList("scores")

        def filters = params.getList("fq")
        def searchTerm = params.query
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER]
        additionalFilters.addAll(filters)
        def results = reportService.aggregate(additionalFilters, searchTerm, reportService.findScoresByLabel(scores))
        render results as JSON
    }

    def targetsReportByScoreLabel() {
        def scoreLabels = params.getList("scores")
        def scores = reportService.findScoresByLabel(scoreLabels)
        def filters = params.getList("fq")
        def searchTerm = params.query
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER]

        additionalFilters.addAll(filters)
        def targets = reportService.outputTargetsBySubProgram(params, scores)
        def scoresReport = reportService.outputTargetReport(additionalFilters, searchTerm, scores)

        def results = [scores:scoresReport, targets:targets]
        render results as JSON
    }

    def targetsReport() {
        def filters = params.getList("fq")
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER]
        additionalFilters.addAll(filters)
        def targets = reportService.outputTargetsBySubProgram(params)
        def scores = reportService.outputTargetReport(additionalFilters)

        def results = [scores:scores, targets:targets]
        render results as JSON
    }

    def report() {

        def filters = params.getList("fq")

        def results = reportService.runReport(filters, 'Green Army Monthly Summary', params)
        render results as JSON
    }

    def downloadProjectDataFile() {
        if (!params.id) {
            response.setStatus(400)
            render "A download ID is required"
        } else {
            File file = new File("${grailsApplication.config.temp.dir}${File.separator}${params.id}.zip")
            if (file) {
                response.setContentType(ContentType.BINARY.toString())
                response.setHeader('Content-Disposition', 'Attachment;Filename="data.zip"')

                file.withInputStream { i -> response.outputStream << i }
            } else {
                response.setStatus(404)
                render "No download was found for id ${params.id}"
            }
        }
    }

    @RequireApiKey
    def downloadAllData() {
        defaultDownloadQueryParams(params)

        if (params.containsKey("isMerit") && !params.isMerit.toBoolean()) {
            if (params.async?.toBoolean()) {
                if (!params.email) {
                    response.setStatus(400)
                    render "An email address must be provided for asynchronous downloads"
                } else {
                    downloadService.downloadProjectDataAsync(params)

                    response.setStatus(200)
                    render "OK"
                }
            } else {
                response.setContentType(ContentType.BINARY.toString())
                response.setHeader('Content-Disposition', 'Attachment;Filename="data.zip"')

                downloadService.downloadProjectData(response.outputStream, params)
            }
        } else {
            downloadMeritData(params)
        }
    }

    private static defaultDownloadQueryParams(params) {
        if (!params.max) {
            params.max = 5000
            params.offset = 0
        }
    }

    void downloadMeritData(GrailsParameterMap params) {
        Set ids = downloadService.getProjectIdsForDownload(params, HOMEPAGE_INDEX)

        withFormat {
            json {
                List projects = ids.collect { projectService.get(it, ProjectService.ALL) }
                render projects as JSON
            }
            xlsx {
                XlsExporter exporter = downloadService.exportProjectsToXls(ids, true)
                exporter.setResponseHeaders(response)

                exporter.save(response.outputStream)
            }
        }
    }

    @RequireApiKey
    def downloadSummaryData() {

        def defaultCategory = "Not categorized"
        def filters = params.getList("fq")
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER] + filters

        def results = reportService.aggregate(additionalFilters)
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

    @RequireApiKey
    def downloadShapefile() {

        if (!params.max) {
            params.max = 1000
            params.offset = 0
        }
        def query = params.query
        if (!query) {
            query = '*'
        }

        SearchResponse res = elasticSearchService.search(query, params, "homepage")

        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.source.projectId) {
                ids << hit.source.projectId
            }
        }

        if (ids.size() > 0) {

            SimpleDateFormat format = new SimpleDateFormat('yyyy-MM-dd')
            def name = 'meritSites-' + format.format(new Date())
            response.setContentType("application/zip")
            response.setHeader("Content-disposition", "filename=${name}.zip")
            reportService.exportShapeFile(ids, name, response.outputStream)
            response.outputStream.flush()
        }
        else {
            response.setStatus(400)
            render "No project sites selected for download"
        }
    }

}
