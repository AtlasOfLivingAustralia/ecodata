package au.org.ala.ecodata

import au.org.ala.web.AuthService
import au.org.ala.web.Pac4jAuthService
import au.org.ala.ws.security.profile.AlaM2MUserProfile
import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification

class PreAuthoriseInterceptorSpec extends Specification implements InterceptorUnitTest<PreAuthoriseInterceptor> {
    def hubService
    def userService
    def permissionService
    def delegateService

    def setup() {
        interceptor.hubService = hubService = Stub(HubService)
        interceptor.userService = userService = Stub(UserService)
        interceptor.permissionService = permissionService = Stub(PermissionService)
        interceptor.authService = new AuthService()
        interceptor.authService.delegateService = delegateService = Stub(Pac4jAuthService)
    }

    void "interceptor should allow access to public endpoints"() {
        when:
        withInterceptors([controller: 'documentation']) {
            controller.getProjectSites()
        }

        then:
        response.status == 200
    }

    void "interceptor should check user's hub permission where required and return forbidden when no permission is granted"() {
        given:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        params.id = "abc"
        userService.setUser() >> new UserDetails(userId: "1", userName: "testUser", displayName: "Test User")
        hubService.findByUrlPath(params.id) >> [hubId: "hub1", name: "Test Hub", urlPath: params.id]
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.checkPermission("readOnly", "hub1", Hub.class.name, "1") >> [error: 'Unauthorized', status: 403]

        when:

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedMethod')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)
        withRequest(controller: "annotatedMethod", action: action)
        def result = interceptor.before()

        then:
        response.status == statusCode
        result == before

        where:
        action | statusCode | before
        "securedAction" | 403 | false
        "annotatedPublicAction" | 200 | true
        "publicAction" | 200 | true
    }

    void "interceptor should check user's hub permission where required and pass when permission is granted"() {
        given:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        params.id = "abc"
        userService.setUser() >> new UserDetails(userId: "1", userName: "testUser", displayName: "Test User")
        hubService.findByUrlPath(params.id) >> [hubId: "hub1", name: "Test Hub", urlPath: params.id]
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.checkPermission("readOnly", "hub1", Hub.class.name, "1") >> [error: '', status: 403]

        when:

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedMethod')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)
        withRequest(controller: "annotatedMethod", action: action)
        def result = interceptor.before()

        then:
        response.status == statusCode
        result == before

        where:
        action | statusCode | before
        "securedAction" | 200 | true
        "annotatedPublicAction" | 200 | true
        "publicAction" | 200 | true
    }

    void "interceptor should allow access using M2M token where required and pass when permission is granted"() {
        given:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        params.id = "abc"
        userService.setUser() >> null
        delegateService.getUserProfile() >> new AlaM2MUserProfile("m2mUser", "issuer", [])
        hubService.findByUrlPath(params.id) >> [hubId: "hub1", name: "Test Hub", urlPath: params.id]
        permissionService.isUserAlaAdmin(_) >> false
        permissionService.checkPermission("readOnly", "hub1", Hub.class.name, "m2mUser") >> [error: '', status: 403]

        when:

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedMethod')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)
        withRequest(controller: "annotatedMethod", action: action)
        def result = interceptor.before()

        then:
        response.status == statusCode
        result == before

        where:
        action | statusCode | before
        "securedAction" | 200 | true
        "annotatedPublicAction" | 200 | true
        "publicAction" | 200 | true
    }
}



class AnnotatedMethodController {
    @PreAuthorise(basicAuth = true, accessLevel = "readOnly", idType = "hubId")
    def securedAction() {

    }

    @PreAuthorise(basicAuth = false)
    def annotatedPublicAction() {

    }

    def publicAction() {

    }
}
