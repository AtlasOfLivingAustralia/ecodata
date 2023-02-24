package au.org.ala.ecodata.mapper

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import spock.lang.Specification

/**
 * This test is quite fragile as the elasticsearch API is mocked, so an upgrade of the API may be happily ignored
 * by this test.
 * There are functional tests in MERIT that use real data and searches that will catch issues.
 */
class ElasticSearchBackwardsCompatibilityMapperSpec extends Specification {

    void "The hit source can be mapped"() {
        setup:
        Map source = [projectId:'p1']

        when:
        Map result = ElasticSearchBackwardsCompatibilityMapper.mapHitSource(source)

        then:
        result == source
    }

    private StringTerms.Bucket mockBucket(String term, long count) {
        StringTerms.Bucket bucket = Mock(StringTerms.Bucket)
        bucket.getKeyAsString() >> term
        bucket.getDocCount() >> count
        bucket
    }

    private StringTerms mockStringTerms(Map<String, Long> terms) {
        List buckets = []
        terms.each {String term, long count ->
            buckets << mockBucket(term, count)
        }

        StringTerms agg = Mock(StringTerms)
        agg.getName() >> "facet1"
        agg.getType() >> "sterms"
        agg.getBuckets() >> buckets

        agg
    }

    void "A terms aggregation can be mapped"() {
        setup:
        StringTerms agg = mockStringTerms([key1:2, key2:3])

        when:
        Map result = ElasticSearchBackwardsCompatibilityMapper.mapAggregation(agg)

        then:
        result == [facet1:[terms:[[term:"key1", count:2], [term:"key2", count:3]], _type:"terms", total:5]]

    }

    void "Aggregations can be mapped"() {
        setup:
        List aggs = []
        aggs << mockStringTerms([key1:2, key2:3])
        Aggregations aggregations = new Aggregations(aggs)

        when:
        Map result = ElasticSearchBackwardsCompatibilityMapper.mapAggregations(aggregations)

        then:
        result == [facet1:[terms:[[term:"key1", count:2], [term:"key2", count:3]], _type:"terms", total:5]]
    }

}
