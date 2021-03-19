package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.ProjectExporter
import au.org.ala.ecodata.reporting.ProjectXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.testing.web.controllers.ControllerUnitTest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.facet.Facets
import spock.lang.Specification

class SearchControllerSpec extends Specification implements ControllerUnitTest<SearchController> {

    DownloadService downloadService = Mock(DownloadService)
    UserService userService = Mock(UserService)
    ElasticSearchService elasticSearchService = Mock(ElasticSearchService)
    MetadataService metadataService = Mock(MetadataService)
    ReportingService reportingService = Mock(ReportingService)
    ActivityFormService activityFormService = Mock(ActivityFormService)

    void setup() {
        controller.downloadService = downloadService
        controller.userService = userService
        controller.elasticSearchService = elasticSearchService

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
        Facets facets = Mock(Facets)
        mockResponse.getFacets() >> facets

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
        1 * searchResponse.getFacets() >> Mock(Facets)
        projectExporter instanceof ProjectXlsExporter
        ((ProjectXlsExporter)projectExporter).formSectionPerTab == formSectionPerTab
        projectExporter.tabsToExport == params.tabs

        where:
        formSectionPerTab | _
                true  | _
                false | _

    }
}
