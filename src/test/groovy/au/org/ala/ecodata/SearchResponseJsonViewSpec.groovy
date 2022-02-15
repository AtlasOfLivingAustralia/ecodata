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
        SearchHits hits = GroovyMock(SearchHits)
        SearchHit hit = GroovyMock(SearchHit)
        SearchHit[] searchHits = [hit] as SearchHit[]
        hit.getId() >> "1"
        hit.docId() >> 1
        hit.getIndex() >> "homepage"

        hits.getTotalHits() >> new TotalHits(10, TotalHits.Relation.EQUAL_TO)
        hits.getHits() >> searchHits
        searchResponse.getHits() >> hits

        def result = render(template: "/search/searchResponse", model:[searchResponse:searchResponse])

        then:"The json is correct"
        println result.json
        result.json.hits == [total:10, hits:[[_id:"1", _source:null, highlightFields:null]]]
    }
}
