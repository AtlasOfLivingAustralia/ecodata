package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.converters.marshaller.json.CollectionMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.ListenableActionFuture
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.Client
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Created by sat01a on 24/11/15.
 */
@TestFor(ElasticSearchService)
@Mock([Document, Project, Site, ProjectActivity])
class ElasticSearchServiceSpec extends Specification {
    PermissionService permissionService = Stub(PermissionService)
    ProgramService programService = Stub(ProgramService)
    ProjectService projectService = Mock(ProjectService)
    Client client = Mock(Client)
    SiteService siteService = Mock(SiteService)
    ActivityService activityService = Mock(ActivityService)
    DocumentService documentService = Mock(DocumentService)

    def setup() {
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
    }

    void "View type : Invalid - Build a query that returns only non-embargoed records"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.embargoed:false)'
    }

    void "View type: 'myrecords' and empty userId - Build a query that should return only non-embargoed records"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "", 'view': "myrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.embargoed:false)'
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
        map.query == '(docType:activity AND projectActivity.projectId:' + map.projectId + ' AND (projectActivity.embargoed:false OR userId:' + map.userId + '))'
    }


    void "View type: 'project'- if unauthenticated user and valid project >> show non embargoed records."() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "", 'projectId': "abc", 'view': "project", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '(docType:activity AND projectActivity.projectId:' + map.projectId + ' AND projectActivity.embargoed:false)'
    }

    void "View type: 'allrecords' - logged in users and ala admin >> show all records across the projects"() {
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

    void "View type: 'allrecords' - logged in users and not ala admin >> show embargoed records that user own or been a member of the projects"() {
        when:
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.isUserAdminForProject(_, _) >> false
        permissionService.isUserEditorForProject(_, _) >> false
        permissionService.getProjectsForUser('1234', AccessLevel.admin, AccessLevel.editor) >> ['abc', 'cde']

        GrailsParameterMap map = new GrailsParameterMap([getParameterMap: { ->
            ['userId': "1234", 'projectId': "", 'view': "allrecords", 'query': ""]
        }] as HttpServletRequest)
        service.buildProjectActivityQuery(map)

        then:
        map.query == '((docType:activity) AND ((projectActivity.projectId:abc OR projectActivity.projectId:cde) OR (projectActivity.embargoed:false OR userId:' + map.userId + ')))'
    }

    void "View type: 'allrecords', logged in users and not ala admin >> show embargoed records that user own "() {
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
        map.query == '((docType:activity) AND (projectActivity.embargoed:false OR userId:' + map.userId + '))'
    }

    void "View type: 'allrecords' - unauthenticated user >> show only embargoed records across the projects."() {
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
        map.query == '(docType:activity AND projectActivity.embargoed:false)'
    }

    void "View type: 'allrecords' - unauthenticated user >> show only embargoed records across the projects and attach the searchTerm"() {
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
        map.query == 'Test AND (docType:activity AND projectActivity.embargoed:false)'
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

    def "A project can be prepared with extra information for use in the main project finder/project explorer index"() {

        setup:
        Project project = new Project(projectId:'p1', isMERIT:true)
        IndexRequestBuilder builder = Mock(IndexRequestBuilder)
        Map result

        when:
        service.indexHomePage(project, Project.class.name)

        then:
        2 * client.prepareGet(ElasticIndex.HOMEPAGE_INDEX, "doc", project.projectId)
        1 * client.prepareIndex(ElasticIndex.HOMEPAGE_INDEX, "doc", project.projectId) >> builder
        1 * builder.setSource({result = JSON.parse(it)}) >> builder
        1 * builder.execute() >> Mock(ListenableActionFuture)
        1 * projectService.toMap(project, ProjectService.FLAT) >> new HashMap(project.properties)

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

        Project meritProject = new Project(projectId:'p1', isMERIT:true)
        Project biocollectProject = new Project(projectId:'p2', isMERIT:false)
        IndexRequestBuilder builder = Mock(IndexRequestBuilder)
        Map meritResult
        Map biocollectResult

        when:
        service.indexHomePage(meritProject, Project.class.name)

        then:
        2 * client.prepareGet(ElasticIndex.HOMEPAGE_INDEX, "doc", meritProject.projectId)
        1 * client.prepareIndex(ElasticIndex.HOMEPAGE_INDEX, "doc", meritProject.projectId) >> builder
        1 * builder.setSource({meritResult = JSON.parse(it)}) >> builder
        1 * builder.execute() >> Mock(ListenableActionFuture)
        1 * projectService.toMap(meritProject, ProjectService.FLAT) >> new HashMap(meritProject.properties)
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
        2 * client.prepareGet(ElasticIndex.HOMEPAGE_INDEX, "doc", biocollectProject.projectId)
        1 * client.prepareIndex(ElasticIndex.HOMEPAGE_INDEX, "doc", biocollectProject.projectId) >> builder
        1 * builder.setSource({biocollectResult = JSON.parse(it)}) >> builder
        1 * builder.execute() >> Mock(ListenableActionFuture)
        1 * projectService.toMap(biocollectProject, ProjectService.FLAT) >> new HashMap(biocollectProject.properties)
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
        Project meritProject = new Project(projectId:'p1', name:"project 1", isMERIT:true)
        meritProject.save(flush:true, failOnError: true)
        Project biocollectProject = new Project(projectId:'p2', name:"project 2", isMERIT:false)
        biocollectProject.save(flush:true, failOnError: true)

        IndexRequestBuilder builder = Mock(IndexRequestBuilder)

        when: "A site linked to only MERIT projects is updated"
        service.indexDocType("site1", Site.class.name)

        then: "It won't be indexed"
        1 * siteService.toMap(_, SiteService.FLAT) >> [siteId:"site1", projects:[meritProject.projectId]]
        0 * client.prepareIndex(_,_,_)

        when: "The site is linked to a non-MERIT project"
        service.indexDocType("site1", Site.class.name)

        then: "It will be indexed"
        1 * siteService.toMap(_, SiteService.FLAT) >> [siteId:"site1", projects:[biocollectProject.projectId]]
        1 * client.prepareIndex(_,_,_) >> builder
        1 * builder.setSource(_) >> builder
        1 * builder.execute() >> Mock(ListenableActionFuture)

        when: "A site in linked to MERIT and non-MERIT projects it will be indexed"
        service.indexDocType("site1", Site.class.name)

        then: "It will be indexed"
        1 * siteService.toMap(_, SiteService.FLAT) >> [siteId:"site1", projects:[biocollectProject.projectId, meritProject.projectId]]
        1 * client.prepareIndex(_,_,_) >> builder
        1 * builder.setSource(_) >> builder
        1 * builder.execute() >> Mock(ListenableActionFuture)

    }

}