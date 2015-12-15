package au.org.ala.ecodata

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Created by sat01a on 24/11/15.
 */
@TestFor(ElasticSearchService)
class ElasticSearchServiceSpec extends Specification {
    PermissionService permissionService = Stub(PermissionService)

    def setup() {
        service.permissionService = permissionService
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
}