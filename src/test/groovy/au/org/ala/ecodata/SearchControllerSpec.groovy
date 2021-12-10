package au.org.ala.ecodata

import au.org.ala.ecodata.command.UserSummaryReportCommand
import au.org.ala.ecodata.reporting.ProjectExporter
import au.org.ala.ecodata.reporting.ProjectXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.apache.lucene.search.TotalHits
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import spock.lang.Specification

class SearchControllerSpec extends Specification implements ControllerUnitTest<SearchController> {

    DownloadService downloadService = Mock(DownloadService)
    UserService userService = Mock(UserService)
    ElasticSearchService elasticSearchService = Mock(ElasticSearchService)
    MetadataService metadataService = Mock(MetadataService)
    ReportingService reportingService = Mock(ReportingService)
    ActivityFormService activityFormService = Mock(ActivityFormService)
    ReportService reportService = Mock(ReportService)

    void setup() {
        controller.downloadService = downloadService
        controller.userService = userService
        controller.elasticSearchService = elasticSearchService
        controller.reportService = reportService

        defineBeans {
            metadataService(MetadataService)
            userService(UserService)
            reportingService(ReportingService)
            activityFormService(ActivityFormService)
        }
    }

    def "The MERIT download uses the streaming xls exporter"() {

        setup:
        params.test = 'far'
        params.isMERIT = true

        SearchResponse mockResponse = Mock(SearchResponse)
        Aggregations aggregations = Mock(Aggregations)
        mockResponse.getAggregations() >> aggregations

        when:
        params.email = 'test@test.org'
        controller.downloadAllData()

        then:
        1 * downloadService.getProjectIdsForDownload(_, ElasticIndex.HOMEPAGE_INDEX)
        1 * downloadService.downloadProjectDataAsync(_, {it instanceof Closure})

    }

    def "The MERIT project exporter accepts parameters"(boolean formSectionPerTab) {
        setup:
        XlsExporter exporter = Mock(XlsExporter)
        params.formSectionPerTab = formSectionPerTab
        params.tabs = ['tab1', 'tab2']
        SearchResponse searchResponse = GroovyMock(SearchResponse)

        when:
        controller.elasticSearchService = elasticSearchService
        ProjectExporter projectExporter = controller.meritProjectExporter(exporter, params)

        then:
        1 * elasticSearchService.search(_, params, _) >> searchResponse

        projectExporter instanceof ProjectXlsExporter
        ((ProjectXlsExporter)projectExporter).formSectionPerTab == formSectionPerTab
        projectExporter.tabsToExport == params.tabs

        where:
        formSectionPerTab | _
                true  | _
                false | _

    }

    def "The elasticGeo method returns project site information from an elasticsearch query"() {
        setup:
        SearchResponse searchResponse = GroovyMock(SearchResponse)
        List projectDocs = ['p1', 'p2', 'p3'].collect {
            [projectId:it, name:'project - '+it, organisationName:'org - '+it, geo:[[:]], sites:[[extent:[geometry:[:]]]]]
        }

        SearchHit[] hits = projectDocs.collect{
            SearchHit searchHit = GroovyMock(SearchHit)
            searchHit.getSourceAsMap() >> it
            searchHit
        }

        TotalHits totalHits = new TotalHits(3, TotalHits.Relation.EQUAL_TO)
        searchResponse.getHits() >> new SearchHits(hits, totalHits, 1.0)

        List expectedResults = projectDocs.collect {
            [projectId:it.projectId, name:it.name, org: it.organisationName, geo:[[geometry:it.sites[0].extent.geometry]]]
        }

        when:
        params.query = "*:*"
        controller.elasticGeo()

        then:
        1 * elasticSearchService.search( "*:*", params, ElasticIndex.HOMEPAGE_INDEX, null) >> searchResponse
        response.json == [
                total:totalHits.value,
                projects:expectedResults,
                selectedFacetTerms:[]]
    }

    def "The elasticGeo method triggers post processing of search results when markBy is supplied"() {
        setup:
        SearchResponse searchResponse = GroovyMock(SearchResponse)
        List projectDocs = ['p1', 'p2', 'p3'].collect {
            [projectId:it, name:'project - '+it, organisationName:'org - '+it, associatedProgram: 'program - '+it, geo:[[:]], sites:[[extent:[geometry:[:]]]]]
        }
        String markBy = 'associatedProgramFacet'
        SearchHit[] hits = projectDocs.collect{
            SearchHit searchHit = GroovyMock(SearchHit)
            searchHit.getSourceAsMap() >> it
            searchHit
        }

        ParsedStringTerms terms = new ParsedStringTerms()
        terms.setName(markBy)

        List buckets = projectDocs.collect {
            Terms.Bucket bucket = GroovyMock(Terms.Bucket)
            bucket.getKey() >> it.associatedProgram
            bucket.getDocCount() >> 1
            bucket
        }

        terms.getBuckets().addAll(buckets)
        Aggregations aggregations = new Aggregations([terms])
        
        TotalHits totalHits = new TotalHits(3, TotalHits.Relation.EQUAL_TO)
        searchResponse.getHits() >> new SearchHits(hits, totalHits, 1.0)
        searchResponse.getAggregations() >> aggregations

        int i=0
        List expectedResults = projectDocs.collect {
            [projectId:it.projectId, name:it.name, org: it.organisationName, geo:[[legendName:it.associatedProgram, index:i++]]]
        }
        i=0
        List expectedFacetTerms = projectDocs.collect {
            [legendName:it.associatedProgram, index: i++, count: 2] // I feel this should be 1 but this is what the existing code does.
        }

        when:
        params.query = "*:*"
        params.markBy = markBy
        controller.elasticGeo()

        then:
        1 * elasticSearchService.search("*:*", params, ElasticIndex.HOMEPAGE_INDEX, null) >> searchResponse
        response.json == [
            total:totalHits.value,
            projects:expectedResults,
            selectedFacetTerms:expectedFacetTerms]
    }

    def "The geoPost method presents search results in a backwards compatible way"() {

        setup:
        SearchResponse searchResponse = GroovyMock(SearchResponse)

        when:
        request.json = [query:'*:*']
        controller.elasticPost()

        then:
        1 * elasticSearchService.search('*:*', _, ElasticIndex.DEFAULT_INDEX) >> searchResponse
        model == [searchResponse:searchResponse]
        // This previously was 'elasticPost' and is now '/search/elasticPost.gsp, possibly to do with the
        // grails json view plugin behaviour depending on how the test is executed?
        view.contains('elasticPost')

    }

    def "The download user list action delegates to the reportService to build the report"() {
        when:
        UserSummaryReportCommand command = new UserSummaryReportCommand(hubId:"merit")
        controller.downloadUserList(command)

        then:
        1 * userService.getCurrentUserDisplayName() >> "Test"
        1 * downloadService.downloadProjectDataAsync(params, _)
        response.status == HttpStatus.SC_OK
    }

    def "The download user action will return an error if the params fail validation"() {
        when:
        UserSummaryReportCommand command = new UserSummaryReportCommand(email:"test")
        command.validate()
        controller.downloadUserList(command)

        then:
        0 * userService.getCurrentUserDisplayName()
        0 * downloadService.downloadProjectDataAsync(_, _)
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
    }

}
