package au.org.ala.ecodata

import au.org.ala.grails.AnnotationMatcher
import au.org.ala.web.AuthService
import au.org.ala.web.Pac4jAuthService
import au.org.ala.ws.security.profile.AlaM2MUserProfile
import grails.converters.JSON
import grails.core.GrailsApplication
import org.pac4j.core.profile.UserProfile

import javax.annotation.PostConstruct

class PreAuthoriseInterceptor {

    int order = 100 // This needs to be after the @RequireApiKey interceptor which makes the userId available via the authService

    UserService userService
    AuthService authService
    Pac4jAuthService pac4jAuthService
    PermissionService permissionService
    HubService hubService
    GrailsApplication grailsApplication

    public PreAuthoriseInterceptor() {

    }

    @PostConstruct
    void init() {
        AnnotationMatcher.matchAnnotation(this, grailsApplication, PreAuthorise)
    }

    boolean before() {
        def matchResult = AnnotationMatcher.getAnnotation(grailsApplication, controllerNamespace, controllerName, actionName, PreAuthorise, null)
        PreAuthorise pa = matchResult.effectiveAnnotation()
        Map result = [error: '', status : 401]

        if (pa?.basicAuth()) {
            def user = userService.setUser()
            request.userId = user?.userId
            if (user == null) {
                UserProfile profile = authService.delegateService.getUserProfile()
                if (profile instanceof AlaM2MUserProfile) {
                    // get client id of the M2M user
                    String userId = request.userId = profile.getUserId()
                    userService.setCurrentUser(userId)
                }
            }

            if (permissionService.isUserAlaAdmin(request.userId)) {
                /* Don't enforce check for ALA admin.*/
            }
            else if (request.userId) {
                String accessLevel = pa.accessLevel()
                String idType = pa.idType()
                String entityId = params[pa.id()]

                if (accessLevel && idType) {

                    switch (idType) {
                        case "hubId":
                            def hub = hubService.findByUrlPath(entityId)
                            if (!hub) {
                                hub = hubService.get(entityId)
                            }

                            if (!hub) {
                                result.error = "Hub not found for id: ${entityId}"
                                result.status = 404
                                break
                            }

                            result = permissionService.checkPermission(accessLevel, hub.hubId, Hub.class.name, request.userId)
                            break
                        case "organisationId":
                            result = permissionService.checkPermission(accessLevel, entityId, Organisation.class.name, request.userId)
                            break
                        case "projectId":
                            result = permissionService.checkPermission(accessLevel, entityId, Project.class.name, request.userId)
                            break
                        case "projectActivityId":
                            def pActivity = projectActivityService.get(entityId)
                            request.projectId = pActivity?.projectId
                            result = permissionService.checkPermission(accessLevel, pActivity?.projectId, Project.class.name, request.userId)
                            break
                        case "activityId":
                            def activity = activityService.get(entityId,'flat')
                            result = permissionService.checkPermission(accessLevel, activity?.projectId, Project.class.name, request.userId)
                            break
                        default:
                            break
                    }
                }

            } else {
                result.error = "Access denied"
                result.status = 401
            }
        }

        if(result.error) {
            log.info("Access denied for userId: ${request.userId}, controller: ${controllerName}, action: ${actionName}, params: ${params}")
            response.status = result.status ?: 403
            render result as JSON
            return false
        }

        true
    }

    boolean after() { true }

    void afterView() {
        userService.clearCurrentUser()
    }
}