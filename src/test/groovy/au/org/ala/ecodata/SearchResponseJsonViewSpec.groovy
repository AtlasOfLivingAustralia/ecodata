package au.org.ala.ecodata

import grails.plugin.json.view.test.JsonViewTest
import grails.util.Holders
import org.apache.lucene.search.TotalHits
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class SearchResponseJsonViewSpec extends Specification implements JsonViewTest, GrailsUnitTest {

    def "There is a custom JSON view for the search response to preserve backwards compatibility"() {
        when:"A gson view is rendered"
        Holders.grailsApplication = grailsApplication
        SearchResponse searchResponse = Mock(SearchResponse)
        SearchHit hit = new SearchHit(1, "1", null, null, null)
        SearchHit[] searchHits = [hit] as SearchHit[]
        SearchHits hits = new SearchHits(searchHits, new TotalHits(10, TotalHits.Relation.EQUAL_TO), 1.0f)
        searchResponse.getHits() >> hits

        def result = render(template: "/search/searchResponse", model:[searchResponse:searchResponse])

        then:"The json is correct"
        result.json.hits.total == 10
        result.json.hits.hits.size() == 1
        result.json.hits.hits[0]._id == "1"
        result.json.hits.hits[0]._source == null
        result.json.hits.hits[0].highlightFields == [:]
    }
}
