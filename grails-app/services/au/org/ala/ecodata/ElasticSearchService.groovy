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

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.get.MultiGetRequestBuilder
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.node.Node
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder

import javax.annotation.PostConstruct

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
    def DEFAULT_TYPE = "doc"
    def MAX_FACETS = 10;

    /**
     * Init method to be called on service creation
     */
    @PostConstruct
    def initialize() {
        // see http://www.elasticsearch.org/guide/clients/groovy-api/client/ for details for adding config
        log.info "Setting-up elasticsearch node and client"
        node = nodeBuilder().node();
        client = node.client();
    }

    /**
     * Create a new index with custom mappings
     *
     * TODO add code to check if index exists and only add/update mappings if required
     */
    def reInitialiseIndex() {
        log.debug "reInitialiseIndex"
        try {
//            CreateIndexResponse createResponse = client.admin().indices().prepareCreate(DEFAULT_INDEX).execute().actionGet();
//            if (createResponse.acknowledged) {
//                log.debug "created index ${DEFAULT_INDEX}"
//            } else {
//                log.debug "failed to create index ${DEFAULT_INDEX}"
//            }

//            if (client.admin().indices().prepareExists(DEFAULT_INDEX).execute().actionGet().exists()) {
//                // update index
//            } else {
//                // create index
//                client.admin().indices().prepareCreate(DEFAULT_INDEX).addMapping(DEFAULT_TYPE, mapping).setSettings(settings).execute().actionGet();
//            }

            addMappings()
        } catch (Exception e) {
            log.error "Error creating index: ${e}"
        }
    }

    /**
     * Index a single document (toMap representation not domain class)
     * Does a check to see if doc has been marked as deleted.
     *
     * @param doc
     * @return IndexResponse
     */
    def indexDoc(doc) {
        def docId = getEntityId(doc)

        if (checkForDelete(doc, docId)) {
            return null
        }

        addCustomFields(doc)

        client.prepareIndex(DEFAULT_INDEX, DEFAULT_TYPE, docId)
            .setSource(
                doc as HashMap<String, Object>
            ).execute().actionGet();
    }

    /**
     * Get the doc identifier, which differs for each domain class
     *
     * @param doc
     * @return docId (String)
     */
    def getEntityId(doc) {
        def docId
        switch ( doc.class ) {
            case "au.org.ala.ecodata.Project":
                docId = doc.projectId; break
            case "au.org.ala.ecodata.Site":
                docId = doc.siteId; break
            case "au.org.ala.ecodata.Activity":
                docId = doc.activityId; break
            default:
                docId = doc.id; break
        }
        docId
    }

    /**
     * Check if a doc has been marked as deleted.
     * Returns false if the doc to be indexed exists in the search index
     * and has {status: "deleted"}. Doc is deleted from search index.
     *
     * @param doc
     * @param docId
     * @return isDeleted (Boolean)
     */
    def checkForDelete(doc, docId) {
        def isDeleted = false
        def indexIsReady = false
        def maxTries = 60
        def tries = 0

        while (!indexIsReady) {
            //log.debug "$docId - indexIsReady = ${indexIsReady}"
            tries++
            if (client.admin().indices().prepareExists(DEFAULT_INDEX).execute().actionGet().isExists()) {
                indexIsReady = true
            } else if (tries >= maxTries) {
                break
            } else {
                sleep(1000)
            }
        }

        def resp = client.prepareGet(DEFAULT_INDEX, DEFAULT_TYPE, docId)
                .execute()
                .actionGet();

        if (resp && doc.status == "deleted") {
            try {
                deleteDocById(docId)
                isDeleted = true
            } catch (Exception e) {
                log.error "Error deleting doc with ID ${docId}: ${e.message}"
            }
        }

        return isDeleted
    }

    /**
     * Add extra (custom) fields to doc in search index.
     *
     * @param doc
     */
    def addCustomFields(doc) {
        // hand-coded copy fields with different analysers
        doc.organisationFacet = doc.organisationName
        doc.typeFacet = doc.type

        if (doc.extent?.geometry) {
            doc.stateFacet = doc.extent.geometry.state
            doc.lgaFacet = doc.extent.geometry.lga
            doc.nrmFacet = doc.extent.geometry.nrm
        }

        if (!doc.name && doc.type) {
            // activities have no name so we'll use the type
            doc.name = doc.type
        }

        doc.nameSort = doc.name

        if (doc.extent?.geometry?.decimalLatitude && doc.extent?.geometry?.decimalLatitude) {
            String lat = doc.extent.geometry.decimalLatitude as String
            String lon = doc.extent.geometry.decimalLongitude as String
            doc.geo = [:]
            doc.geo.lat = lat.toFloat()
            doc.geo.lon = lon.toFloat()
        } else if (doc.location?.data?.decimalLatitude && doc.location?.data?.decimalLongitude) {
            //log.debug "data = ${doc.location.data}"
            def lat = doc.location.data.decimalLatitude.getAt(0) as String
            def lon = doc.location.data.decimalLongitude.getAt(0) as String
            //log.debug "lat = ${lat} || type = ${lat.getClass().name}"
            doc.geo = [:]
            doc.geo.lat = lat.toFloat()
            doc.geo.lon = lon.toFloat()
        }
    }

    /**
     * Add custom mapping for ES index.
     */
    def addMappings() {

        def mappingJson = '''
            {
                "doc": {
                    "_all": {
                        "enabled": true,
                        "store": "yes"
                    },
                    "properties": {
                        "organisationFacet" : {
                            "type":"string",
                            "index":"not_analyzed"
                        },
                        "typeFacet": {
                            "type":"string",
                            "index":"not_analyzed"
                        },
                        "class": {
                            "type":"string",
                            "index":"not_analyzed"
                        },
                        "stateFacet": {
                            "type":"string",
                            "index":"not_analyzed"
                        },
                        "lgaFacet": {
                            "type":"string",
                            "index":"not_analyzed"
                        },
                        "nrmFacet": {
                            "type":"string",
                            "index":"not_analyzed"
                        },
                        "nameSort": {
                            "type":"string",
                            "index":"not_analyzed"
                        }
                    }
                }
            }
        '''
        client.admin().indices().prepareCreate(DEFAULT_INDEX).addMapping(DEFAULT_TYPE, mappingJson).execute().actionGet()
    }

    /**
     * Index any document type using the toMap representation of it
     *
     * @param doc
     */
    def indexDocType(doc) {
        log.debug "doc has class: ${doc.getClass().name}"
        def docClass = doc.getClass()
        switch(docClass) {
            case au.org.ala.ecodata.Project:
                def projectMap = projectService.toMap(doc, "flat")
                projectMap["class"] = docClass.name
                indexDoc(projectMap)
                break;
            case au.org.ala.ecodata.Site:
                def siteMap = siteService.toMap(doc, "flat")
                siteMap["class"] = docClass.name
                indexDoc(siteMap)
                break;
            case au.org.ala.ecodata.Activity:
                def activityMap = activityService.toMap(doc, "flat")
                activityMap["class"] = docClass.name
                indexDoc(activityMap)
                break;
        }
    }

    /**
     * Index all documents. Index is cleared first.
     */
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
            def siteMap = siteService.toMap(it, "flat")
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

    /**
     * Search with a query string
     *
     * @param query
     * @param params
     * @return IndexResponse
     */
    def search(String query, GrailsParameterMap params) {
        log.debug "search params: ${params}"

//        SearchRequestBuilder builder = client
//                .prepareSearch(DEFAULT_INDEX)
//                .setTypes(DEFAULT_TYPE)
//                .setQuery(queryString(query))
//                .setFrom(0)
//                .setSize(10)
//                .addHighlightedField("description")
//        SearchResponse sr = builder.execute().actionGet();

        def request = buildSearchRequest(query, params)
        client.search(request).actionGet()
    }

    /**
     * Full text search with just a query (String)
     *
     * @deprecated
     * @param request
     * @return IndexResponse
     */
    def doSearch(SearchRequest request) {
        def response = client.search(request).actionGet()
        //def searchHits = response.hits()
        //def result = [:]
        //result.total = searchHits.totalHits()
        //log.debug "Search returned ${result.total ?: 0} result(s)."
        return response
    }

    /**
     * Build the search request object from query and params
     *
     * @param query
     * @param params
     * @return SearchRequest
     */
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

        // add query
        source.query(queryString(query))

        // add facets
        addFacets(params.facets, params.fq, params.maxFacets ? params.maxFacets as int : MAX_FACETS).each {
            source.facet(it)
        }

        // handle facet filter
        if (params.fq) {
            log.debug "fq detected: ${params.fq}"
            source.filter(buildFilters(params.fq))
        }

        if (params.highlight) {
            source.highlight(new HighlightBuilder().preTags("<b>").postTags("</b>").field("_all", 60, 2))
        }

        request.source(source)

        return request
    }

    /**
     * Generate list of facets for search request
     *
     * @param facets
     * @param filters
     * @return facetList
     */
    def addFacets(facets, filters, maxFacets = MAX_FACETS) {
        // use FacetBuilders
        // e.g. FacetBuilders.termsFacet("f1").field("field")
        log.debug "filters = $filters; maxFacets = ${maxFacets}"

        def facetList = []
        def filterList = getFilterList(filters)

        if (facets) {
            facets.split(",")each {
                facetList.add(FacetBuilders.termsFacet(it).field(it).size(maxFacets).facetFilter(addFacetFilter(filterList)))
            }
        } else {
            facetList.add(FacetBuilders.termsFacet("typeFacet").field("typeFacet").size(maxFacets).facetFilter(addFacetFilter(filterList)))
            facetList.add(FacetBuilders.termsFacet("assessment").field("assessment").size(maxFacets).facetFilter(addFacetFilter(filterList)))
            facetList.add(FacetBuilders.termsFacet("class").field("class").size(maxFacets).facetFilter(addFacetFilter(filterList)))
            facetList.add(FacetBuilders.termsFacet("organisationFacet").field("organisationFacet").size(maxFacets).facetFilter(addFacetFilter(filterList)))
            facetList.add(FacetBuilders.termsFacet("stateFacet").field("stateFacet").size(maxFacets).facetFilter(addFacetFilter(filterList)))
            facetList.add(FacetBuilders.termsFacet("lgaFacet").field("lgaFacet").size(maxFacets).facetFilter(addFacetFilter(filterList)))
            facetList.add(FacetBuilders.termsFacet("nrmFacet").field("nrmFacet").size(maxFacets).facetFilter(addFacetFilter(filterList)))
        }

        return facetList
    }

    /**
     * Generate FilterBuilders from the fq request params
     *
     * @param filterList
     * @return FilterBuilders
     */
    def addFacetFilter(filterList) {
        def fb

        filterList.find {
            if (it) {
                def fqs = it.tokenize(":")
                QueryBuilder qb = QueryBuilders.matchQuery(fqs[0], fqs[1]);
                fb =  FilterBuilders.queryFilter(qb)
            }
        }

        fb
    }

    /**
     * Build up the fq filter (builders)
     *
     * @param filters
     * @return
     */
    def buildFilters(filters) {
        // see http://www.elasticsearch.org/guide/reference/java-api/query-dsl-filters/
        //log.debug "filters (fq) = ${filters} - type: ${filters.getClass().name}"

        List filterList = getFilterList(filters) // allow for multiple fq params

        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
        filterList.each { fq ->
            def fqs = fq.tokenize(":")
            // support SOLR style filters (-) for exclude
            if (fqs[0].getAt(0) == "-") {
                boolFilter.mustNot(FilterBuilders.prefixFilter(fqs[0][1..-1], fqs[1]))
            } else {
                boolFilter.must(FilterBuilders.prefixFilter(fqs[0], fqs[1]))
            }
        }

        FilterBuilders.boolFilter().should(boolFilter)
    }

    /**
     * Helper method to return a List give either a String or String[]
     *
     * @param filters
     * @return filterList
     */
    private getFilterList(filters) {
        def filterList = []

        if (filters instanceof String[]) {
            // assume a String[] array
            filterList = filters as List
        } else {
            filterList.add(filters)
        }

        filterList
    }

    /**
     * Delete a doc given the toMap version of it
     *
     * @param obj
     * @return
     */
    def deleteDoc(obj) {
        // see http://www.elasticsearch.org/guide/reference/java-api/delete/
        def id = obj[getEntityId(obj)]
        log.debug "deleting doc with id: ${id}"
        deleteDocById(id)
    }

    /**
     * Delete a doc given its ID
     *
     * @param id
     * @return
     */
    def deleteDocById(id) {
        client.prepareDelete(DEFAULT_INDEX, DEFAULT_TYPE, id)
                .execute()
                .actionGet();
    }

    /**
     * Delete the (default) ES index
     *
     * @return
     */
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

        // recreate the index and mappings
        reInitialiseIndex()
        return "index cleared"
    }

    /**
     * Shutdown ES server (called by Bootstrap.groovy)
     */
    def destroy() {
        node.close();
    }

}
