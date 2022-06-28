package au.org.ala.ecodata.mapper

import au.org.ala.ecodata.DocumentHostInterceptor
import au.org.ala.ecodata.DocumentUrlBuilder
import groovy.transform.CompileStatic
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange
import org.elasticsearch.search.aggregations.metrics.ParsedStats
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes

/**
 * Converts an ElasticSearch 7+ SearchResponse into a Map format equivalent to the ElasticSearch 1
 * response to maintain compatibility with MERIT and BioCollect.
 */
@CompileStatic
class ElasticSearchBackwardsCompatibilityMapper {
    static Map mapAggregation(Aggregation agg) {
        String type = agg.type
        Map result
        long total = 0
        if (agg.type.endsWith("terms")) {
            type = 'terms'
            MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) agg
            result = [(agg.name): [terms: multiBucketsAggregation.buckets.collect { bucket ->
                total += bucket.docCount
                [term: bucket.keyAsString, count: bucket.docCount] // keyAsString converts boolean 0/1 to false/true
            }, _type                    : type, total: total]]
        }
        else if (agg.type == 'range') {

            ParsedRange range = (ParsedRange) agg
            result = [(agg.name): [ranges: range.buckets.collect { bucket ->
                total += bucket.docCount
                Map bucketAsMap = [term: bucket.key, count: bucket.docCount]
                def from = bucket.from
                if (from && from != Double.NEGATIVE_INFINITY && from != Double.POSITIVE_INFINITY) {
                    bucketAsMap += [from: from, from_str: bucket.fromAsString]
                }
                def to = bucket.to
                if (to && to != Double.NEGATIVE_INFINITY && to != Double.POSITIVE_INFINITY) {
                    bucketAsMap += [to: to, to_str: bucket.toAsString]
                }
                bucketAsMap
            }, _type                     : type, total: total]]
        }
        else if (agg.type == 'histogram') {
            ParsedHistogram histogram = (ParsedHistogram) agg
            result = [(agg.name): [entries: histogram.buckets.collect { bucket ->
                total += bucket.docCount
                [key: bucket.key, count: bucket.docCount]
            }, _type                      : type, total: total]]
        }
        else if (agg.type == 'stats') {
            ParsedStats stats = (ParsedStats) agg
            result = [(agg.name): [_type: agg.type, min: stats.min, max: stats.max, avg: stats.avg, sum: stats.sum, count: stats.count]]
        }
        else {
            result = [(agg.name):[_type:agg.type, error:"Unknown type: "+agg.type]]
        }
        result
    }

    static List mapAggregations(Aggregations aggregations) {
        List results = []
        if (aggregations != null) {
            for (Aggregation agg : aggregations) {
                results.add(mapAggregation(agg))
            }
        }
        results
    }


    static Map mapHitSource(Map source) {
        Map result = source
        String hostName = GrailsWebRequest.lookup()?.getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST)

        if (hostName) {
            result = DocumentUrlBuilder.updateDocumentURL(source, hostName)
        }
        result
    }

}
