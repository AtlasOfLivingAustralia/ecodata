package au.org.ala.ecodata

import grails.converters.JSON
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
}