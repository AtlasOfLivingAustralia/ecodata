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
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.node.Node
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder

import static org.elasticsearch.index.query.QueryBuilders.queryString
import static org.elasticsearch.node.NodeBuilder.*

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

    Node node;
    Client client;
    def DEFAULT_INDEX = "all"
    def MAX_FACETS = 10;

    def initialize() {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/client/ for details for adding config
        log.info "Setting-up elasticsearch node and client"
        node = nodeBuilder().node();
        client = node.client();
    }

    def reInitialiseIndex() {
        log.debug "reInitialiseIndex"
        try {
//            CreateIndexResponse createResponse = client.admin().indices().prepareCreate(DEFAULT_INDEX).execute().actionGet();
//            if (createResponse.acknowledged) {
//                log.debug "created index ${DEFAULT_INDEX}"
//            } else {
//                log.debug "failed to create index ${DEFAULT_INDEX}"
//            }
            addMappings()
        } catch (Exception e) {
            log.error "Error creating index: ${e}"
        }
    }

    def indexDoc(doc) {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/index_/
        def docJson = doc as JSON
        log.debug "Indexing doc: ${docJson}"

//        def indexR = client.index {
//            index DEFAULT_INDEX
//            type doc["class"]?:"doc"
//            id doc["id"]
//            source {
//                doc
//            }
//        }
//        log.debug "indexR response: ${indexR.actionGet()}"

        def className = doc.class?:"doc"
        def docId
        switch ( className ) {
            case "au.org.ala.ecodata.Project":
                docId = doc.projectId; break
            case "au.org.ala.ecodata.Site":
                docId = doc.siteId; break
            case "au.org.ala.ecodata.Activity":
                docId = doc.activityId; break
            default:
                docId = doc.id; break
        }
        log.debug "class = ${className} & id = ${docId}"

        addCustomFields(doc)

        IndexResponse response = client.prepareIndex(DEFAULT_INDEX, className, docId)
            .setSource(
                doc as HashMap<String, Object>
            )
            .execute()
            .actionGet();
        log.debug "index response = " + response.toString()
    }

    def addCustomFields(doc) {
        // TODO: remove nasty hack for underscores = spaces
        doc.organisationFacet = doc.organisationName?.replace(" ", "_")
        doc.typeFacet = doc.type?.replace(" ", "_")
    }

    def addMappings() {
        def analysisJson = '''
            {
                "analysis": {
                    "analyzer": {
                        "organisationFacet" : {
                            "type" : "string",
                            "store" : "yes",
                            "index" : "not_analyzed"
                        }
                    }
                }
            }
        '''

        client.admin().indices().prepareCreate(DEFAULT_INDEX)
            .setSettings(
                ImmutableSettings.settingsBuilder().loadFromSource( analysisJson )
            ).execute().actionGet()
    }

    def createFieldMappings(indexType) {
        def mappingsSrc = '''{
            "analysis": {
                "analyzer": {
                    "organisation" : {
                        "type" : "custom",
                        "tokenizer" : "standard",
                        "filter" : ["snowball", "standard", "lowercase"]
                    }
                }
            }
        }'''

//        client.admin().indices()
//                .preparePutMapping(DEFAULT_INDEX)
//                .setType([])
//                .setSource(mappingsSrc)
//                .execute().actionGet()
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
        //if (query instanceof Closure) {
            //source.query(new GXContentBuilder().buildAsBytes(query))
        //} else {
            source.query(queryString(query))
        //}

        // add facets
        addFacets(params.facets, params.fq).each {
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

    def addFacets(facets, filters) {
        // use FacetBuilders
        // e.g. FacetBuilders.termsFacet("f1").field("field")
        log.debug "filters = $filters"

        def facetList = []
        def filterList = getFilterList(filters)

        if (facets) {
            facets.each {
                facetList.add(FacetBuilders.termsFacet(it).field(it).size(5))
            }
        } else {
            //facetList.add(FacetBuilders.termsFacet("status").field("status").size(MAX_FACETS))
            facetList.add(FacetBuilders.termsFacet("type").field("typeFacet").size(MAX_FACETS).facetFilter(addFacetFilter("typeFacet", filterList)))
            facetList.add(FacetBuilders.termsFacet("class").field("class").size(MAX_FACETS).facetFilter(addFacetFilter("class", filterList)))
            facetList.add(FacetBuilders.termsFacet("organisation").field("organisationFacet").size(MAX_FACETS).facetFilter(addFacetFilter("organisationFacet", filterList)))
        }

        return facetList
    }

    def addFacetFilter(facet, filterList) {
        def fb
        filterList.find {
            def fqs = it.tokenize(":")
            if (true && facet == fqs[0]) {
                fb =  FilterBuilders.termFilter(facet, fqs[1])
            }
        }

        fb
    }

    def buildFilters(filters) {
        // see http://www.elasticsearch.org/guide/reference/java-api/query-dsl-filters/
        log.debug "filters (fq) = ${filters} - type: ${filters.getClass().name}"

        List filterList = getFilterList(filters)

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

    private getFilterList(String filters) {
        def filterList = []

        if (filters instanceof String) {
            filterList.add(filters)
        } else {
            // assume a String[] array
            filterList = filters as List
        }
        filterList
    }

    def deleteDoc(obj) {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/delete.html

    }

    public deleteIndex() {
        try {
            def response = node.client().admin().indices().prepareDelete(DEFAULT_INDEX).execute().get()
            if (response.acknowledged) {
                log.info "The index is removed"
            } else {
                log.error "The index could not be removed"
            }
        } catch (Exception e) {
            log.error "The index you want to delete is missing : ${e.message}"
        }

        reInitialiseIndex()
        return "index removed"
    }

    def destroy() {
        node.close();
    }

}
