package au.org.ala.ecodata

import au.org.ala.ecodata.command.UserSummaryReportCommand
import au.org.ala.ecodata.reporting.*
import au.org.ala.web.AlaSecured
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import java.text.SimpleDateFormat

import static au.org.ala.ecodata.ElasticIndex.*

@Slf4j
class SearchController {

    static responseFormats = ['json', 'xml']

    static final String PUBLISHED_ACTIVITIES_FILTER = 'publicationStatus:published'

    SearchService searchService
    ElasticSearchService elasticSearchService
    ReportService reportService
    ProjectService projectService
    MetadataService metadataService
    DocumentService documentService
    ActivityService activityService
    SiteService siteService
    UserService userService
    DownloadService downloadService
    PermissionService permissionService
    SensitiveSpeciesService sensitiveSpeciesService
    ReportingService reportingService
    OrganisationService organisationService
    MapService mapService
    ManagementUnitService managementUnitService

    def index(String query) {
        def list = searchService.findForQuery(query, params)
        render list as JSON
    }

    def elastic() {
        if (params.terms) {
            params.terms = JSON.parse( params.terms)
        }

        def res = elasticSearchService.search(params.query, params, DEFAULT_INDEX)
        respond searchResponse:res
    }

    def elasticHome() {
        Map geoSearch = null
        if (params.geoSearchJSON) {
            geoSearch = new JsonSlurper().parseText(params.geoSearchJSON)
        }
        def res = elasticSearchService.search(params.query, params, HOMEPAGE_INDEX, geoSearch)
        respond searchResponse:res
    }

    /*
    * Searches the given query in project activity context.
    * Requires API key to prevent unauthorized access to embargoed records.
    */
    @RequireApiKey
    def elasticProjectActivity(){
        def res
        if (params?.version) {
            //search auditMessage
            res = (auditMessageSearch(params) as JSON).toString()
            response.setContentType("application/json; charset=\"UTF-8\"")
            render res
        } else {
            elasticSearchService.buildProjectActivityQuery(params)
            res = elasticSearchService.search(params.query, params, PROJECT_ACTIVITY_INDEX)
            respond searchResponse:res
        }

    }

    def getHeatmap () {
        def res, index, geohashField, boundingBoxField
        switch (params.dataType) {
            case MapService.PROJECT_TYPE:
                geohashField = "projectArea.geoPoint"
                boundingBoxField = "projectArea.geoIndex"
                index = HOMEPAGE_INDEX
                break
            default:
                elasticSearchService.buildProjectActivityQuery(params)
                geohashField = "sites.geoPoint"
                boundingBoxField = "sites.geoIndex"
                index = PROJECT_ACTIVITY_INDEX
                break
        }


        res = elasticSearchService.searchAndAggregateOnGeohash(params.query, params, geohashField, boundingBoxField, index)
        Map features = mapService.getFeatureCollectionFromSearchResult(res)
        features = mapService.setHeatmapColour(features)
        response.setContentType("application/json; charset=\"UTF-8\"")
        render features as JSON
    }

    /*
    * AuditMessage search that is equivalent to the elastic search with a version
    *
    *       elasticSearchService.buildProjectActivityQuery(params)
    *       res = elasticSearchService.search(params.query, params, PROJECT_ACTIVITY_INDEX)
    *
     */
    private def auditMessageSearch(params) {
        String userId = params.userId
        String projectId = params.projectId
        List<String> projectsTheUserIsAMemberOf

        //find project activities
        def all
        if (projectId) {
            all = AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(projectId, ProjectActivity.class.name, new Date(params.version as Long), [sort:'date', order:'desc'])
        } else {
            all = AuditMessage.findAllByEntityTypeAndDateLessThanEquals(ProjectActivity.class.name, new Date(params.version as Long), [sort:'date', order:'desc'])
        }
        def projectActivities = []
        def found = []
        all.each {
            if (!found.contains(it.entityId)) {
                found << it.entityId

                if (it.eventType == AuditEventType.Update || it.eventType == AuditEventType.Insert) {
                    def added = false

                    it.entity.lastUpdated = it.date

                    switch (params.view) {

                        case 'myrecords':
                            if (userId && it.userId == userId) {
                                projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                added = true
                            }
                            break

                        case 'project':
                            if (projectId) {
                                if (userId && permissionService.isUserAlaAdmin(userId) || permissionService.isUserAdminForProject(userId, projectId) || permissionService.isUserEditorForProject(userId, projectId)) {
                                    projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                    added = true
                                } else if (userId && (!it.entity.embargoed || it.userId == userId)) {
                                    projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                    added = true
                                } else if (!userId && !it.entity.embargoed) {
                                    projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                    added = true
                                }
                            }
                            break

                        case 'allrecords':
                            if (!projectId) {
                                if (userId && permissionService.isUserAlaAdmin(userId)) {
                                    projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                    added = true
                                } else if (userId) {
                                    if (!projectsTheUserIsAMemberOf) projectsTheUserIsAMemberOf = permissionService.getProjectsForUser(userId, AccessLevel.admin, AccessLevel.editor)

                                    if ((!projectsTheUserIsAMemberOf || projectsTheUserIsAMemberOf.contains(it.projectId)) &&
                                            (!it.entity.embargoed || it.userId == userId)) {
                                        projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                        added = true
                                    }
                                } else if (!userId && !it.entity.embargoed) {
                                    projectActivities << elasticSearchService.prepareActivityForIndexing(it.entity, params?.version)
                                    added = true
                                }
                            }
                            break
                    }

                    if (!added) {
                        if (!it.entity.embargoed) {
                            projectActivities << it.entity
                        }
                    }
                }
            }
        }

        [hits: [hits: projectActivities.collect { [_source: it]}, total: projectActivities.size() ]]
    }

    private String propertyNameForFacet(String facet) {
        String facetSuffix = "Facet"
        String result = facet
        if (facet?.endsWith(facetSuffix)) {
            result = facet.substring(0, facet.indexOf(facetSuffix))
        }
        result
    }

    private def populateGeoInfo(markBy, hit, selectedFacetTerms){

        def geo = hit.sourceAsMap.geo
        if(!markBy) {
            geo[0].geometry = hit.sourceAsMap.sites[0].extent.geometry
            return geo
        }

        def legendName, index
        // When fields are indexed, "Facet" or "Name" is appended to the field name.
        String propertyName = propertyNameForFacet(markBy)

        def facetValue = hit.sourceAsMap[propertyName] ?:""

        if (facetValue) {
            // Geographic facets will be List typed (as a site can be in more than one state for example)
            // We have to assign the site to a category, so we'll just pick the first one.
            if (facetValue instanceof List) {
                facetValue = facetValue[0]
            }
            for(int i = 0; i < selectedFacetTerms.size(); i++){
                if(selectedFacetTerms[i].legendName.equals(facetValue)){
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
            hit.sourceAsMap.sites.each { site ->
                if(site.extent?.geometry) {
                    facetValue =  site.extent?.geometry[propertyName] ?: ""

                    if(facetValue) {
                        // Geographic facets will be List typed (as a site can be in more than one state for example)
                        // We have to assign the site to a category, so we'll just pick the first one.
                        if (facetValue instanceof List) {
                            facetValue = facetValue[0]
                        }
                        for(int i = 0; i < selectedFacetTerms.size(); i++){
                            if(selectedFacetTerms[i].legendName.equals(facetValue)){
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
        Map geoSearch = null
        if (params.geoSearchJSON) {
            geoSearch = new JsonSlurper().parseText(params.geoSearchJSON)
        }
        String markBy = params.markBy
        params.include = ['projectId', 'geo', 'name', 'organisationName', 'sites.extent', 'sites.siteId']
        if (markBy) {
            // Field name by convention is the markBy minus the word "Facet"
            params.include << propertyNameForFacet(markBy)
        }
        SearchResponse res = elasticSearchService.search(params.query, params, ElasticIndex.HOMEPAGE_INDEX, geoSearch)
        List selectedFacetTerms = []

        if (markBy) {
            ParsedTerms toMarkBy = res.aggregations.find { it.name == markBy }
            if (toMarkBy) {
                List buckets = toMarkBy.buckets
                buckets.eachWithIndex{ Terms.Bucket entry, int i ->
                    Map data = [:]
                    data.legendName = entry.key
                    data.index = i
                    data.count = entry.docCount
                    selectedFacetTerms << data
                }
            }
        }

        def geoRes = []

        SearchHits hits = res.hits
        SearchHit[] moreHits = hits.hits
        for (SearchHit hit in moreHits) {
            if (hit.sourceAsMap?.geo) {
                def proj = [:]
                proj.projectId = hit.sourceAsMap.projectId
                proj.name = hit.sourceAsMap.name
                proj.org = hit.sourceAsMap.organisationName
                proj.geo = populateGeoInfo(markBy, hit, selectedFacetTerms)

                geoRes << proj
            }
        }
        response.setContentType("application/json; charset=\"UTF-8\"")
        def projectsAndTotal = ['total':res.hits.totalHits.value,'projects':geoRes,'selectedFacetTerms':selectedFacetTerms]

        render projectsAndTotal as JSON
    }

    def elasticPost() {
        def paramsObj = request.JSON
        def paramMap = new GrailsParameterMap(paramsObj, request)
        log.debug "paramMap = ${paramMap}"

        if (paramMap) {
            SearchResponse res = elasticSearchService.search(paramMap.query, paramMap, ElasticIndex.DEFAULT_INDEX)
            respond searchResponse:res
        } else {
            def msg = [error: "Required JSON body not found"]
            render msg as JSON
        }
    }

    def indexAll() {
        render (elasticSearchService.indexAll()?:[]) as JSON
    }

    def dashboardReport() {

        def filters = params.getList("fq")
        List<Score> scores = Score.findAll()
        def results = reportService.aggregate(filters, params.query ?: "*:*", scores)
        render results as JSON
    }

    def scoresByLabel() {
        def scores = params.getList("scores")

        def filters = params.getList("fq")
        def searchTerm = params.query ?: "*:*"

        def results = reportService.aggregate(filters, searchTerm, reportService.findScoresByLabel(scores))
        render results as JSON
    }

    def targetsReportForScoreIds() {
        def scoreIds = params.getList("scoreIds")
        def scores = reportService.findScoresByScoreId(scoreIds)

        Map results = targetsReportForScores(scores, params)
        render results as JSON
    }

    def targetsReportByScoreLabel() {
        def scoreLabels = params.getList("scores")
        def scores = reportService.findScoresByLabel(scoreLabels)

        Map results = targetsReportForScores(scores, params)
        render results as JSON
    }

    private def targetsReportForScores(List scores, params) {
        List filters = params.getList("fq")
        String searchTerm = params.query ?: "*:*"
        boolean approvedActivitiesOnly = params.getBoolean('approvedActivitiesOnly', true)
        def targets = reportService.outputTargetsBySubProgram(params, scores)
        def scoresReport = reportService.aggregate(filters, searchTerm, scores, null, approvedActivitiesOnly)

        def results = [scores:scoresReport, targets:targets]
        return results
    }

    def targetsReport() {
        def filters = params.getList("fq")
        def searchTerm = params.query ?: "*:*"

        def targets = reportService.outputTargetsBySubProgram(params)
        def scores = reportService.outputTargetReport(filters, searchTerm)

        def results = [scores:scores, targets:targets]
        render results as JSON
    }

    @RequireApiKey
    def activityReport() {
        Map params = request.JSON
        def approvedOnly = params.approvedActivitiesOnly
        def results = reportService.runActivityReport(params.query ?: "*:*", params.fq, params.reportConfig, approvedOnly)
        render results as JSON
    }


    @Deprecated
    /**
     *  Use DownloadController instead
    */

    def downloadProjectDataFile() {
        if (!params.id) {
            response.setStatus(400)
            render "A download ID is required"
        } else {
            String extension = params.fileExtension ?: 'zip'
            File file = new File("${grailsApplication.config.temp.dir}${File.separator}${params.id}.${extension}")
            if (file) {
                if (extension.toLowerCase() == "zip") {
                    response.setContentType("application/zip")
                } else {
                    response.setContentType("application/octet-stream")
                }

                response.setHeader('Content-Disposition', 'Attachment;Filename="data.'+extension+'"')

                file.withInputStream { i -> response.outputStream << i }
            } else {
                response.setStatus(404)
                render "No download was found for id ${params.id}"
            }
        }
    }

    @RequireApiKey
    def downloadAllData() {
        if (params.containsKey("isMerit") && !params.isMerit.toBoolean()) {
            params.max = 10000
            params.offset = 0

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
                response.setContentType("application/zip")
                response.setHeader('Content-Disposition', 'Attachment;Filename="data.zip"')

                downloadService.downloadProjectData(response.outputStream, params)
            }
        } else {
            downloadProjectData(params)
            response.setStatus(200)
            render "OK"
        }
    }

    void downloadProjectData(GrailsParameterMap params) {
        if (!params.max) {
            params.max = 5000
            params.offset = 0
        }

        Set ids = downloadService.getProjectIdsForDownload(params, HOMEPAGE_INDEX)


        if (!params.email) {
            params.email = userService.getCurrentUserDetails().userName
        }
        log.info("Download requested: "+params.email+", Project count: "+ids?.size()+", Tabs: "+params.tabs)
        params.fileExtension = "xlsx"
        Closure doDownload = { OutputStream outputStream, GrailsParameterMap paramMap ->

            File file = File.createTempFile("download", "xlsx")
            XlsExporter xlsExporter
            ProjectExporter projectExporter
            if (params.reportType == 'works') {
                xlsExporter = new XlsExporter(file.name)
                projectExporter = worksProjectExporter(xlsExporter, params)
            }
            else {
                xlsExporter = new StreamingXlsExporter(file.name)
                projectExporter = meritProjectExporter(xlsExporter, params)
            }
            exportProjectsToXls(ids, projectExporter)
            xlsExporter.save(outputStream)
        }
        downloadService.downloadProjectDataAsync(params, doDownload)
    }

    protected ProjectExporter meritProjectExporter(XlsExporter xlsExporter, GrailsParameterMap params) {
        String ELECTORATES = 'electFacet'
        params.facets = ELECTORATES
        SearchResponse result = elasticSearchService.search(params.query, params, HOMEPAGE_INDEX)
        List<String> electorates = result.aggregations?.find{it.name == ELECTORATES}?.buckets?.collect{it.key}
        List tabsToExport = params.getList('tabs')
        boolean formSectionPerTab = params.getBoolean("formSectionPerTab", false)
        Map metadata = [:].withDefault {
            DataDescription.findByXlsxName(it)
        }
        return new ProjectXlsExporter(projectService, xlsExporter, tabsToExport, electorates, managementUnitService, metadata, formSectionPerTab)
    }

    private ProjectExporter worksProjectExporter(XlsExporter xlsExporter, GrailsParameterMap params) {
        return new WorksProjectXlsExporter(xlsExporter, [:], TimeZone.getDefault())
    }

    private XlsExporter exportProjectsToXls(Set<String> projectIds, ProjectExporter projectExporter) {
        long start = System.currentTimeMillis()

        Project.withSession { session ->
            int batchSize = 50
            List projects = new ArrayList(batchSize)
            for (int i = 0; i < projectIds.size(); i++) {
                Map project =  projectService.get(projectIds[i], ProjectService.ALL)
                if (project)
                    projects << project
                else
                    log.warn(projectIds[i] + ' cannot be found!')

                if (i % batchSize == batchSize - 1 || i == projectIds.size() - 1) {
                    projectExporter.exportAllProjects(projects)
                    projects.clear()
                    session.clear()

                    log.info "Exported ${i + 1} of ${projectIds.size()} projects..."
                }
            }
        }
        log.info "Exporting ${projectIds.size()} projects took ${System.currentTimeMillis() - start} millis"
    }

    def downloadOrganisationData() {
        if (!params.email) {
            params.email = userService.getCurrentUserDetails().userName
        }
        params.max = 10000
        params.offset = 0
        params.fileExtension = "xlsx"

        Collection<String> orgIds = downloadService.getProjectIdsForDownload(params, DEFAULT_INDEX, 'organisationId')
        Closure doDownload = { OutputStream outputStream, GrailsParameterMap paramMap ->

            XlsExporter exporter = exportOrganisationsToXls(orgIds, paramMap.getList('tabs'))
            exporter.save(outputStream)
        }
        downloadService.downloadProjectDataAsync(params, doDownload)

        response.status = 200
        render "OK"
    }

    private XlsExporter exportOrganisationsToXls(Collection<String> organisationIds, List<String> tabs) {
        File file = File.createTempFile("download", "xlsx")
        XlsExporter xlsExporter = new XlsExporter(file.name)

        OrganisationXlsExporter exporter = new OrganisationXlsExporter(xlsExporter, tabs, [:])

        Organisation.withSession { session ->
            int batchSize = 50

            for (int i = 0; i < organisationIds.size(); i++) {
                String organisationId = organisationIds[i]
                Map organisation = organisationService.get(organisationId)
                if (organisation) {
                    organisation.reports = reportingService.findAllByOwner('organisationId', organisationId)


                    exporter.export(organisation)
                    if (i % batchSize == 1) {
                        session.clear()
                    }

                    log.info "Exported ${i + 1} of ${organisationIds.size()} organisations..."
                }
            }
        }
        xlsExporter
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

    @RequireApiKey
    def downloadUserList(UserSummaryReportCommand userSummaryReportCommand) {

        if (userSummaryReportCommand.hasErrors()) {
            respond userSummaryReportCommand.errors
            return
        }
        log.info("User "+userService.getCurrentUserDisplayName()+" requested the user summary report for hub "+userSummaryReportCommand.hubId)
        String hubId = userSummaryReportCommand.hubId

        Closure doDownload = { OutputStream outputStream, GrailsParameterMap paramMap ->
            try {
                outputStream.withPrintWriter { writer ->
                    reportService.userSummary(hubId, writer)
                }
            }
            catch (Exception e) {
                log.error("There was an error running the user report for hubId "+hubId, e)
            }
        }
        downloadService.downloadProjectDataAsync(userSummaryReportCommand.populateParams(params), doDownload)

        response.status = 200
        render "OK"
    }

    @RequireApiKey
    def downloadShapefile() {

        if (!params.max) {
            params.max = 1000
            params.offset = 0
        }
        if (!params.email) {
            params.email = userService.getCurrentUserDetails().userName
        }
        params.fileExtension = "zip"
        def query = params.query
        if (!query) {
            query = '*'
        }
        params.include = 'projectId'
        SearchResponse res = elasticSearchService.search(query, params, "homepage")

        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.sourceAsMap.projectId) {
                ids << hit.sourceAsMap.projectId
            }
        }

        Closure doDownload = { OutputStream outputStream, GrailsParameterMap paramMap ->
            SimpleDateFormat format = new SimpleDateFormat('yyyy-MM-dd')
            def name = 'meritSites-' + format.format(new Date())

            reportService.exportShapeFile(ids, name, outputStream)
        }
        downloadService.downloadProjectDataAsync(params, doDownload)
        response.status = 200
        render "OK"
    }

    /**
     * Check given species is in a senstive list.
     * @param name species common name or scientific name
     * @param lat latitude
     * @param lng latitude
     * @return generalised lat and lng value
     */
    def sensitiveSpecies(String name, double lat, double lng){
        if(name && lat && lng){
            def result = sensitiveSpeciesService.findSpecies(name, lat, lng)
            if(result) {
                render ([status:'ok', text:"sensitive species", result: result] as JSON)
            } else {
                render ([status:'ok', text:"not a sensitive species"] as JSON)
            }
        } else {
            response.setStatus(400)
            render ([status:'error', error:'Invalid query (expected: name, lat and lng)'] as JSON)
        }
    }

    /**
     * A test method to get the document mapping used by Elastic Search (or will be used by in the next re-index).
     * @return
     */
    def getMapping(){
        render(text: elasticSearchService.getMapping() as JSON, contentType: 'application/json')
    }
}