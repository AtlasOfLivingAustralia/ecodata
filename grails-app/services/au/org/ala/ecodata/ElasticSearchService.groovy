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
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.FilteredQueryBuilder
import org.elasticsearch.index.query.MatchAllQueryBuilder
import org.elasticsearch.node.Node
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.terms.TermsFacet
import org.elasticsearch.search.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.EventType

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.concurrent.ConcurrentLinkedQueue

import static org.elasticsearch.index.query.QueryBuilders.queryString
import static org.elasticsearch.node.NodeBuilder.nodeBuilder
/**
 * ElasticSearch service. This service is responsible for indexing documents as well as handling searches (queries).
 *
 * Code gist taken from
 *   https://github.com/mstein/elasticsearch-grails-plugin/blob/master/grails-app/services/org/grails/plugins/elasticsearch/ElasticSearchService.groovy
 *
 * @author "Nick dos Remedios <nick.dosremedios@csiro.au>"
 */
class ElasticSearchService {
    static transactional = false
    def grailsApplication, projectService, siteService, activityService, metadataService

    Node node;
    Client client;
    def indexingTempInactive = false // can be set to true for loading of dump files, etc
    def ALLOWED_DOC_TYPES = ['au.org.ala.ecodata.Project','au.org.ala.ecodata.Site','au.org.ala.ecodata.Activity']
    def DEFAULT_INDEX = "search"
    def HOMEPAGE_INDEX = "homepage"
    def DEFAULT_TYPE = "doc"
    def MAX_FACETS = 10
    private static Queue<IndexDocMsg> _messageQueue = new ConcurrentLinkedQueue<IndexDocMsg>()
    private static List<Class> EXCLUDED_OBJECT_TYPES = [ AuditMessage.class, UserPermission, Setting ]


    /**
     * Init method to be called on service creation
     */
    @PostConstruct
    def initialize() {
        log.info "Setting-up elasticsearch node and client"
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("path.home", grailsApplication.config.app.elasticsearch.location);
        //settings.put("number_of_shards",1);
        //settings.put("number_of_replicas",0);
        node = nodeBuilder().local(true).settings(settings).node();
        client = node.client();
        client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
    }

    /**
     * Create a new index with custom mappings
     *
     * TODO add code to check if index exists and only add/update mappings if required
     */
    def reInitialiseIndex(index) {
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

            addMappings(index)
        } catch (Exception e) {
            log.error "Error creating index: ${e}", e
        }
    }

    /**
     * Index a single document (toMap representation not domain class)
     * Does a check to see if doc has been marked as deleted.
     *
     * @param doc
     * @return IndexResponse
     */
    def indexDoc(doc, index) {
        def docId = getEntityId(doc)
        def docJson = doc as JSON
        index = index?:DEFAULT_INDEX

        if (checkForDelete(doc, docId)) {
            return null
        }

        try {
            addCustomFields(doc)
            IndexRequestBuilder builder = client.prepareIndex(index, DEFAULT_TYPE, docId)
            builder.setSource(docJson.toString(false)).execute().actionGet()

        } catch (Exception e) {
            log.error "Error indexing document: ${docJson.toString(true)}\nError: ${e}", e
        }
    }

    /**
     * Get the doc identifier, which differs for each domain class.
     * Note this can be called for both the Domain object itself or the
     * "toMap" representation it. TODO might be better way to do this
     *
     * @param  doc
     * @return docId (String)
     */
    def getEntityId(doc) {
        def docId
        def className = (doc.className) ? doc.className : doc.class.name;

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
        docId
    }

    def getDocType(doc) {
        def className = doc.className?:"au.org.ala.ecodata.doc"
        className.tokenize(".")[-1].toLowerCase()
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
        def resp
        try {
            resp = client.prepareGet(DEFAULT_INDEX, DEFAULT_TYPE, docId)
                    .execute()
                    .actionGet();
        } catch (Exception e) {
            log.error "ES prepareGet error: ${e}", e
        }

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
    def addCustomFields(Map doc) {

        // Remove the mongo id if it exists.
        doc.remove("_id")

        // hand-coded copy fields with different analysers
        doc.docType = getDocType(doc)

        if (!doc.name && doc.type) {
            // activities have no name so we'll use the type
            doc.name = doc.type
        }

        // Add some processed lat/lon data to doc
        doc.geo = []
        def lat, lon

        if (doc.extent?.geometry?.decimalLatitude && doc.extent?.geometry?.decimalLatitude) {
            lat = doc.extent.geometry.decimalLatitude as String
            lon = doc.extent.geometry.decimalLongitude as String
        } else if (doc.extent?.geometry?.centre?.size() == 2) {
            lat = doc.extent.geometry.centre[1] as String
            lon = doc.extent.geometry.centre[0] as String
        }

        if (lat && lon) {
            def geoObj = [:]
            geoObj.siteName = doc.name
            geoObj.siteId = doc.siteId
            def loc = [:]
            loc.lat = lat.toFloat()
            loc.lon = lon.toFloat()
            geoObj.loc = loc
            doc.geo.add(geoObj)
        }

        // Homepage index is nested TODO: remove duplicate code from above
        if (doc.sites?.size() > 0) {
            // one or more sites to a project (deep copy)

            doc.sites.each { site ->

                if (site.extent?.geometry?.decimalLatitude && site.extent?.geometry?.decimalLatitude) {
                    lat = site.extent.geometry.decimalLatitude as String
                    lon = site.extent.geometry.decimalLongitude as String
                } else if (site.extent?.geometry?.centre?.size() == 2) {
                    lat = site.extent.geometry.centre[1] as String
                    lon = site.extent.geometry.centre[0] as String
                }
                if (lat && lon) {
                    def geoObj = [:]
                    geoObj.siteName = site.name
                    geoObj.siteId = site.siteId
                    def loc = [:]
                    loc.lat = lat.toFloat()
                    loc.lon = lon.toFloat()
                    geoObj.loc = loc
                    doc.geo.add(geoObj)
                }
            }
        }

    }

    /**
     * Add custom mapping for ES index.
     */
    def addMappings(index) {

        def mappingJson = '''
        {
            "mappings":{
                "doc": {
                    "_all": {
                        "enabled": true,
                        "store": "yes"
                    },
                    "properties": {
						"status": {
                            "type" : "string",
                            "path" : "just_name",
                            "fields" : {
                                "status" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },
                        "organisationName": {
                            "type" : "string",
                            "path" : "just_name",
                            "fields" : {
                                "organisationFacet" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },
                        "serviceProviderName": {
                            "type" : "string",
                            "path" : "just_name",
                            "fields" : {
                                "organisationName" : {"type" : "string", "index" : "analyzed"},
                                "organisationFacet" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },
                        "type": {
                            "type" : "string",
                            "fields" : {
                                "typeFacet" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },
                        "className": {
                            "type":"string",
                            "analyzer":"facetKeyword"
                        },
                        "associatedProgram": {
                            "type" : "string",
                            "fields" : {
                                "associatedProgramFacet" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },
                        "associatedSubProgram": {
                            "type" : "string",
                            "fields" : {
                                "associatedSubProgramFacet" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },

                        "name": {
                            "type" : "string",
                            "fields" : {
                                "nameSort" : {"type" : "string", "index" : "not_analyzed"}
                            }
                        },
                        "extent":{
                            "properties": {
                                "geometry": {
                                    "properties": {


                                    }
                                }
                            }
                        },
                        "sites":{
                            "properties":{
                                "extent":{
                                    "properties": {
                                        "geometry": {
                                            "properties": {

                                            }
                                        }
                                    }
                                },
                                "externalId":{"type":"string"}
                            }
                        },
                        "externalId": {
                            "type":"string"
                        },
                        "activities":{
                            "properties":{
                                "mainTheme": {
                                    "type":"string",
                                    "path":"just_name",
                                    "fields": {
                                        "mainThemeFacet":{"type":"string", "index":"not_analyzed"}
                                    }
                                }
                            }
                        },
                        "mainTheme": {
                            "type":"string",
                            "path":"just_name",
                            "fields": {
                                "mainThemeFacet":{"type":"string", "index":"not_analyzed"}
                            }
                        },
                        "publicationStatus":{
                            "type":"string",
                            "index":"not_analyzed"
                        }

                    },
                    "dynamic_templates": [
                        {
                            "output_template": {
                                "path_match": "outputTargets.*",
                                "mapping": {
                                    "type": "string",
                                    "index": "analyzed"
                                }
                            }
                        }
                    ]
                }
            },
            "settings":{
                "analysis":{
                    "analyzer":{
                        "facetKeyword":{
                           "filter":[
                              "trim"
                           ],
                           "type":"custom",
                           "tokenizer":"keyword"
                        }
                    }
                }
            }
        }
        '''

        def parsedJson = new JsonSlurper().parseText(mappingJson)
        def facetMappings = buildFacetMapping()

        // Geometries can appear at two different locations inside a doc depending on the type (site, activity or project)
        parsedJson.mappings.doc["properties"].extent["properties"].geometry.put("properties", facetMappings)
        parsedJson.mappings.doc["properties"].sites["properties"].extent["properties"].geometry.put("properties", facetMappings)

        def mappingsDoc = (parsedJson as JSON).toString()

        def indexes = (index) ? [ index ] : [DEFAULT_INDEX, HOMEPAGE_INDEX]
        indexes.each {
            client.admin().indices().prepareCreate(it).setSource(mappingsDoc).execute().actionGet()
        }
        //client.admin().indices().prepareCreate(DEFAULT_INDEX).addMapping(DEFAULT_TYPE, mappingJson).execute().actionGet()
        client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet()
    }

    def buildFacetMapping() {
        def facetList = []
        def facetConfig = grailsApplication.config.app.facets.geographic
        // These groupings of facets determine the way the layers are used with a site, but can be treated the
        // same for the purposes of indexing the results.
        ['gridded', 'grouped', 'special'].each {
            facetList.addAll(facetConfig[it].collect {k, v -> k})
        }

        def properties = [:]
        facetList.each { facetName ->
            properties << [(facetName):[type:'multi_field', path:'just_name', fields:[(facetName):[type:"string", index:"analyzed"], (facetName+"Facet"):[type:"string", index:"not_analyzed"]]]]
        }
        properties
    }


    /**
     * Log GORM event to msg queue
     *
     * @param event
     */
    def queueGormEvent(AbstractPersistenceEvent event) {
        def doc = event.entityObject
        def docType = doc.getClass().name
        def docId = getEntityId(doc)
        def projectIdsToUpdate = []

        if (!ALLOWED_DOC_TYPES.contains(docType)) {
            return
        }

        //log.debug "GORM event: ${event.eventType} - doc has class: ${docType} with id: ${docId}"

// CG - nested sessions appear to result in a database connection leak which takes down the system.
//        if (event.eventType == EventType.PreUpdate && docType == "au.org.ala.ecodata.Site") {
//
//            Site.withNewSession { session ->
//                def site = Site.findBySiteId(docId)
//                //log.debug "site = ${site?:"none"} and has projects: ${site?.projects}"
//                site.projects.each {
//                    if (it.size() > 1) {
//                        //log.debug "it = ${it}"
//                        projectIdsToUpdate.add(it)
//                    }
//                }
//            }
//        }

        try {
            def message = new IndexDocMsg(docType: docType, docId: docId, indexType: event.eventType, docIds: projectIdsToUpdate)
            _messageQueue.offer(message)
        } catch (Exception ex) {
            log.error ex.localizedMessage, ex
        }
    }

    /**
     * Called by Quartz job - grabs all message on the queue and indexes
     * documents with ElasticSearch. Code gist taken from AuditService.
     *
     * @param maxMessagesToFlush
     * @return
     */
    public int flushIndexMessageQueue(int maxMessagesToFlush = 1000) {
        int messageCount = 0

        try {
            IndexDocMsg message = null;
            while (messageCount < maxMessagesToFlush && (message = _messageQueue.poll()) != null) {
                log.debug "Processing IndexDocMsg: ${message}"

                switch(message.indexType) {
                    case EventType.PostUpdate:
                    case EventType.PostInsert:
                        indexDocType(message.docId, message.docType)
                        break
                    case EventType.PostDelete:
                        deleteDocByIdAndType(message.docId, message.docType)
                        break
                    case EventType.PreUpdate:
                        checkDeleteForProjects(message.docIds)
                        break
                    default:
                        log.warn "Unexpected GORM event type: ${message.indexType}"
                }

                messageCount++
            }
        } catch (Exception ex) {
            log.error "Error indexing docs from message queue: ${ex}", ex
        }
        return messageCount
    }

    /**
     * Index any document type using the toMap representation of it.
     * Called by {@link GormEventListener GormEventListener}.
     *
     * @param doc (domain object)
     */
    def indexDocType(docId, docType) {
        // skip indexing
        if (indexingTempInactive
                || !grailsApplication.config.app.elasticsearch.indexOnGormEvents
                || !ALLOWED_DOC_TYPES.contains(docType)) {
            return null
        }

        switch(docType) {
            case "au.org.ala.ecodata.Project":
                def doc = Project.findByProjectId(docId)
                def projectMap = projectService.toMap(doc, "flat")
                projectMap["className"] = docType
                indexDoc(projectMap, DEFAULT_INDEX)
                // update homepage search index (map, etc)
                indexHomePage(doc, docType)
                break;
            case "au.org.ala.ecodata.Site":
                def doc = Site.findBySiteId(docId)
                def siteMap = siteService.toMap(doc, "flat")
                siteMap["className"] = docType
                indexDoc(siteMap, DEFAULT_INDEX)
                // update linked projects -- index for homepage
                doc.projects.each { // assume list of Strings (ids)
                    def pDoc = Project.findByProjectId(it)
                    if (pDoc) {
                        indexHomePage(pDoc, "au.org.ala.ecodata.Project")
                    } else {
                        log.warn "Project not found for id: ${it}"
                    }
                }
                break;
            case "au.org.ala.ecodata.Activity":
                Activity activity = Activity.findByActivityId(docId)
                def doc = activityService.toMap(activity, ActivityService.FLAT)
                doc = prepareActivityForIndexing(doc)
                indexDoc(doc, DEFAULT_INDEX)
                break;
        }
    }

    /**
     * Update index for home page (projects with sites)
     *
     * @param doc
     * @param docType
     */
    def indexHomePage(doc, docType) {
        // homepage index - turned off due to triggering recursive POST INSERT events for some reason
        try {
            def projectMapDeep = prepareProjectForHomePageIndex(doc)
            projectMapDeep["className"] = docType
            indexDoc(projectMapDeep, HOMEPAGE_INDEX)
        } catch (StackOverflowError e) {
            log.error "SO error - indexDocType for ${doc.projectId}: ${e.message}", e
        } catch (Exception e)  {
            log.error "Exception - indexDocType for ${doc?.projectId}: ${e.message}", e
        }
    }

    /**
     * Delete doc from search main index.
     *
     * @param doc (domain object)
     */
    def deleteDocType(doc) {
        def docId = getEntityId(doc)
        // skip indexing
        if (indexingTempInactive
                || !grailsApplication.config.app.elasticsearch.indexOnGormEvents
                || !ALLOWED_DOC_TYPES.contains(doc.getClass().name)) {
            return null
        }
        // delete from index
        def resp = checkForDelete(doc, docId)
        log.info "Delete from index for ${doc}: ${resp} "
    }

    /**
     * Delete doc from search index - by doc id and type
     *
     * @param docId
     * @param docType
     * @return
     */
    def deleteDocByIdAndType(docId, docType) {
        def doc

        switch(docType) {
            case "au.org.ala.ecodata.Project":
                doc = Project.findByProjectId(docId);
                break
            case "au.org.ala.ecodata.Site":
                doc = Site.findBySiteId(docId)
                break
            case "au.org.ala.ecodata.Activity":
                doc = Activity.findByActivityId(docId)
                break
        }

        if (doc) {
            deleteDocType(doc)
        } else {
            log.warn "Attempting to delete an unknown doc type: ${docType}. Doc not deleted from search index"
        }
    }

    /**
     * If an activity or site is deleted we need to keep track of the owning project (id)
     * and then re-index those projects.
     *
     * @param docIds
     * @return
     */
    def checkDeleteForProjects(docIds) {
        // docIds is assumed to be a list of ProjectIds
        docIds.each { id ->
            //log.debug "Updating project id: ${id}"
            indexDocType(id, "au.org.ala.ecodata.Project")
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
            it["className"] = new Project().getClass().name
            indexDoc(it, DEFAULT_INDEX)
        }
        // homepage index (doing some manual batching due to memory constraints)
        Project.withNewSession {
            def batchParams = [offset:0, max:50]
            def projects = Project.findAllByStatusInList([ProjectService.ACTIVE,ProjectService.COMPLETED], batchParams)

            while (projects) {
                projects.each { project ->
                    Map projectMap = prepareProjectForHomePageIndex(project)

                    indexDoc(projectMap, HOMEPAGE_INDEX)
                }

                batchParams.offset = batchParams.offset + batchParams.max
                projects = Project.findAllByStatusInList([ProjectService.ACTIVE,ProjectService.COMPLETED], batchParams)
            }
        }
        log.debug "Indexing all sites"
        def sites = Site.findAll()
        sites.each {
            def siteMap = siteService.toMap(it, "flat")
            siteMap["className"] = new Site().getClass().name
            indexDoc(siteMap, DEFAULT_INDEX)
        }
        log.debug "Indexing all activities"

        activityService.doWithAllActivities { activity ->
            prepareActivityForIndexing(activity)
            indexDoc(activity, DEFAULT_INDEX)
        }

    }

    /**
     * Augments the supplied Project with information required by the facets supported on the home page.
     * Specifically this includes site & activity information.
     * @param project the project
     * @return a Map ready for indexing.
     */
    private Map prepareProjectForHomePageIndex(Project project) {
        def projectMap = projectService.toMap(project, ProjectService.FLAT)

        // This is done so MERIT can filter relevant projects easily.
        def program = metadataService.programModel(projectMap.associatedProgram)
        def isMeritProgramme = program?.isMeritProgramme
        projectMap.isMeritProgramme = isMeritProgramme?:false

        projectMap["className"] = new Project().getClass().name
        projectMap.sites = siteService.findAllForProjectId(project.projectId, SiteService.FLAT)
        projectMap.activities = activityService.findAllForProjectId(project.projectId, LevelOfDetail.NO_OUTPUTS.name())
        projectMap
    }

    private Map prepareActivityForIndexing(Map activity) {
        activity["className"] = Activity.class.getName()
        // The project data is being flattened to match the existing mapping definition for the facets and to simplify the
        // faceting for reporting.

        if (activity.siteId) {
            def site = siteService.get(activity.siteId, SiteService.FLAT)
            activity.sites = [site]
        }
        def project = projectService.get(activity.projectId, ProjectService.FLAT)
        if (project) {
            activity.putAll(project)
        }
        activity
    }

    /**
     * Search with a query string
     *
     * @param query
     * @param params
     * @return IndexResponse
     */
    def search(String query, GrailsParameterMap params, index) {
        log.debug "search params: ${params}"

//        SearchRequestBuilder builder = client
//                .prepareSearch(DEFAULT_INDEX)
//                .setTypes(DEFAULT_TYPE)
//                .setQuery(queryString(query))
//                .setFrom(0)
//                .setSize(10)
//                .addHighlightedField("description")
//        SearchResponse sr = builder.execute().actionGet();

        index = index?:DEFAULT_INDEX
        def request = buildSearchRequest(query, params, index)
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


    def searchActivities(activityFilters, Map paginationParams, String index = DEFAULT_INDEX) {
        SearchRequest request = new SearchRequest()
        request.indices(index)
        request.searchType SearchType.DFS_QUERY_THEN_FETCH

        def queryBuilder = new MatchAllQueryBuilder()
        if (activityFilters) {
            def filters = buildFilters(activityFilters)
            queryBuilder = new FilteredQueryBuilder(queryBuilder, filters)
        }

        SearchSourceBuilder source =  pagenateQuery(paginationParams).query(queryBuilder)

        request.source(source)


        client.search(request).actionGet()

    }

    /**
     * Build the search request object from query and params
     *
     * @param query
     * @param params
     * @return SearchRequest
     */
    def buildSearchRequest(query, GrailsParameterMap params, index) {
        SearchRequest request = new SearchRequest()
        request.searchType SearchType.DFS_QUERY_THEN_FETCH

        // set indices and types
        request.indices(index)
        def types = []
        if (params.types && params.types instanceof Collection<String>) {
            types = params.types
        }
        request.types(types as String[])

        // set pagination stuff
        SearchSourceBuilder source = pagenateQuery(params)

        // add query
        source.query(queryString(query))

        // add facets
        addFacets(params.facets, params.fq, params.flimit, params.fsort).each {
            source.facet(it)
        }

        // handle facet filter
        if (params.fq) {
            log.debug "fq detected: ${params.fq}"
            source.postFilter(buildFilters(params.fq))
        }

        if (params.highlight) {
            source.highlight(new HighlightBuilder().preTags("<b>").postTags("</b>").field("_all", 60, 2))
        }

        request.source(source)

        return request
    }

    private SearchSourceBuilder pagenateQuery(Map params) {
        SearchSourceBuilder source = new SearchSourceBuilder()
        source.from(params.offset ? params.offset as int : 0)
        source.size(params.max ? params.max as int : 10)
        source.explain(params.explain ?: false)
        if (params.sort) {
            source.sort(params.sort, SortOrder.valueOf(params.order?.toUpperCase() ?: "ASC"))
        }
        source
    }

    /**
     * Generate list of facets for search request
     *
     * @param facets
     * @param filters
     * @return facetList
     */
    def addFacets(facets, filters, flimit, fsort) {
        // use FacetBuilders
        // e.g. FacetBuilders.termsFacet("f1").field("field")
        log.debug "filters = $filters; flimit = ${flimit}"
        try {
            flimit = (flimit) ? flimit as int : MAX_FACETS
        } catch (Exception e) {
            log.warn "addFacets error: ${e.message}"
            flimit = MAX_FACETS
        }
        try {
            fsort = (fsort) ? TermsFacet.ComparatorType.fromString(fsort) : TermsFacet.ComparatorType.COUNT
        } catch (Exception e) {
            log.warn "addFacets error: ${e.message}"
            fsort = TermsFacet.ComparatorType.COUNT
        }

        def facetList = []
        def filterList = getFilterList(filters)

        if (facets) {
            facets.split(",").each {
                facetList.add(FacetBuilders.termsFacet(it).field(it).size(flimit).facetFilter(addFacetFilter(filterList)))
            }
        } else {

            def defaultFacets = ['typeFacet', 'className', 'organisationFacet', 'stateFacet', 'lgaFacet', 'nrmFacet']
            defaultFacets.each { facet ->
                facetList.add(FacetBuilders.termsFacet(facet).field(facet).size(flimit).order(fsort).facetFilter(addFacetFilter(filterList)))
            }

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
		List repeatFacets = getRepeatFacetList(filterList)

        filterList.each {
            if (it) {
                if (!fb) {
                    fb = FilterBuilders.boolFilter()
                }
                def fqs = it.tokenize(":")
                if (fqs.size() > 1) {
					
					if(repeatFacets.find{it == fqs[0]}){
						fb.should(FilterBuilders.prefixFilter(fqs[0], fqs[1]))
					}
					else{
						fb.must(FilterBuilders.prefixFilter(fqs[0], fqs[1]))
					}
                }
                else {
                    fb.must(FilterBuilders.missingFilter(fqs[0]).nullValue(true))
                }
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
		
		List repeatFacets = getRepeatFacetList(filterList)

        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
        filterList.each { fq ->
            def fqs = fq.tokenize(":")
            // support SOLR style filters (-) for exclude
            if (fqs.size() > 1) {
                if (fqs[0].getAt(0) == "-") {
                    boolFilter.mustNot(FilterBuilders.prefixFilter(fqs[0][1..-1], fqs[1]))
                }
				else if(repeatFacets.find{it == fqs[0]}){
					boolFilter.should(FilterBuilders.prefixFilter(fqs[0], fqs[1]))
				}
				else{
					boolFilter.must(FilterBuilders.prefixFilter(fqs[0], fqs[1]))
				}
            }
            else {
                boolFilter.must(FilterBuilders.missingFilter(fqs[0]).nullValue(true))
            }
        }

        FilterBuilders.boolFilter().should(boolFilter)
    }

    /**
     * Helper method to return a List given either a List, String or String[]
     *
     * @param filters
     * @return filterList
     */
    private getFilterList(filters) {
        def filterList = []

        if (filters instanceof String[]) {
            // assume a String[] array
            filterList = filters as List
        } else if (filters instanceof List) {
            filterList.addAll(filters)
        }
        else {
            filterList.add(filters)
        }

        filterList
    }
	
	private getRepeatFacetList (filters) {
		def allFilters = getFilterList(filters)
		def facetNames = []
		def repeatFacets = []
		Set uniqueFacets

		if(allFilters.size() <= 1){
            return repeatFacets
        }
		allFilters.collect{
			def fqs = it.tokenize(":")
			facetNames.add(fqs[0])
		}
		uniqueFacets = facetNames as Set
		int repeatCount = 0;

		uniqueFacets.each { facet->
			allFilters.each {filter->
				def fqs = filter.tokenize(":")
				if(facet.equals(fqs[0])){
					repeatCount++;
				}
			}
			if(repeatCount >= 2){
				repeatFacets.add(facet)
			}
			repeatCount = 0
		}

		repeatFacets
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
    public deleteIndex(index) {
        def indexes = (index) ? [ index ] : [DEFAULT_INDEX, HOMEPAGE_INDEX]

        indexes.each {
            log.info "trying to delete $it"
            try {
                def response = node.client().admin().indices().prepareDelete(it).execute().get()
                if (response.acknowledged) {
                    log.info "The index is removed"
                } else {
                    log.error "The index could not be removed"
                }
            } catch (Exception e) {
                log.error "The index you want to delete is missing : ${e.message}"
            }
        }

        // recreate the index and mappings
        reInitialiseIndex(index)
        return "index cleared"
    }

    /**
     * Shutdown ES server
     */
    @PreDestroy
    def destroy() {
        node.close();
    }

}
