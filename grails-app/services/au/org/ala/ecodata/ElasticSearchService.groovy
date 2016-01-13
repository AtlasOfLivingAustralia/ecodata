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

import com.vividsolutions.jts.geom.Coordinate
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.commons.validator.routines.UrlValidator
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.FilteredQueryBuilder
import org.elasticsearch.index.query.GeoShapeFilterBuilder
import org.elasticsearch.index.query.QueryBuilders
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
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentLinkedQueue

import static org.elasticsearch.index.query.FilterBuilders.*
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery
import static org.elasticsearch.node.NodeBuilder.nodeBuilder
import static au.org.ala.ecodata.Status.*
import static au.org.ala.ecodata.ElasticIndex.*

/**
 * ElasticSearch service. This service is responsible for indexing documents as well as handling searches (queries).
 *
 * Note:
 * DEFAULT_INDEX used by MERIT
 * HOMEPAGE_INDEX shared by both Biocollect and MERIT (MERIT embeds activities to the project. Bicollect doesn't include embedded activities)
 * PROJECT_ACTIVITY_INDEX used by Biocollect and its applicable to survey based projects (ie; non NRM one's)
 *
 * Code gist taken from
 *   https://github.com/mstein/elasticsearch-grails-plugin/blob/master/grails-app/services/org/grails/plugins/elasticsearch/ElasticSearchService.groovy
 *
 * @author "Nick dos Remedios <nick.dosremedios@csiro.au>"
 */
class ElasticSearchService {
    static transactional = false
    def grailsApplication

    ProjectService projectService
    ActivityService activityService
    SiteService siteService
    PermissionService permissionService
    UserService userService
    DocumentService documentService
    ProjectActivityService projectActivityService
    RecordService recordService
    MetadataService metadataService
    OrganisationService organisationService

    Node node;
    Client client;
    def indexingTempInactive = false // can be set to true for loading of dump files, etc
    def ALLOWED_DOC_TYPES = [Project.class.name, Site.class.name, Activity.class.name, Record.class.name, Organisation.class.name]
    def DEFAULT_TYPE = "doc"
    def MAX_FACETS = 10
    private static Queue<IndexDocMsg> _messageQueue = new ConcurrentLinkedQueue<IndexDocMsg>()
    private static List<Class> EXCLUDED_OBJECT_TYPES = [AuditMessage.class, UserPermission, Setting]
    /**
     * Init method to be called on service creation
     */
    @PostConstruct
    def initialize() {
        log.info "Setting-up elasticsearch node and client"
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("path.home", grailsApplication.config.app.elasticsearch.location);
        node = nodeBuilder().local(true).settings(settings).node();
        client = node.client();
        client.admin().cluster().prepareHealth().setWaitForYellowStatus().setTimeout('30s').execute().actionGet();
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
        index = index ?: DEFAULT_INDEX

        if (checkForDelete(doc, docId, index)) {
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
     * @param doc
     * @return docId (String)
     */
    def getEntityId(doc) {
        def docId
        def className = (doc.className) ? doc.className : doc.class.name;

        switch (className) {
            case Project.class.name:
                docId = doc.projectId; break
            case Site.class.name:
                docId = doc.siteId; break
            case Activity.class.name:
                docId = doc.activityId; break
            case Organisation.class.name:
                docId = doc.organisationId; break
            case Report.class.name:
                docId = doc.reportId; break
            case Record.class.name:
                docId = doc.occurrenceID; break
            default:
                docId = doc.id; break
        }
        docId
    }

    def getDocType(doc) {
        def className = doc.className ?: "au.org.ala.ecodata.doc"
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
    def checkForDelete(doc, docId, String index = DEFAULT_INDEX) {
        def isDeleted = false
        def resp

        try {
            resp = client.prepareGet(index, DEFAULT_TYPE, docId).execute().actionGet();
        } catch (Exception e) {
            log.error "ES prepareGet error: ${e}", e
        }

        if (resp && doc.status == DELETED) {
            try {
                deleteDocById(docId, index)
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

        String[] schemes = ["http","https"]
        def parsedJson
        UrlValidator urlValidator = new UrlValidator(schemes);
        if(urlValidator.isValid("${grailsApplication.config.app.esmapping.location}")) {
            parsedJson = new JsonSlurper().parseText(new URL("${grailsApplication.config.app.esmapping.location}").getText())
        } else {
            parsedJson = new JsonSlurper().parse(new File("${grailsApplication.config.app.esmapping.location}"))
        }

        def facetMappings = buildFacetMapping()
        // Geometries can appear at two different locations inside a doc depending on the type (site, activity or project)
        parsedJson.mappings.doc["properties"].extent["properties"].geometry.put("properties", facetMappings)
        parsedJson.mappings.doc["properties"].sites["properties"].extent["properties"].geometry.put("properties", facetMappings)

        def mappingsDoc = (parsedJson as JSON).toString()

        def indexes = (index) ? [index] : [DEFAULT_INDEX, HOMEPAGE_INDEX, PROJECT_ACTIVITY_INDEX]
        indexes.each {
            client.admin().indices().prepareCreate(it).setSource(mappingsDoc).execute().actionGet()
        }

        client.admin().cluster().prepareHealth().setWaitForYellowStatus().setTimeout('3').execute().actionGet()
    }

    def buildFacetMapping() {
        def facetList = []
        def facetConfig = grailsApplication.config.app.facets.geographic
        // These groupings of facets determine the way the layers are used with a site, but can be treated the
        // same for the purposes of indexing the results.
        ['contextual', 'grouped', 'special'].each {
            facetList.addAll(facetConfig[it].collect { k, v -> k })
        }

        def properties = [:]
        facetList.each { facetName ->
            properties << [(facetName): [type: 'multi_field', path: 'just_name', fields: [(facetName): [type: "string", index: "analyzed"], (facetName + "Facet"): [type: "string", index: "not_analyzed"]]]]
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

                switch (message.indexType) {
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

        switch (docType) {
            case "au.org.ala.ecodata.Project":
                def doc = Project.findByProjectId(docId)
                def projectMap = projectService.toMap(doc, "flat")
                projectMap["className"] = docType
                doc?.isMERIT ? indexDoc(projectMap, DEFAULT_INDEX) : ''
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

            case "au.org.ala.ecodata.Record":
                Record record = Record.findByOccurrenceID(docId)
                if(record) {
                    Activity activity = Activity.findByActivityId(record.activityId)
                    def doc = activityService.toMap(activity, ActivityService.FLAT)
                    doc = prepareActivityForIndexing(doc)
                    indexDoc(doc, doc?.projectActivityId ? PROJECT_ACTIVITY_INDEX : DEFAULT_INDEX)
                }
                break

            case "au.org.ala.ecodata.Activity":
                Activity activity = Activity.findByActivityId(docId)
                def doc = activityService.toMap(activity, ActivityService.FLAT)
                doc = prepareActivityForIndexing(doc)
                indexDoc(doc, doc?.projectActivityId ? PROJECT_ACTIVITY_INDEX : DEFAULT_INDEX)
                // update linked project -- index for homepage
                def pDoc = Project.findByProjectId(doc.projectId)
                if (pDoc) {
                    indexHomePage(pDoc, "au.org.ala.ecodata.Project")
                }
                break
            case "au.org.ala.ecodata.Organisation":
                Map organisation = organisationService.get(docId)
                prepareOrganisationForIndexing(organisation)
                indexDoc(organisation, DEFAULT_INDEX)
                break
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
            def docId = getEntityId(doc)
            if (checkForDelete(doc, docId, HOMEPAGE_INDEX)) {
                return null
            }

            def projectMapDeep = prepareProjectForHomePageIndex(doc)
            projectMapDeep["className"] = docType
            indexDoc(projectMapDeep, HOMEPAGE_INDEX)
        } catch (StackOverflowError e) {
            log.error "SO error - indexDocType for ${doc.projectId}: ${e.message}", e
        } catch (Exception e) {
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

        switch (docType) {
            case "au.org.ala.ecodata.Project":
                doc = Project.findByProjectId(docId);
                break
            case "au.org.ala.ecodata.Site":
                doc = Site.findBySiteId(docId)
                break
            case "au.org.ala.ecodata.Activity":
                doc = Activity.findByActivityId(docId)
                break
            case "au.org.ala.ecodata.Organisation":
                doc = Organisation.findByOrganisationId(docId)
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

        log.debug "Indexing all MERIT based projects in MERIT SEARCH index."
        def list = projectService.listMeritProjects("flat", false)
        list.each {
            it["className"] = new Project().getClass().name
            indexDoc(it, DEFAULT_INDEX)
        }

        // homepage index (doing some manual batching due to memory constraints)
        log.debug "Indexing all MERIT and NON-MERIT projects in generic HOMEPAGE index"
        Project.withNewSession {
            def batchParams = [offset: 0, max: 50]
            def projects = Project.findAllByStatusInList([ACTIVE, COMPLETED], batchParams)

            while (projects) {
                projects.each { project ->
                    Map projectMap = prepareProjectForHomePageIndex(project)

                    indexDoc(projectMap, HOMEPAGE_INDEX)
                }

                batchParams.offset = batchParams.offset + batchParams.max
                projects = Project.findAllByStatusInList([ACTIVE, COMPLETED], batchParams)
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
            indexDoc(activity, activity?.projectActivityId ? PROJECT_ACTIVITY_INDEX : DEFAULT_INDEX)
        }

        log.debug "Indexing all organisations"
        List<Map> organisations = organisationService.list()
        for (Map org : organisations) {
            prepareOrganisationForIndexing(org)
            indexDoc(org, DEFAULT_INDEX)
        }

        log.debug "Indexing complete"
    }

    /**
     * Adds information useful for searching to the organisation
     * @param organisation the existing organisation details
     */
    private void prepareOrganisationForIndexing(Map organisation) {
        organisation["className"] = Organisation.class.name
        Map results = documentService.search([organisationId:organisation.organisationId, role:DocumentService.LOGO])
        if (results && results.documents) {
            organisation.logoUrl = results.documents[0].url
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
        projectMap["className"] = new Project().getClass().name
        projectMap.sites = siteService.findAllForProjectId(project.projectId, SiteService.FLAT)
        projectMap.links = documentService.findAllLinksForProjectId(project.projectId)
        projectMap.isMobileApp = documentService.isMobileAppForProject(projectMap);
        projectMap.imageUrl = documentService.findImageUrlForProjectId(project.projectId);
        projectMap.admins = permissionService.getAllAdminsForProject(project.projectId)?.collect {
            it.userId
        };
        // Include only for MERIT type projects.
        if (project.isMERIT) {
            projectMap.activities = activityService.findAllForProjectId(project.projectId, LevelOfDetail.NO_OUTPUTS.name())
        }

        projectMap
    }

    private Map prepareActivityForIndexing(Map activity) {
        activity["className"] = Activity.class.getName()

        def project = projectService.get(activity.projectId, ProjectService.FLAT)

        // Include project activity only for survey based projects.
        def pActivity = projectActivityService.get(activity.projectActivityId)
        if (pActivity) {
            Map projectActivity = [:]
            List records = []

            projectActivity.name = pActivity.name
            projectActivity.endDate = pActivity.endDate
            projectActivity.projectActivityId = pActivity.projectActivityId
            projectActivity.embargoed = pActivity?.visibility?.embargoUntil && pActivity?.visibility?.embargoUntil.after(new Date())
            projectActivity.embargoUntil = pActivity?.visibility?.embargoUntil ?: ''
            projectActivity.activityOwnerName = userService.lookupUserDetails(activity.userId)?.displayName
            projectActivity.projectName = project?.name
            projectActivity.projectId = project?.projectId

            def allRecords = recordService.getAllByActivity(activity.activityId)
            allRecords?.each {
                Map values = [:]
                values.name = it.name
                values.guid = it.guid
                values.occurrenceID = it.occurrenceID
                values.coordinates = [it.decimalLatitude, it.decimalLongitude]
                records << values
            }
            projectActivity.records = records
            projectActivity.lastUpdatedMonth = new SimpleDateFormat("MMMM").format(activity.lastUpdated)
            projectActivity.lastUpdatedYear = new SimpleDateFormat("yyyy").format(activity.lastUpdated)

            activity.projectActivity = projectActivity
        } else if (project) {
            // The project data is being flattened to match the existing mapping definition for the facets and to simplify the
            // faceting for reporting.
            project.remove('custom')
            project.remove('timeline')
            project.remove('outputTargets')
            project.remove('plannedStartDate')
            project.remove('plannedEndDate')
            project.remove('startDate')
            project.remove('endDate')
            project.remove('description')
            activity.putAll(project)
            activity.programSubProgram = project.associatedProgram + ' - ' + project.associatedSubProgram
        }

        if (activity.siteId) {
            def site = siteService.get(activity.siteId, SiteService.FLAT)
            if (site) {
                activity.sites = [site]
            }
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
    def search(String query, GrailsParameterMap params, String index, Map geoSearchCriteria = [:]) {
        log.debug "search params: ${params}"

        index = index ?: DEFAULT_INDEX
        def request = buildSearchRequest(query, params, index, geoSearchCriteria)
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
        return response
    }


    def searchActivities(activityFilters, Map paginationParams, String searchTerm = null, String index = DEFAULT_INDEX) {
        SearchRequest request = new SearchRequest()
        request.indices(index)
        request.searchType SearchType.DFS_QUERY_THEN_FETCH

        def queryBuilder = searchTerm ? QueryBuilders.queryStringQuery(searchTerm) : QueryBuilders.matchAllQuery()

        if (activityFilters) {
            def filters = buildFilters(activityFilters)
            queryBuilder = new FilteredQueryBuilder(queryBuilder, filters)
        }

        SearchSourceBuilder source = pagenateQuery(paginationParams).query(queryBuilder)
        request.source(source)

        client.search(request).actionGet()
    }

    /*
    *  Builds a customized project activity query for home page index based on userId and projectId
    *  1. My records page >> show all records associated to the user.
    *  2. Project data page >>
    *       // a. if ala admin / project member  >> show all records associated to the project
    *       // b. if logged in user >> show non embargoed records + records created by user.
    *       // c. if unauthenticated user >> show non embargoed records.
    *   3. All records page and no projectId's
    *       // a. logged in users and ala admin >> show all records across the projects
    *       // b. logged in users and not ala admin >> show embargoed records that user own or been member of the projects
    *       // c. unauthenticated user >> show only embargoed records across the projects.
    *
    */
    void buildProjectActivityQuery(GrailsParameterMap params) {

        String query = params.searchTerm ?: ''
        String userId = params.userId
        String projectId = params.projectId
        String forcedQuery = ''

        switch (params.view) {

            case 'myrecords':
                if (userId) {
                    forcedQuery = '(docType:activity AND userId:' + userId + ')'
                }
                break

            case 'project':
                if (projectId) {
                    if (userId && permissionService.isUserAlaAdmin(userId) || permissionService.isUserAdminForProject(userId, projectId) || permissionService.isUserEditorForProject(userId, projectId)) {
                        forcedQuery = '(docType:activity AND projectActivity.projectId:' + projectId + ')'
                    } else if (userId) {
                        forcedQuery = '(docType:activity AND projectActivity.projectId:' + projectId + ' AND (projectActivity.embargoed:false OR userId:' + userId + '))'
                    } else if (!userId) {
                        forcedQuery = '(docType:activity AND projectActivity.projectId:' + projectId + ' AND projectActivity.embargoed:false)'
                    }
                }
                break

            case 'allrecords':
                if (!projectId) {
                    if (userId && permissionService.isUserAlaAdmin(userId)) {
                        forcedQuery = '(docType:activity)'
                    } else if (userId) {
                        forcedQuery = '((docType:activity)'
                        List<String> projectsTheUserIsAMemberOf = permissionService.getProjectsForUser(userId, AccessLevel.admin, AccessLevel.editor)

                        projectsTheUserIsAMemberOf?.eachWithIndex { item, index ->
                            if (index == 0) {
                                forcedQuery = forcedQuery + ' AND (('
                            } else if (index != 0) {
                                forcedQuery = forcedQuery + ' OR '
                            }

                            forcedQuery = forcedQuery + 'projectActivity.projectId:' + item
                        }
                        if (projectsTheUserIsAMemberOf) {
                            forcedQuery = forcedQuery + ') OR (projectActivity.embargoed:false OR userId:' + userId + ')))'
                        } else {
                            forcedQuery = forcedQuery + ' AND (projectActivity.embargoed:false OR userId:' + userId + '))'
                        }
                    } else if (!userId) {
                        forcedQuery = '(docType:activity AND projectActivity.embargoed:false)'
                    }
                }
                break

            default:
                forcedQuery = '(docType:activity AND projectActivity.embargoed:false)'
                break
        }

        if (!forcedQuery) {
            forcedQuery = '(docType:activity AND projectActivity.embargoed:false)'
        }

        params.facets = "activityLastUpdatedYearFacet,activityLastUpdatedMonthFacet,projectNameFacet,projectActivityNameFacet,recordNameFacet,activityOwnerNameFacet"
        params.query = query ? query + ' AND ' + forcedQuery : forcedQuery
    }

    /**
     * Build the search request object from query and params
     *
     * @param query
     * @param params
     * @return SearchRequest
     */
    def buildSearchRequest(String query, GrailsParameterMap params, String index, Map geoSearchCriteria = [:]) {
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
        if (geoSearchCriteria) {
            // geo shape filters are not supported by the queryString syntax, so we need to create a filteredQuery
            source.query(filteredQuery(queryStringQuery(query), buildGeoFilter(geoSearchCriteria)))
        } else {
            source.query(queryStringQuery(query))
        }

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

        if (params.omitSource) {
            source.noFields()
        }

        request.source(source)

        return request
    }

    private static FilterBuilder buildGeoFilter(Map geographicSearchCriteria) {
        GeoShapeFilterBuilder filter = null

        ShapeBuilder shape = null
        switch (geographicSearchCriteria.type) {
            case "Polygon":
                shape = ShapeBuilder.newPolygon()
                shape.points(geographicSearchCriteria.coordinates[0].collect { coordinate ->
                    new Coordinate(coordinate[0] as double, coordinate[1] as double)
                } as Coordinate[])
                break;
            case "Circle":
                shape = ShapeBuilder.newCircleBuilder()
                        .radius(geographicSearchCriteria.radius?.toString())
                        .center(geographicSearchCriteria.coordinates[0] as double, geographicSearchCriteria.coordinates[1] as double)
                break
        }

        if (shape) {
            filter = geoShapeFilter("geoIndex", shape, ShapeRelation.INTERSECTS)
        }

        filter
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

                    if (repeatFacets.find { it == fqs[0] }) {
                        fb.should(FilterBuilders.termFilter(fqs[0], fqs[1]))
                    } else {
                        fb.must(FilterBuilders.termFilter(fqs[0], fqs[1]))
                    }
                } else {
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
                    boolFilter.mustNot(FilterBuilders.termFilter(fqs[0][1..-1], fqs[1]))
                } else if (repeatFacets.find { it == fqs[0] }) {
                    boolFilter.should(FilterBuilders.termFilter(fqs[0], fqs[1]))
                } else {
                    boolFilter.must(FilterBuilders.termFilter(fqs[0], fqs[1]))
                }
            } else {
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
        } else {
            filterList.add(filters)
        }

        filterList
    }

    private getRepeatFacetList(filters) {
        def allFilters = getFilterList(filters)
        def facetNames = []
        def repeatFacets = []
        Set uniqueFacets

        if (allFilters.size() <= 1) {
            return repeatFacets
        }
        allFilters.collect {
            def fqs = it.tokenize(":")
            facetNames.add(fqs[0])
        }
        uniqueFacets = facetNames as Set
        int repeatCount = 0;

        uniqueFacets.each { facet ->
            allFilters.each { filter ->
                def fqs = filter.tokenize(":")
                if (facet.equals(fqs[0])) {
                    repeatCount++;
                }
            }
            if (repeatCount >= 2) {
                repeatFacets.add(facet)
            }
            repeatCount = 0
        }

        repeatFacets
    }

    /**
     * Delete a doc given its ID
     *
     * @param id
     * @return
     */
    def deleteDocById(id, String index = DEFAULT_INDEX) {
        client.prepareDelete(index, DEFAULT_TYPE, id).execute().actionGet();
    }

    /**
     * Delete the (default) ES index
     *
     * @return
     */
    public deleteIndex(index) {
        def indexes = (index) ? [index] : [DEFAULT_INDEX, HOMEPAGE_INDEX, PROJECT_ACTIVITY_INDEX]

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

        createIndexAndMapping(index)
        return "index cleared"
    }

    /**
     * Create a new index add configure custom mappings
     */
    def createIndexAndMapping(index) {
        log.debug "Creating new index and configuring elastic search custom mapping"
        try {
            addMappings(index)
        } catch (Exception e) {
            log.error "Error creating index: ${e}", e
        }
    }

    /**
     * Shutdown ES server
     */
    @PreDestroy
    def destroy() {
        node.close();
    }


}
