package au.org.ala.ecodata

import grails.testing.web.controllers.ControllerUnitTest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.aggregations.Aggregations
import spock.lang.Specification

class SearchControllerSpec extends Specification implements ControllerUnitTest<SearchController> {

    DownloadService downloadService = Mock(DownloadService)
    UserService userService = Mock(UserService)
    ElasticSearchService elasticSearchService = Mock(ElasticSearchService)

    void setup() {
        controller.downloadService = downloadService
        controller.userService = userService
        controller.elasticSearchService = elasticSearchService
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
}
