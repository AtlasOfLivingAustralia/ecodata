package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonSlurper
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller

import javax.servlet.http.HttpServletRequest
/**
 * Created by sat01a on 24/11/15.
 */
class ElasticSearchIndexServiceSpec extends MongoSpec implements ServiceUnitTest<ElasticSearchService> {
    PermissionService permissionService = Stub(PermissionService)
    ProgramService programService = Stub(ProgramService)
    ProjectService projectService = Mock(ProjectService)
    RestHighLevelClient client = GroovyMock(RestHighLevelClient) // Need a groovy mock here due to final methods
    SiteService siteService = Mock(SiteService)
    ActivityService activityService = Mock(ActivityService)
    DocumentService documentService = Mock(DocumentService)
    CacheService cacheService = new CacheService()

    def setup() {
        service.cacheService = cacheService
        service.permissionService = permissionService
        service.programService = programService
        service.client = client
        service.projectService = projectService
        service.siteService = siteService
        service.activityService = activityService
        service.documentService = documentService

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    def cleanup() {
        Project.collection.remove([:])
    }

    void "View type : Invalid - Build a query that returns only non-embargoed records that don't have to be verified or have been approved"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus)))'
    }

    void "View type: 'myrecords' and empty userId - Build a query that should return only non-embargoed records that don't have to be verified or have been approved"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "myrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus)))'
    }

    void "View type: 'myrecords' and valid userId - Build a query that returns all records associated to the user."() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "8997", 'projectId': "", 'view': "myrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == "(docType:activity AND userId:" + map.userId + ")"
    }

    void "View type: 'project' - if ala admin or project member >> show all records associated to the project"() {
        when:
        permissionService.isUserAlaAdmin(_) >> true
        permissionService.isUserAdminForProject(_, _) >> true
        permissionService.isUserEditorForProject(_, _) >> true

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "8997", 'projectId': "abc", 'view': "project", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.projectId:' + map.projectId + ')'
    }

        // Admin, moderator, editor should see all verificationStatus
    void "View type: 'project' - if project editor >> show all records in project"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> true

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "8997", 'projectId': "abc", 'view': "project", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.projectId:' + map.projectId + ')'
    }

    void "View type: 'project'- if logged in user >> show non embargoed records + records created by user"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "889", 'projectId': "abc", 'view': "project", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.projectId:' + map.projectId + ' AND ((projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus))) OR userId:' + map.userId + '))'
    }


    void "View type: 'project'- if unauthenticated user and valid project >> show non embargoed records that don't have to be verified or have been approved."() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "abc", 'view': "project", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.projectId:' + map.projectId + ' AND projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus)))'
    }

    void "View type: 'allrecords' - logged in users with ala admin role >> show all records across the projects"() {
        when:
        permissionService.isUserAlaAdmin(_) >> true
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "", 'view': "allrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity)'
    }

    void "View type: 'allrecords' - logged in users and not ala admin >> show embargoed records that user owns or been a member of the projects"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser('1234', AccessLevel.admin, AccessLevel.moderator, AccessLevel.editor) >> ['abc', 'cde']

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "", 'view': "allrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '((docType:activity) AND ((projectActivity.projectId:abc OR projectActivity.projectId:cde) OR ((projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus))) OR userId:' + map.userId + ')))'
    }

    void "View type: 'allrecords', logged in users and not ala admin >> show embargoed records that user owns "() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser('1234', AccessLevel.admin, AccessLevel.editor) >> []

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "", 'view': "allrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '((docType:activity) AND ((projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus))) OR userId:' + map.userId + '))'
    }

    void "View type: 'allrecords' - unauthenticated user >> show only embargoed records that don't have to be verified or have been approved across the projects."() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser(_, _) >> []

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "allrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus)))'
    }

    void "View type: 'allrecords' - unauthenticated user >> show only embargoed records  that don't have to be verified or have been approved across the projects and attach the searchTerm"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser(_, _) >> []

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "allrecords", 'query': "", 'searchTerm': "Test"]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == 'Test AND (docType:activity AND projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus)))'
    }

    void "View type: 'bulkimport' - logged in users with ala admin role >> show all records across the projects"() {
        when:
        permissionService.isUserAlaAdmin(_) >> true
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "", 'view': "bulkimport", "bulkImportId": "123", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND bulkImportId:123)'
    }

    void "View type: 'bulkimport' - no bulk import id"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser('1234', AccessLevel.admin, AccessLevel.moderator, AccessLevel.editor) >> ['abc', 'cde']

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "", 'view': "bulkimport", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.embargoed:false AND (verificationStatusFacet:approved OR verificationStatusFacet:\"not applicable\" OR (NOT _exists_:verificationStatus)))'
    }

    void "View type: 'bulkimport', project admin >> show records associated with bulk import "() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> true
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser('1234', AccessLevel.admin, AccessLevel.editor) >> ["123"]

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "123", 'view': "bulkimport", "bulkImportId": "123", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND bulkImportId:123)'
    }

    void "View type: 'bulkimport' - unauthenticated user >> show only public records of import"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser(_, _) >> []

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "bulkimport", "bulkImportId": "123", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND bulkImportId:123 AND projectActivity.embargoed:false)'
    }


    void  "the include and exclude parameters are optional"() {
        setup:
        String queryString = "name:test"
        Map params = new GrailsParameterMap([:], null)
        String index = ElasticIndex.HOMEPAGE_INDEX

        when:
        service.buildSearchRequest(queryString, params, index)

        then:
        noExceptionThrown()


        when:
        params = new GrailsParameterMap(["include":"test", "exclude":"test"], null)
        service.buildSearchRequest(queryString, params, index)

        then:
        noExceptionThrown()

        when:
        params = new GrailsParameterMap(["include":["test", "test2"], "exclude":['test3', "test4"]], null)
        service.buildSearchRequest(queryString, params, index)

        then:
        noExceptionThrown()

    }

    def "Searches can include a shape component"() {
        setup:
        String queryString = "name:test"
        Map params = new GrailsParameterMap([:], null)
        String index = ElasticIndex.HOMEPAGE_INDEX
        Map geoSearchCritera = [type:'Polygon', coordinates:[[[1,0], [1, 1], [0, 1], [0, 0], [1, 0]]]]

        when:
        service.buildSearchRequest(queryString, params, index, geoSearchCritera)

        then:
        noExceptionThrown()

    }

    def  "the aggs parameters is optional"() {
        setup:
        String queryString = ""
        Map params = new GrailsParameterMap(["aggs":'[{"type": "geohash", "field": "geoPoint", "precision": 4}]'], null)
        String index = ElasticIndex.HOMEPAGE_INDEX

        when:
        service.buildSearchRequest(queryString, params, index)

        then:
        noExceptionThrown()
    }

    def "A project can be prepared with extra information for use in the main project finder/project explorer index"() {

        setup:
        Map projectProps = [projectId:'p1', isMERIT:true]
        Project project = new Project(projectProps)
        def result

        when:
        service.indexHomePage(project, Project.class.name)

        then:
        2 * client.get({GetRequest get -> get.index() == ElasticIndex.HOMEPAGE_INDEX && get.id() == projectProps.projectId}, RequestOptions.DEFAULT) >> Mock(GetResponse)
        1 * client.index({ IndexRequest index -> index.index() == ElasticIndex.HOMEPAGE_INDEX && index.id() == projectProps.projectId}, RequestOptions.DEFAULT) >>
                { index, options -> result = new JsonSlurper().parseText(index.source().utf8ToString()); Mock(IndexResponse) }
        1 * projectService.toMap(project, ProjectService.FLAT) >> projectProps

        and:
        result.projectId == project.projectId
        result.isMERIT ==  project.isMERIT
    }

    /**
     * This is done due to the performance impacts on indexing of some of the very large MERIT sites.
     */
    def "merit sites should not include the geoIndex field in the homepage index"() {
        setup:
        Map site1 = [name:'site1', projects:['p1'], extent:[geometry:[coordinates:[1,2]]], geoIndex:[type:'MultiPolygon']]
        Map site2 = [name:'site1', projects:['p1'], extent:[geometry:[coordinates:[1,2]]], geoIndex:[type:'MultiPolygon']]

        Map meritProjectProps = [projectId:'p1', isMERIT:true]
        Project meritProject = new Project(meritProjectProps)
        Map biocollectProjectProps = [projectId:'p2', isMERIT:false]
        Project biocollectProject = new Project(biocollectProjectProps)
        Map meritResult
        Map biocollectResult

        when:
        service.indexHomePage(meritProject, Project.class.name)

        then:
        2 * client.get({GetRequest get -> get.index() == ElasticIndex.HOMEPAGE_INDEX && get.id() == meritProjectProps.projectId}, RequestOptions.DEFAULT) >> Mock(GetResponse)
        1 * client.index({ IndexRequest index -> index.index() == ElasticIndex.HOMEPAGE_INDEX && index.id() == meritProjectProps.projectId}, RequestOptions.DEFAULT) >>
                { index, options  -> meritResult = new JsonSlurper().parseText(index.source().utf8ToString()); Mock(IndexResponse) }
        1 * projectService.toMap(meritProject, ProjectService.FLAT) >> meritProjectProps
        1 * siteService.findAllForProjectId(meritProject.projectId, SiteService.FLAT) >> [site1]

        and:
        meritResult.projectId == meritProject.projectId
        meritResult.isMERIT ==  meritProject.isMERIT
        meritResult.sites.size() == 1
        meritResult.sites[0].geoIndex == null
        meritResult.sites[0].extent.geometry.coordiantes == null

        when: "The project is not a MERIT project"
        service.indexHomePage(biocollectProject, Project.class.name)

        then:
        2 * client.get({GetRequest get -> get.index() == ElasticIndex.HOMEPAGE_INDEX && get.id() == biocollectProjectProps.projectId}, RequestOptions.DEFAULT) >> Mock(GetResponse)
        1 * client.index({ IndexRequest index -> index.index() == ElasticIndex.HOMEPAGE_INDEX && index.id() == biocollectProjectProps.projectId}, RequestOptions.DEFAULT) >>
                { index, options -> biocollectResult = new JsonSlurper().parseText(index.source().utf8ToString()); Mock(IndexResponse) }
        1 * projectService.toMap(biocollectProject, ProjectService.FLAT) >> biocollectProjectProps
        1 * siteService.findAllNonPrivateSitesForProjectId(biocollectProject.projectId, SiteService.FLAT) >> [site2]

        and: "The geoIndex on the attached sites is unchanged."
        biocollectResult.projectId == biocollectProject.projectId
        biocollectResult.isMERIT == biocollectProject.isMERIT
        biocollectResult.sites.size() == 1
        biocollectResult.sites[0].geoIndex != null
    }

    /**
     * This is done due to the performance impacts on indexing of some of the very large MERIT sites.
     */
    def "merit sites should not be included in the default index"() {
        setup:
        Map meritProjectProps = [projectId:'p1', name:"project 1", isMERIT:true]
        Project meritProject = new Project(meritProjectProps)
        meritProject.save(flush:true, failOnError: true)
        Map biocollectProjectProps = [projectId:'p2', name:"project 2", isMERIT:false]
        Project biocollectProject = new Project(biocollectProjectProps)
        biocollectProject.save(flush:true, failOnError: true)

        when: "A site linked to only MERIT projects is updated"
        service.indexDocType("site1", Site.class.name)

        then: "It won't be indexed"
        1 * siteService.toMap(_, SiteService.FLAT) >> [siteId:"site1", projects:[meritProject.projectId]]
        0 * client.get(_, _) >> Mock(GetResponse)
        0 * client.index(_,_)

        when: "The site is linked to a non-MERIT project"
        service.indexDocType("site1", Site.class.name)

        then: "It will be indexed"
        1 * siteService.toMap(_, SiteService.FLAT) >> [siteId:"site1", projects:[biocollectProject.projectId]]
        3 * client.get(_, _) >> Mock(GetResponse)
        1 * client.index({IndexRequest index ->
                index.index() == ElasticIndex.DEFAULT_INDEX && index.id() == 'site1'}, RequestOptions.DEFAULT) >> Mock(IndexResponse)
        1 * projectService.toMap(biocollectProject, ProjectService.FLAT) >> biocollectProjectProps


        when: "A site in linked to MERIT and non-MERIT projects it will be indexed"
        service.indexDocType("site1", Site.class.name)

        then: "It will be indexed"
        1 * siteService.toMap(_, SiteService.FLAT) >> [siteId:"site1", projects:[biocollectProject.projectId, meritProject.projectId]]
        2 * client.get(_, _) >> Mock(GetResponse)
        1 * client.index(_,_) >> Mock(IndexResponse)

    }

    void "works activities must be indexed even if output is not present"() {
        setup:
        Project worksProject = new Project(projectId:'p1', name:"project 1", isMERIT:false, isWorks: true)
        worksProject.save(flush:true, failOnError: true)
        Activity activity = new Activity(activityId: 'act1', projectId:'p1')
        activity.save(flush:true, failOnError: true)

        IndexRequestBuilder builder = Mock(IndexRequestBuilder)

        when: "Activity without associated output linked to works project is indexed"
        service.indexDocType("act1", Activity.class.name)

        then: "Activity object is indexed"
        1 * activityService.toMap(_, ActivityService.FLAT) >> [activityId:"act1", projectId:worksProject.projectId, status: 'active']
        1 * projectService.toMap (_, ProjectService.FLAT) >> [projectId:'p1', name:"project 1", isMERIT:false, isWorks: true]

        3 * client.get(_, _) >> Mock(GetResponse)
        2 * client.index(_,_) >> Mock(IndexResponse)
    }
}