/*
 * Copyright (C) 2013 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.ecodata

import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.groovy.client.GClient
import org.elasticsearch.groovy.common.xcontent.GXContentBuilder
import org.elasticsearch.groovy.node.GNode
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder

import static org.elasticsearch.index.query.QueryBuilders.queryString
import static org.elasticsearch.groovy.node.GNodeBuilder.*

/**
 * ElasticSearch service
 * Note: initialize and destroy methods are called by Bootstrap.groovy
 * Code gist taken from
 *   https://github.com/mstein/elasticsearch-grails-plugin/blob/master/grails-app/services/org/grails/plugins/elasticsearch/ElasticSearchService.groovy
 *
 * @author "Nick dos Remedios <nick.dosremedios@csiro.au>"
 */
class ElasticSearchService {
    static transactional = false

    def projectService
    def siteService
    def activityService

    GNode node;
    GClient client;
    def DEFAULT_INDEX = "all"

    def initialize() {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/client/ for details for adding config
        log.info "Setting-up elasticsearch node and client"
        node = nodeBuilder().node();
        client = node.getClient();
    }

    def indexDoc(doc) {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/index_/
        def docJson = doc as JSON
        log.debug "Indexing doc: ${docJson}"

        def indexR = client.index {
            index DEFAULT_INDEX
            type doc["class"]?:"doc"
            id doc["id"]
            source {
                doc
            }
        }
        log.debug "indexR response: ${indexR.actionGet()}"
    }

    def indexAll() {
        log.debug "Clearing index first"
        deleteIndex()
        log.debug "Indexing all projects"
        def list = projectService.list("flat", false)
        list.each {
            it["class"] = new Project().getClass().name
            indexDoc(it)
        }
        log.debug "Indexing all sites"
        def sites = Site.findAll()
        sites.each {
            def siteMap = siteService.toMap(it, "brief")
            siteMap["class"] = new Site().getClass().name
            indexDoc(siteMap)
        }
        log.debug "Indexing all activities"
        def acts = activityService.getAll(false, "flat")
        acts.each {
            it["class"] = new Activity().getClass().name
            indexDoc(it)
        }
    }

    def search(String query, GrailsParameterMap params) {
        def req = buildSearchRequest(query, params)
        doSearch(req)
        //doSearch(query, params)
    }

    def doSearch(SearchRequest request) {
        def response = client.search(request).actionGet()
        //def searchHits = response.hits()
        //def result = [:]
        //result.total = searchHits.totalHits()
        //log.debug "Search returned ${result.total ?: 0} result(s)."
        return response
    }

    def buildSearchRequest(query, GrailsParameterMap params) {
        SearchRequest request = new SearchRequest()
        request.searchType SearchType.DFS_QUERY_THEN_FETCH

        // set indices and types
        request.indices(DEFAULT_INDEX)
        def types = []
        if (params.types && params.types instanceof Collection<String>) {
            types = params.types
        }
        request.types(types as String[])

        // set pagination stuff
        SearchSourceBuilder source = new SearchSourceBuilder()
        source.from(params.offset ? params.offset as int : 0)
        source.size(params.max ? params.max as int : 10)
        source.explain(params.explain ?: false)
        if (params.sort) {
            source.sort(params.sort, SortOrder.valueOf(params.order?.toUpperCase() ?: "ASC"))
        }

        // Handle the query, can either be a closure or a string
        if (query instanceof Closure) {
            source.query(new GXContentBuilder().buildAsBytes(query))
        } else {
            source.query(queryString(query))
        }

        // add facets
        addFacets(params.facets).each {
            source.facet(it)
        }

        // handle facet filter
        if (params.fq) {
            source.filter(buildFilters(params.fq))
        }

        // Handle highlighting
        if (params.highlight) {
            def highlighter = new HighlightBuilder()
            // params.highlight is expected to provide a Closure.
            def highlightBuilder = params.highlight
            highlightBuilder.delegate = highlighter
            highlightBuilder.resolveStrategy = Closure.DELEGATE_FIRST
            highlightBuilder.call()
            source.highlight highlighter
        }

        request.source(source)

        return request
    }

    def addFacets(facets) {
        // use FacetBuilders
        // e.g. FacetBuilders.termsFacet("f1").field("field")
        def facetList = []
        if (facets) {
            facets.each {
                facetList.add(FacetBuilders.termsFacet(it).field(it).size(5))
            }
        } else {
            facetList.add(FacetBuilders.termsFacet("status").field("status").size(5))
            facetList.add(FacetBuilders.termsFacet("type").field("type").size(5))
        }

        return facetList
    }

    def buildFilters(filters) {
        // see http://www.elasticsearch.org/guide/reference/java-api/query-dsl-filters/
        log.debug "filters (fq) = ${filters} - type: ${filters.getClass().name}"

        def filterList = []

        if (filters instanceof java.lang.String) {
            filterList.add(filters)
        } else {
            // assume a String[] array
            filterList = filters as List
        }

        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
        filterList.each { fq ->
            def fqs = fq.tokenize(":")
            log.debug "each filters: $fq - $fqs"
            // support SOLR style filters (-) for exclude
            if (fqs[0].getAt(0) == "-") {
                boolFilter.mustNot(FilterBuilders.termFilter(fqs[0][1..-1], fqs[1]))
            } else {
                boolFilter.must(FilterBuilders.termFilter(fqs[0], fqs[1]))
            }
        }

        FilterBuilders.boolFilter().should(boolFilter)
    }

    /**
     *
     * @deprecated - groovy way but not able to handle complex searches
     * @param queryString
     * @param params
     * @return
     */
    def doSearch (queryString, params) {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/search/
        def fqs = []
        params.each { key, val ->
            if (key == "fq") {
                def terms = val.tokenize(":")
                def map = [:]
                map[terms[0]] = terms[1]
                fqs.add(terms: map)
            }
        }
        log.debug "Search - queryString = ${queryString}"
        log.debug "Search - fqs = ${fqs}"
        log.debug "Search - params = ${params}"
        node.client.search {
            indices DEFAULT_INDEX
            types []
            source {
                from = 0
                size = 10
                query {
                    query_string(
                            //fields: [field],
                            query: queryString)
                }
                facets {
                    status {
                        terms (
                            field: "status"
                        )
                    }
                    docType {
                        terms (
                                field: "type"
                        )
                    }
                }
                filters {
                    term (status:"active")
                }
            }
        }
    }

    def deleteDoc(obj) {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/delete.html

    }

    public deleteIndex() {
        try {
            def response = node.client.admin.indices.prepareDelete(DEFAULT_INDEX).execute().get()
            if (response.acknowledged) {
                log.info "The index is removed"
            } else {
                log.error "The index could not be removed"
            }
        } catch (Exception e) {
            log.error "The index you want to delete is missing : ${e.message}"
        }
        return "index removed"
    }

    def destroy() {
        node.close();
    }

}
